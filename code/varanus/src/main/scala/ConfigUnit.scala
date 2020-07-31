package varanus

import Chisel._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.property._
import freechips.rocketchip.config._
import freechips.rocketchip.rocket._
import Chisel.ImplicitConversions._
import TinyALU._

trait ActionConfigParameters {
  implicit val p: Parameters
  val OP_SZ = 6
  val IN_OPERAND_SZ = 12
  val OUT_OPERAND_SZ = 11
  val CONF_SZ = 16
}

trait ActionConfigEnums {
  // Enum for filling the action table: OP | IN1 | IN2 | FN | OUT | DATA(xLen)
  val (e_SA_OP :: e_SA_IN1 :: e_SA_IN2 :: e_SA_FN :: e_SA_OUT :: e_SA_DATA :: e_SA_DONE :: e_SA_COUNT :: Nil) = Enum(UInt(), 8)
  // Enum for specifying OP type
  val (e_OP_INTR :: e_OP_MEM_WR :: e_OP_MEM_RD :: e_OP_ALU :: e_OP_MEM_XA_ADD :: Nil) = Enum(UInt(), 5)
  // Enum for specifying operand (in1 and in2) type
  val (e_IN_DATA_MU :: e_IN_ADDR_MU :: e_IN_CONST :: e_IN_LOC1 :: e_IN_LOC2 :: e_IN_QUEUE :: e_IN_DATA_RESP :: e_IN_LOC3 :: e_IN_LOC4 :: e_IN_LOC5 :: e_IN_LOC6 :: e_IN_COMPRESSED :: Nil) = Enum(UInt(), 12)
  val (e_OUT_LOC1 :: e_OUT_LOC2 :: e_OUT_QUEUE :: e_OUT_DATA :: e_OUT_ADDR :: e_INTR :: e_OUT_LOC3 :: e_OUT_LOC4 :: e_OUT_LOC5 :: e_OUT_LOC6 :: e_DONE :: Nil) = Enum(UInt(), 11)

}

class ActionConfigTable(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with ActionConfigParameters with HasCoreParameters {
  val op = UInt(width=log2Up(OP_SZ))
  val in1 = UInt(width=log2Up(IN_OPERAND_SZ))
  val in2 = UInt(width=log2Up(IN_OPERAND_SZ))
  val fn = UInt(width=SZ_ALU_FN)
  val out = UInt(width=log2Up(OUT_OPERAND_SZ))
  val data = UInt(width=xLen)
}

class ActionALU(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with ActionConfigParameters with HasCoreParameters {
  val fn = Bits(width=SZ_ALU_FN)
  val in1 = UInt(width=log2Up(IN_OPERAND_SZ))
  val in2 = UInt(width=log2Up(IN_OPERAND_SZ))
  val out = UInt(width=log2Up(OUT_OPERAND_SZ))
  val data = UInt(width=xLen)
}

class ActionMem(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with ActionConfigParameters {
  val mem_op = UInt(width=5) 
  val data = UInt(width=log2Up(IN_OPERAND_SZ))
  val addr = UInt(width=log2Up(IN_OPERAND_SZ))
  val tag = UInt(width=10)
}

// The action response specifies the action type of each corresponding MU:
// interrupt, shared memory request, alu request
class ActionResp(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with ActionConfigParameters with HasCoreParameters {
  val intr = Bool()
  val alu_req = Valid(new ActionALU)
  val mem_req = Valid(new ActionMem)
}

class ActionConfigUnitInterface(implicit val p: Parameters)
    extends ParameterizedBundle with ActionConfigParameters with HasCoreParameters {
  val cmd = Valid(new KomodoMatchUnitReq).flip // The input cmd (commit log)
  val req = Bool(INPUT) // The request coming from the Control Unit
  val cu_wait = Bool(INPUT)
  val skip_actions = Bool(INPUT)
  val resp = new ActionResp()
  val act_done = Bool(OUTPUT)
  val conf_read = Valid(UInt(width=xLen))
}

class ActionConfigUnit(id: Int)(implicit val p: Parameters)
    extends Module with KomodoParameters with KomodoEnums with ActionConfigParameters with ActionConfigEnums with HasCoreParameters {
  val io = new ActionConfigUnitInterface
  
  val conf_list = Reg(Vec(CONF_SZ, new ActionConfigTable))
  val ctrl = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(1))
  val is_reset = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(2))
  val set_conf = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(5))
  val read_conf = io.cmd.valid && (io.cmd.bits.inst.funct === UInt(13))
  val action = io.cmd.bits.rs1(63,32)
  val data = io.cmd.bits.rs2
  val counter = RegInit(UInt(0, width=log2Up(CONF_SZ)))
  val act_ptr = RegInit(UInt(0, width=log2Up(CONF_SZ)))
  
  io.act_done := Bool(false)
  io.resp.mem_req.valid := Bool(false)
  io.resp.alu_req.valid := Bool(false)
  io.resp.intr := Bool(false)
  io.resp.mem_req.bits.tag := UInt(id)
  io.resp.mem_req.bits.mem_op := M_XWR

  assert((counter - UInt(1)) =/= CONF_SZ.U)

  when (is_reset) {
    act_ptr := UInt(0)
  }

  when (ctrl) {
    when (action == (e_C_RESET) ) {
      act_ptr := UInt(0)
      counter := UInt(0)
    }
  }

  when (set_conf) {
    printf("[EXTRA] MU%d config cmd: %d action: %d, data: %d\n", UInt(id), io.cmd.bits.inst.funct, action, data)
    switch (action) {
      is (e_SA_DONE) {
        counter := counter + UInt(1)
      }
      is (e_SA_OP) {
        conf_list(counter).op := data
      }
      is (e_SA_IN1) {
        conf_list(counter).in1 := data
      }
      is (e_SA_IN2) {
        conf_list(counter).in2 := data
      }
      is (e_SA_FN) {
        conf_list(counter).fn := data
      }
      is (e_SA_OUT) {
        conf_list(counter).out := data
      }
      is (e_SA_DATA) {
        conf_list(counter).data := data
      }
    }
  }

  when (read_conf) {
    switch (action) {
      is (e_SA_COUNT) {
        io.conf_read.bits := counter
      }
      is (e_SA_DONE) {
        counter := counter - UInt(1)
      }
      is (e_SA_OP) {
        io.conf_read.bits := conf_list(counter-UInt(1)).op
        conf_list(counter - UInt(1)).op := UInt(0)
      }
      is (e_SA_IN1) {
        io.conf_read.bits := conf_list(counter-UInt(1)).in1
        conf_list(counter - UInt(1)).in1 := UInt(0)
      }
      is (e_SA_IN2) {
        io.conf_read.bits := conf_list(counter-UInt(1)).in2
        conf_list(counter - UInt(1)).in2 := UInt(0)
      }
      is (e_SA_FN) {
        io.conf_read.bits := conf_list(counter-UInt(1)).fn
        conf_list(counter - UInt(1)).fn := UInt(0)
      }
      is (e_SA_OUT) {
        io.conf_read.bits := conf_list(counter-UInt(1)).out
        conf_list(counter - UInt(1)).out := UInt(0)
      }
      is (e_SA_DATA) {
        io.conf_read.bits := conf_list(counter-UInt(1)).data
        conf_list(counter - UInt(1)).data := UInt(0)
      }
    }
    io.conf_read.valid := Bool(true)
  }

  when (io.cu_wait) {
    printf("[EXTRA] Config Unit%d request with operation(%d): %d size: %d\n", UInt(id), act_ptr, conf_list(act_ptr).op, counter)
    act_ptr := act_ptr + UInt(1)
    when (act_ptr === counter) {
      act_ptr := UInt(0)
      io.act_done := Bool(true)
    }
    .otherwise {
      switch(conf_list(act_ptr).op) {
        is (e_OP_INTR) {
          io.resp.intr := Bool(true)
          io.resp.mem_req.valid := Bool(false)
          io.resp.alu_req.valid := Bool(false)
        }
        is (e_OP_MEM_WR) {
          printf("[EXTRA] Config Unit memory write\n")
          io.resp.intr := Bool(false)
          io.resp.alu_req.valid := Bool(false)
          io.resp.mem_req.valid := Bool(true)
          io.resp.mem_req.bits.mem_op := M_XWR
          io.resp.mem_req.bits.data := conf_list(act_ptr).in1
          io.resp.mem_req.bits.addr := conf_list(act_ptr).in2
        }
        is (e_OP_MEM_RD) {
          printf("[EXTRA] Config Unit memory read\n")
          io.resp.intr := Bool(false)
          io.resp.mem_req.valid := Bool(true)
          io.resp.mem_req.bits.mem_op := M_XRD
          io.resp.mem_req.bits.data := conf_list(act_ptr).in1
          io.resp.mem_req.bits.addr := conf_list(act_ptr).in2
        }
        is (e_OP_ALU) {
          printf("[EXTRA] Config Unit ALU operation\n")
          io.resp.intr := Bool(false)
          io.resp.mem_req.valid := Bool(false)
          io.resp.alu_req.bits <> conf_list(act_ptr)
          io.resp.alu_req.valid := Bool(true)
        }
      }
    }
  }

  when (io.skip_actions) {
    act_ptr := UInt(0)
    printf("[EXTRA] Config Unit skipping rest of the actions\n")
  }
}
