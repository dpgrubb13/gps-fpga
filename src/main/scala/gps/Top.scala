package gps

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch


class Top(val adcWidth: Int) extends Module {
  val io = IO(new Bundle) {
    val adcClk = Input(Clock())
    val sample = Input(SInt(adcWidth.W)) //TODO set to proper width or parameterize
  })
  
  withClock(io.adcClk) {
    BoringUtils.addSource(io.sample, "adcIO")
    val hw = LazyModule(new GpsThing).module
  }
}
