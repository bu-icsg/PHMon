package varanus

import Chisel._
import freechips.rocketchip.util.property._
import Chisel.ImplicitConversions._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.rocket._
import freechips.rocketchip.config.{Parameters,Field}

class MemoryRequest(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with HasCoreParameters {
  val addr = UInt(width = xLen)
  val data = UInt(width = xLen)
  val cmd = UInt(width = M_SZ)
  val tag = UInt(width = 8)
}

class ALURequest(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with HasCoreParameters {
  val fn = Bits(width = 4)
  val in1 = UInt(width = xLen)
  val in2 = UInt(width = xLen)
}

class ConfigLocal(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with HasCoreParameters {
  val index = UInt(width = 3)
  val data = UInt(width = xLen)
}

class ControlUnitInterface(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with HasCoreParameters {
  val doorbell = Valid(new DoorbellResp).flip //doorbell coming from Action Unit
  val alu_req = Valid(new ActionALU).flip // ALU request coming from Config Table
  val mem_req = Valid(new ActionMem).flip // Memory request coming from Config Table
  val act_mem_resp = Valid(UInt(width=xLen)).flip // The memory response coming from Komodo through RoCC
  val act_alu_resp = Valid(UInt(width=xLen)).flip // The ALU response coming from Komodo (ALU)
  val act_intr = Bool(INPUT) // An interrupt is happening (config unit interrupt)
  val act_intr_done = Bool(INPUT) // The interrupt is done
  val act_done = Bool(INPUT) // A signal coming from Config Table indicating that all the actions for the corresponding event are taken
  val conf_req = Valid(new ConfigLocal).flip // A request for accessing local storages
  val is_reset = Bool(INPUT)
  val interrupt_en = Bool(OUTPUT) // Intrupt request to Komodo
  val act_mem_req = Valid(new MemoryRequest) // Memory request to Komodo
  val act_alu_req = Valid(new ALURequest) // ALU request to Komodo (ALU)
  val cu_wait = Bool(OUTPUT) // Control Unit is in the idel state, this signal goes to Config Table
  val ready = Bool(OUTPUT) // Control Unit is ready to take actions for a new event, this signal goes to Komodo
  val read_storage_resp = Reg(Vec(8, UInt(width=xLen))).asOutput // Contains the value of one of the local storages based the conf_req
  val skip_actions = Bool(OUTPUT)
}

class ControlUnit(implicit val p: Parameters)
    extends Module with ActionConfigEnums with HasCoreParameters with HasL1HellaCacheParameters {
  val io = new ControlUnitInterface
  val mu_resp = Reg(UInt(width=xLen))
  val action_mem_req = Reg(new MemoryRequest)
  val action_mem_req_valid = Reg(init=Bool(false))
  val local1 = Reg(UInt(width=xLen))
  val local2 = Reg(UInt(width=xLen))
  val local3 = Reg(UInt(width=xLen))
  val local4 = Reg(UInt(width=xLen))
  val local5 = Reg(UInt(width=xLen))
  val local6 = Reg(UInt(width=xLen))

  val e_ready :: e_idle :: e_busy :: Nil = Enum(UInt(), 3)
  val state = Reg(init = e_ready) // Control Unit's FSM

  val wait_for_done = Reg(init = Bool(false))
  val insn_type = Reg(init=UInt(0,1))
  val counter_action = Reg(init=UInt(0,4))
  val interrupt_en = Mux(io.act_alu_resp.valid, (Mux(io.alu_req.bits.out === e_INTR, io.act_alu_resp.bits === UInt(0), Bool(false))), Bool(false))

  when (io.interrupt_en) {
    printf("[EXTRA] Control Unit: Interrupt\n")
  }

  when(io.conf_req.valid) {
    printf("[EXTRA] conf local index: %d addr: 0x%x\n", io.conf_req.bits.index, io.conf_req.bits.data)
    when(io.conf_req.bits.index === UInt(0)) {
      local1 := io.conf_req.bits.data
    }
      .elsewhen(io.conf_req.bits.index === UInt(1)) {
      local2 := io.conf_req.bits.data
    }
    .elsewhen(io.conf_req.bits.index === UInt(2)) {
      local3 := io.conf_req.bits.data
    }
    .elsewhen(io.conf_req.bits.index === UInt(3)) {
      local4 := io.conf_req.bits.data
    }
    .elsewhen(io.conf_req.bits.index === UInt(4)) {
      local5 := io.conf_req.bits.data
    }
    .elsewhen(io.conf_req.bits.index === UInt(5)) {
      local6 := io.conf_req.bits.data
    }
  }

  io.ready := (state === e_ready)
  io.cu_wait := (state === e_idle)
  
  when (io.doorbell.fire() && (state === e_ready)) {
    state := e_idle
    printf("[EXTRA] state change from ready to idle, state:%d\n", state)
  }
  .elsewhen ((io.mem_req.valid || io.alu_req.valid || io.act_intr) && (state === e_idle)) {
    state := e_busy
    counter_action := counter_action + UInt(1)
    printf("[EXTRA] state change from idle to busy, state: %d\n", state)
  }

  when ((io.act_mem_resp.valid && (state === e_busy)) || (io.act_alu_resp.valid && !interrupt_en) || (io.act_intr_done && (state === e_busy))) {
    state := e_idle
    printf("[EXTRA] state change to idle, state: %d\n", state)
  }

  when (io.act_done) {
    when (state === e_idle) {
      state := e_ready
      wait_for_done := Bool(false)
      counter_action := UInt(0)
      printf("[EXTRA] state change from idle to ready, state: %d\n", state)
    }
      .otherwise {
      wait_for_done := Bool(true)
    }
  }

  when (wait_for_done && (state === e_idle)) {
    state := e_ready
      wait_for_done := Bool(false)
      printf("[EXTRA] state change from idle to ready, state: %d\n", state)
  }

  when (io.skip_actions) {
    state := e_ready
    wait_for_done := Bool(false)
    printf("[EXTRA] state change from idle to ready; skipping rest of the actions, state: %d\n", state)
  }

  io.act_alu_req.valid := Bool(false)

  io.act_mem_req.bits.cmd := action_mem_req.cmd
  io.act_mem_req.bits.tag := action_mem_req.addr << untagBits
  io.act_mem_req.bits.data := action_mem_req.data
  io.act_mem_req.bits.addr := action_mem_req.addr
  io.act_mem_req.valid := action_mem_req_valid
  io.read_storage_resp(0) := local1
  io.read_storage_resp(1) := local2
  io.read_storage_resp(2) := local3
  io.read_storage_resp(3) := local4
  io.read_storage_resp(4) := local5
  io.read_storage_resp(5) := local6
  io.read_storage_resp(6) := state
  io.read_storage_resp(7) := counter_action
  io.interrupt_en := interrupt_en
  io.skip_actions := Bool(false)

  when (io.doorbell.valid && (state === e_ready)) {
    action_mem_req.addr := io.doorbell.bits.addr
    action_mem_req.data := io.doorbell.bits.data
    action_mem_req.cmd := M_XRD
    insn_type := io.doorbell.bits.insn_type
    printf("[KOMODO] ****** Control Unit ***** Insn_type: 0x%x\n", io.doorbell.bits.insn_type)
  }

  when (io.mem_req.valid && (state =/= e_busy)) {
    action_mem_req.cmd := io.mem_req.bits.mem_op
    action_mem_req.tag := io.mem_req.bits.tag
    switch(io.mem_req.bits.data) {
      is (e_IN_ADDR_MU)    { action_mem_req.data := action_mem_req.addr }
      is (e_IN_LOC1)       { action_mem_req.data := local1 }
      is (e_IN_LOC2)       { action_mem_req.data := local2 }
      is (e_IN_LOC3)       { action_mem_req.data := local3 }
      is (e_IN_LOC4)       { action_mem_req.data := local4 }
      is (e_IN_LOC5)       { action_mem_req.data := local5 }
      is (e_IN_LOC6)       { action_mem_req.data := local6 }
      is (e_IN_DATA_RESP)  { action_mem_req.data := mu_resp }
      is (e_IN_COMPRESSED) { action_mem_req.data := insn_type }
    }

    switch(io.mem_req.bits.addr) {
      is (e_IN_DATA_MU)    { action_mem_req.addr := action_mem_req.data }
      is (e_IN_LOC1)       { action_mem_req.addr := local1 }
      is (e_IN_LOC2)       { action_mem_req.addr := local2 }
      is (e_IN_LOC3)       { action_mem_req.addr := local3 }
      is (e_IN_LOC4)       { action_mem_req.addr := local4 }
      is (e_IN_LOC5)       { action_mem_req.addr := local5 }
      is (e_IN_LOC6)       { action_mem_req.addr := local6 }
      is (e_IN_DATA_RESP)  { action_mem_req.addr := mu_resp }
      is (e_IN_COMPRESSED) { action_mem_req.addr := insn_type }
    }
    action_mem_req_valid := Bool(true)
    printf("[EXTRA] Control Unit received memory request from config Unit, data:%d addr: %d\n", io.mem_req.bits.data, io.mem_req.bits.addr)
  }
  .otherwise {
    action_mem_req_valid := Bool(false)
  }

  when (io.act_mem_req.valid) {
    printf("[EXTRA] Control Unit memory request valid, data:0x%x 0x%x addr: 0x%x 0x%x operation: %d state: %d\n", io.act_mem_req.bits.data, action_mem_req.data, io.act_mem_req.bits.addr, action_mem_req.addr, io.act_mem_req.bits.cmd, state)
  }
  
  when (io.alu_req.valid && (state =/= e_busy)) {
    printf("[EXTRA] Control Unit alu request valid, fn: %d, in1: %d, in2: %d, out: %d, state: %d\n", io.alu_req.bits.fn, io.alu_req.bits.in1, io.alu_req.bits.in2, io.alu_req.bits.out, state)

    switch(io.alu_req.bits.in1) {
      is (e_IN_DATA_MU)    { io.act_alu_req.bits.in1 := action_mem_req.data }
      is (e_IN_ADDR_MU)    { io.act_alu_req.bits.in1 := action_mem_req.addr }
      is (e_IN_CONST)      { io.act_alu_req.bits.in1 := io.alu_req.bits.data }
      is (e_IN_LOC1)       { io.act_alu_req.bits.in1 := local1 }
      is (e_IN_LOC2)       { io.act_alu_req.bits.in1 := local2 }
      is (e_IN_LOC3)       { io.act_alu_req.bits.in1 := local3 }
      is (e_IN_LOC4)       { io.act_alu_req.bits.in1 := local4 }
      is (e_IN_LOC5)       { io.act_alu_req.bits.in1 := local5 }
      is (e_IN_LOC6)       { io.act_alu_req.bits.in1 := local6 }
      is (e_IN_DATA_RESP)  { io.act_alu_req.bits.in1 := mu_resp }
      is (e_IN_COMPRESSED) { io.act_alu_req.bits.in1 := insn_type }
    }

    switch(io.alu_req.bits.in2) {
      is (e_IN_DATA_MU)    { io.act_alu_req.bits.in2 := action_mem_req.data }
      is (e_IN_ADDR_MU)    { io.act_alu_req.bits.in2 := action_mem_req.addr }
      is (e_IN_CONST)      { io.act_alu_req.bits.in2 := io.alu_req.bits.data }
      is (e_IN_LOC1)       { io.act_alu_req.bits.in2 := local1 }
      is (e_IN_LOC2)       { io.act_alu_req.bits.in2 := local2 }
      is (e_IN_LOC3)       { io.act_alu_req.bits.in2 := local3 }
      is (e_IN_LOC4)       { io.act_alu_req.bits.in2 := local4 }
      is (e_IN_LOC5)       { io.act_alu_req.bits.in2 := local5 }
      is (e_IN_LOC6)       { io.act_alu_req.bits.in2 := local6 }
      is (e_IN_DATA_RESP)  { io.act_alu_req.bits.in2 := mu_resp }
      is (e_IN_COMPRESSED) { io.act_alu_req.bits.in2 := insn_type }
    }
    io.act_alu_req.bits.fn := io.alu_req.bits.fn
    io.act_alu_req.valid := Bool(true)
  }

  when (io.act_alu_resp.valid) {
    switch(io.alu_req.bits.out) {
      is (e_OUT_LOC1)  { local1 := io.act_alu_resp.bits }
      is (e_OUT_LOC2)  { local2 := io.act_alu_resp.bits }
      is (e_OUT_LOC3)  { local3 := io.act_alu_resp.bits }
      is (e_OUT_LOC4)  { local4 := io.act_alu_resp.bits }
      is (e_OUT_LOC5)  { local5 := io.act_alu_resp.bits }
      is (e_OUT_LOC6)  { local6 := io.act_alu_resp.bits }
      is (e_OUT_DATA)  { action_mem_req.data := io.act_alu_resp.bits }
      is (e_OUT_ADDR)  { action_mem_req.addr := io.act_alu_resp.bits }
      is (e_DONE)      { io.skip_actions := (io.act_alu_resp.bits === UInt(1)) }
    }
    printf("[EXTRA] Control Unit alu resp received, state change to idle out = 0x%x @%d\n", io.act_alu_resp.bits, io.alu_req.bits.out)
  }

  when (io.act_mem_resp.valid ) {
    mu_resp := io.act_mem_resp.bits
    printf("[EXTRA] Contrlo Unit memory resp received: 0x%x, state change to idle\n", io.act_mem_resp.bits)
  }

  when (io.is_reset) {
    state := e_ready
    wait_for_done := Bool(false)
    counter_action := UInt(0)
    local1 := UInt(0)
    local2 := UInt(0)
    local3 := UInt(0)
    local4 := UInt(0)
    local5 := UInt(0)
    local6 := UInt(0)
    mu_resp := UInt(0)
  }
}
