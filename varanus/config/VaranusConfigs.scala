//package rocketchip
package freechips.rocketchip.system

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.util.property._
import freechips.rocketchip.config.Parameters
import Chisel.ImplicitConversions._
//import varanus.{Komodo, DefaultKomodoConfig, DebugKomodo}
import varanus._

class KomodoConfig extends Config ((site, here, up) => {
  case RocketTilesKey => up(RocketTilesKey, site) map { r =>
    r.copy(rocc =
      Seq(
        RoCCParams(
          opcodes = OpcodeSet.custom1,
          generator = (p: Parameters) => {
            val komodo = LazyModule(new PHMonRoCC()(p))
            komodo})
      )
    )
  }
  case DebugCommitLog => false
  case DebugKomodo => false
})

class VaranusDebug extends Config ((site, here, up) => {
  case DebugCommitLog => true
})

class KomodoDebug extends Config ((site, here, up) => {
  case DebugKomodo => true
})

class KomodoBaseConfig extends Config(new KomodoConfig ++ new DefaultKomodoConfig)

class KomodoCppConfig extends Config(new KomodoConfig ++ new DefaultKomodoConfig ++ new DefaultConfig)

class KomodoCppDebugConfig extends Config(new KomodoDebug ++ new KomodoCppConfig)

class KomodoCppCores2Config extends Config(new DualCoreConfig ++  new KomodoConfig ++ new DefaultKomodoConfig)
