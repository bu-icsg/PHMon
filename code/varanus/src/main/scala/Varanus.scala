package freechips.rocketchip.rocket

import Chisel._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.property._
import freechips.rocketchip.config.Parameters
import Chisel.ImplicitConversions._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.tile._
import freechips.rocketchip.config.{Parameters,Field}

case object DebugCommitLog extends Field[Boolean]

class CommitLog(implicit val p: Parameters)
    extends ParameterizedBundle()(p) with HasCoreParameters {
  val pc_src = UInt(width = xLen) // The current pc
  val pc_dst = UInt(width = xLen) // The next pc
  val inst = UInt(width = 32) // The expanded instruction
  val addr = UInt(width = xLen) // Addr of register or memory
  val data = UInt(width = xLen) // The corrsponding data of register or memory
  val priv = UInt(width = 2) // The privilege mode
  val is_compressed = Bool() // Indicating whether the instruction is
                             //compressed or not
  val interrupt_replay = Bool() // Replay interrupt request to address
                                //interrupt drop issue
}
