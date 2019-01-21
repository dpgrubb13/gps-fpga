package gps

import chisel3._
import chisel3.util._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

//instantiate Dsp Queue in thing module

class GpsThing
(
  val width: Int = 4
)(implicit p: Parameters) extends LazyModule {
  //instantiate lazy modules
  val adc = LazyModule(new AdcModule(inputWidth = width)) //TODO fix these instantiations
  val dspQueue = LazyModule(new AXI4DspQueue(depth=16))
  val readQueue = LazyModule(new ReadQueue(depth))

  readQueue.streamNode := dspQueue.streamNode := writeQueue.streamNode

  lazy val module = new LazyModuleImp(this)
}
