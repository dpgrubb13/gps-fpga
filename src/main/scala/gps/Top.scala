package gps

import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch


class Top extends Module {
  val io = IO(new Bundle {
    val adcClk = Input(Clock())
    val sample = Input(SInt(3.W)) //TODO set to proper width or parameterize
  })
  
  withClock(io.adcClk) {
    BoringUtils.addSource(io.sample, "adcIO")
    val hw = LazyModuel(new Thing).module
  }
}



//class ExampleTop(implicit p: Parameters) extends RocketSubsystem
//    with CanHaveMasterAXI4MemPort
//    with HasPeripheryBootROM
//    with HasSyncExtInterrupts {
//  override lazy val module = new ExampleTopModule(this)
//}
//
//class ExampleTopModule[+L <: ExampleTop](l: L) extends RocketSubsystemModuleImp(l)
//    with HasRTCModuleImp
//    with CanHaveMasterAXI4MemPortModuleImp
//    with HasPeripheryBootROMModuleImp
//    with HasExtInterruptsModuleImp
//    with DontTouch
//
//class ExampleTopWithGPS(implicit p: Parameters) extends ExampleTop
//    with HasPeripheryGPS {
//  override lazy val module = new ExampleTopModule(this)
//}
