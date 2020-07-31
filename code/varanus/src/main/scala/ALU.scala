package varanus

import Chisel._
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.property._
import freechips.rocketchip.config.Parameters
import Chisel.ImplicitConversions._

object TinyALU {
  val SZ_ALU_FN = 4
  val FN_X   = BitPat("b????")
  val FN_ADD = UInt(0)
  val FN_SUB = UInt(1)
  val FN_SL  = UInt(2)
  val FN_SR  = UInt(3)
  val FN_SLT = UInt(4) // set less than
  val FN_SEQ = UInt(5) // set equal
  val FN_AND = UInt(6)
  val FN_OR  = UInt(7)
  val FN_XOR = UInt(8)
  val FN_NOP = UInt(9)

  def isSub(cmd: UInt) = cmd === FN_SUB
  def isLogic(cmd: UInt) = (cmd === FN_AND || cmd === FN_OR || cmd === FN_XOR)
  def isShift(cmd: UInt) = (cmd === FN_SL || cmd === FN_SR)
}
import TinyALU._

class TinyALU(implicit val p: Parameters)
    extends Module with HasCoreParameters {
  val io = new Bundle {
    val fn  = Bits(INPUT, SZ_ALU_FN)
    val in2 = UInt(INPUT, xLen)
    val in1 = UInt(INPUT, xLen)
    val out = UInt(OUTPUT, xLen)
    val cmp_out = Bool(OUTPUT)
  }

  val in2_inv = Mux(isSub(io.fn), ~io.in2, io.in2)

  // XOR, AND, OR
  val in1_xor_in2 = io.in1 ^ io.in2
  val in1_and_in2 = io.in1 & io.in2
  val in1_or_in2 = io.in1 | io.in2 
  val logic = Mux(io.fn === FN_XOR, in1_xor_in2, Mux(io.fn === FN_AND, in1_and_in2, Mux(io.fn === FN_OR, in1_or_in2, UInt(0))))

  // ADD, SUB
  val in1_add_in2 = io.in1 + in2_inv + isSub(io.fn)

  // SEQ, SLT
  val cmp_out = Mux(io.fn === FN_SEQ, in1_xor_in2 === UInt(0),
    Mux(io.fn === FN_SLT, io.in1 < io.in2, UInt(0)))

  // SL, SR
  val shamt = Cat(io.in2(5), io.in2(4,0))
  val shift_logic = Mux(io.fn === FN_SL, (io.in1 << shamt)(xLen-1,0),
   Mux(io.fn === FN_SR, (io.in1 >> shamt)(xLen-1,0), io.in1))

  val out = Mux(io.fn === FN_ADD || io.fn === FN_SUB, in1_add_in2,
    Mux(isLogic(io.fn), logic,
      Mux(isShift(io.fn), shift_logic, UInt(0))))

  io.cmp_out := cmp_out
  io.out := Mux(cmp_out === UInt(1), cmp_out, out)

}
