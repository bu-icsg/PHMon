package varanus

import Chisel._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.util.property._
import freechips.rocketchip.config.Parameters
import Chisel.ImplicitConversions._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config.{Parameters,Field}

case object KomodoMatchUnits extends Field[Int]
case object DebugKomodo extends Field[Boolean]

trait KomodoParameters {
  implicit val p: Parameters
  val numUnits = p(KomodoMatchUnits)
  val debugKomodo = p(DebugKomodo)
}

trait KomodoEnums {
  val (e_SM_PC_SRC_CARE :: e_SM_PC_DST_CARE :: e_SM_INST_CARE :: e_SM_RD_CARE :: e_SM_DATA_CARE ::
    e_SM_PC_SRC_DCARE :: e_SM_PC_DST_DCARE :: e_SM_INST_DCARE :: e_SM_RD_DCARE :: e_SM_DATA_DCARE :: Nil) =
    Enum(UInt(), 10)
  val (e_C_VALID :: e_C_INVALID :: e_C_RESET :: e_C_M_COUNT :: e_C_LOCAL :: e_C_COMMIT_IDX :: e_C_INFO_SP_OFFSET :: e_C_WRITE_COUNT :: e_C_MEM_TYPE :: e_C_DONE :: Nil) =
    Enum(UInt(), 10)

  val commit_PC_SRC :: commit_PC_DST :: commit_INST :: commit_DATA :: commit_ADDR :: Nil = Enum(UInt(), 5)

  val (e_ACT_INTR :: e_ACT_MEM_RD :: e_ACT_MEM_WR :: Nil) = Enum(UInt(), 3)

}

class PHMonRoCC(implicit p: Parameters) extends LazyRoCC {
  override lazy val module = new Komodo(this)
}

class Komodo(outer: PHMonRoCC)(implicit p: Parameters) extends LazyRoCCModule(outer) with HasCoreParameters with KomodoParameters with KomodoEnums {
  val matchUnits = Vec.tabulate(numUnits){
    (i: Int) => Module(new KomodoMatchUnit(i)(p)).io }
  val alu = Module(new TinyALU).io
  val controlUnit = Module(new ControlUnit).io
  val configUnits = Vec.tabulate(numUnits){
   (i: Int) => Module(new ActionConfigUnit(i)(p)).io }
  val activeUnit = Reg(UInt(width=log2Up(numUnits))) // The MU that its action is getting executed

  val activation_queue = Module(new Queue(new DoorbellResp, 1024)).io
  
  val ctrl = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(1))
  val enable = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(3))
  val disable = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(4))
  val is_reset = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(2))
  val enabled = Reg(init=Bool(false))
  val resume = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(15))
  val id = io.cmd.bits.rs1(31,0)
  val action = io.cmd.bits.rs1(63,32)
  val data = io.cmd.bits.rs2
  val interrupt_en = Wire(Bool())
  val busy_en = Reg(init=Bool(false))
  val intr_en = Reg(init=Bool(false))
  val mem_wait = Reg(init=Bool(false)) // A register to keep the memory request while RoCC is not ready to receive it
  val mem_req_typ = RegInit(UInt(3,width=3)) //Default: MT_D
  val wait_for_resp = RegInit(init=Bool(false)) // A debugging register to verify whether Varanus is waiting to receive the memory response from RoCC
  val wait_for_resp_after_assert = RegInit(init=Bool(false))
  val read_mask = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(12))
  val read_conf = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(13))
  val read_commit_index = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(14))
  val threshold = RegInit(UInt(1000,width=xLen))

  io.cmd.ready := Bool(true)
  io.resp.bits.rd := io.cmd.bits.inst.rd
  io.resp.valid := Bool(false)
  io.resp.bits.data := UInt(0)
  io.commitLog.ready := Bool(true)

  controlUnit.conf_req.valid := ctrl && (action === e_C_LOCAL)
  controlUnit.act_alu_resp.valid := Bool(false)

  (0 until numUnits).map(i => {
    matchUnits(i).cmd.bits := io.cmd.bits
    matchUnits(i).commitLog <> io.commitLog
    configUnits(i).req := Bool(false)
    configUnits(i).cu_wait := Bool(false)
    configUnits(i).cmd.bits := io.cmd.bits
    configUnits(i).cmd.valid := io.cmd.valid & id === UInt(i)
    when (enable | disable | is_reset) {
      matchUnits(i).cmd.valid := io.cmd.valid }
    .otherwise {
      matchUnits(i).cmd.valid := io.cmd.valid & id === UInt(i) }})

  when (enable) {
    enabled := Bool(true)
  }
  .elsewhen (disable) {
    enabled := Bool(false)
  }

  when (is_reset) {
    mem_wait := Bool(false)
    intr_en := Bool(false)
    busy_en := Bool(false)
    io.mem.req.valid := Bool(false)
    wait_for_resp := Bool(false)
  }

  when (ctrl) {
    switch (action) {
      is (e_C_RESET)           { mem_wait := Bool(false)
        wait_for_resp := Bool(false)
        wait_for_resp_after_assert := Bool(false)}
      is (e_C_LOCAL)           { controlUnit.conf_req.bits.index := id
        controlUnit.conf_req.bits.data := data}
      is (e_C_INFO_SP_OFFSET)  { io.resp.valid := Bool(true)
        io.resp.bits.data := controlUnit.read_storage_resp(id) }
      is (e_C_MEM_TYPE)        { mem_req_typ := data(2,0) }
      is (e_C_DONE)            { io.resp.valid := Bool(true)
        io.resp.bits.data := activation_queue.count }
    }
  }

  for (i <- 0 until numUnits) {
    when (matchUnits(i).resp.doorbell.valid && activation_queue.enq.ready) {
      activation_queue.enq <> matchUnits(i).resp.doorbell
      printf("[EXTRA] MU[%d] increases the counter, counter: %d\n", UInt(i), activation_queue.count)
    }
  }

  when (read_mask | read_commit_index) {
    io.resp.bits.data := matchUnits(id).read_cmd.bits
    io.resp.valid := Bool(true)
  }

  when (read_conf) {
    io.resp.bits.data := configUnits(id).conf_read.bits
    io.resp.valid := Bool(true)
  }

  alu.fn := UInt(9)
  alu.in1 := UInt(0)
  alu.in2 := UInt(0)

  activation_queue.deq.ready := controlUnit.ready
  controlUnit.is_reset := is_reset
  controlUnit.doorbell.bits.addr := activation_queue.deq.bits.addr
  controlUnit.doorbell.bits.data := activation_queue.deq.bits.data
  controlUnit.doorbell.bits.tag := activation_queue.deq.bits.tag
  controlUnit.doorbell.bits.insn_type := activation_queue.deq.bits.insn_type
  controlUnit.doorbell.valid := activation_queue.deq.valid
  controlUnit.alu_req <> configUnits(activeUnit).resp.alu_req
  controlUnit.mem_req <> configUnits(activeUnit).resp.mem_req
  controlUnit.act_done := configUnits(activeUnit).act_done
  configUnits(activeUnit).skip_actions := controlUnit.skip_actions

  configUnits(activeUnit).cu_wait := controlUnit.cu_wait

  when (activation_queue.deq.fire()) {
    printf("[EXTRA] activation_queue dequeued MU%d data: 0x%x addr: 0x%x contorler: %d Count: %d\n", activation_queue.deq.bits.tag, activation_queue.deq.bits.data, activation_queue.deq.bits.addr, controlUnit.ready, activation_queue.count)
    activeUnit := activation_queue.deq.bits.tag
    configUnits(activation_queue.deq.bits.tag).req := Bool(true)
  }

  io.mem.req.bits <> controlUnit.act_mem_req.bits
  io.mem.req.bits.phys := Bool(false)

  io.mem.req.bits.valid_req := wait_for_resp_after_assert && (io.commitLog.bits.priv === UInt(0) || io.commitLog.bits.priv === UInt(1)) && (enabled)
  io.mem.req.valid := (controlUnit.act_mem_req.valid | mem_wait) && (io.commitLog.bits.priv === UInt(0) || io.commitLog.bits.priv === UInt(1)) && (enabled)
  

  when (mem_wait && io.mem.req.ready) {
    printf("[EXTRA] Komodo memory ready has arrived!; data: 0x%x, addr: 0x%x\n", io.mem.req.bits.data, io.mem.req.bits.addr)
  }

  when (controlUnit.act_mem_req.valid && (!io.mem.req.ready || (io.commitLog.bits.priv =/= UInt(0) && io.commitLog.bits.priv =/= UInt(1)) || !enabled)) {
    mem_wait := Bool(true)
    printf("[EXTRA] Komodo has to wait for memory ready; data: 0x%x, addr: 0x%x\n", io.mem.req.bits.data, io.mem.req.bits.addr)
  }

  io.mem.req.bits.typ := mem_req_typ
  io.mem.invalidate_lr := Bool(false)

  interrupt_en := controlUnit.interrupt_en || configUnits(activeUnit).resp.intr || io.commitLog.bits.interrupt_replay || (activation_queue.count === threshold)

  controlUnit.act_mem_resp.valid := io.mem.resp.valid && wait_for_resp
  controlUnit.act_mem_resp.bits := io.mem.resp.bits.data

  when (controlUnit.act_alu_req.valid) {
    alu.fn := controlUnit.act_alu_req.bits.fn
    alu.in1 := controlUnit.act_alu_req.bits.in1
    alu.in2 := controlUnit.act_alu_req.bits.in2
    printf("[KOMODO] Komodo alu request action, fn: %d, in1: 0x%x, in2: 0x%x, out: 0x%x\n", alu.fn, alu.in1, alu.in2, alu.out)
    controlUnit.act_alu_resp.valid := Bool(true)
    controlUnit.act_alu_resp.bits := alu.out
  }

  controlUnit.act_intr_done := Bool(false)
  when (resume) {
    controlUnit.act_intr_done := Bool(true)
  }
  controlUnit.act_intr := interrupt_en

  io.busy := Bool(false)
  io.interrupt := interrupt_en
  when (io.interrupt) {
    printf("[KOMODO] Komodo: Interrupt\n")
  }

  when (io.mem.req.fire()) {
    busy_en := Bool(true)
    mem_wait := Bool(false)
    wait_for_resp := Bool(true)
    printf("[MEM] Komodo memory request arrived, data: 0x%x, addr: 0x%x \n", io.mem.req.bits.data, io.mem.req.bits.addr)
  }
  when (io.mem.resp.valid && wait_for_resp) {
    busy_en := Bool(false)
    wait_for_resp := Bool(false)
    wait_for_resp_after_assert := Bool(false)
    printf("[MEM] Komodo memory response arrived, data: 0x%x\n", io.mem.resp.bits.data)
  }
  when (io.mem.assertion && wait_for_resp) {
    wait_for_resp_after_assert := Bool(true)
  }

}
