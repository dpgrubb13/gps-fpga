package gps

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import dsptools.numbers._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.subsystem.BaseSubsystem
import chisel3.util._

trait GPSParams[T <: Data] {
  val sampleWidth: Int  //ADC sample width
  val dataWidth: Int    //dummy data width
}

class GPSIO[T <: Data](params: GPSParams[T]) extends Bundle {
  val in = Flipped(Decoupled(SInt(params.sampleWidth.W)))
  val out = Decoupled(SInt(params.sampleWidth.W))

  val inData = Input(UInt(params.dataWidth.W))
  val outData = Output(UInt(params.dataWidth.W))

  override def cloneType: this.type = GPSIO(params).asInstanceOf[this.type]
}
object GPSIO {
  def apply[T <: Data](params: GPSParams[T]): GPSIO[T] =
    new GPSIO(params)
}


/**
  * Mixin for top-level rocket to add a PWM
  *
  */
trait HasPeripheryGPS extends BaseSubsystem {
  // instantiate cordic chain
  val gpsChain = LazyModule(new GPSThing(GPSParams(32, 32)))
  // connect memory interfaces to pbus
  //pbus.toVariableWidthSlave(Some("cordicWrite")) {
  //  cordicChain.writeQueue.mem.get
  //}
  pbus.toVariableWidthSlave(Some("gpsRead")) {
    cordicChain.readQueue.mem.get
  }
}

class GPS[T <: Data](val params: GPSParams[T]) extends Module {
  val io = IO(GPSIO(params))

  io.out <> io.in
  io.outData := io.inData + 1.U

}
