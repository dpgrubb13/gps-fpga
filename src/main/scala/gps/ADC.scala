package gps

import chisel3._
import chisel3.util._
import dspblocks._
import dsptools.numbers._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._

abstract class AdcModule
(
  val inputWidth: Int = 4
  val streamParameters: AXI4StreamMasterParameters = AXI4StreamMasterParameters()
)(implicit p: Parameters) extends LazyModule with HasCSR {  
  
  val streamNode = AXI4StreamMasterNode(streamParameters)

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.out.length == 1)

    val out = streamNode.out.head._1 // get the output bundle associated with the AXI4Stream node

    val io = IO(val adcIn = Input(SInt(inputWidth.W)))
    BoringUtils.addSink(io.adcIn, "adcIO")
    
    out.bits.data := io.adcIn
    out.bits.last := false.B

    regmap(
      0x0 -> Seq(RegField.w(8, out.valid)),  //TODO connect to valid? Does width have to be by byte?
    )
  }

}

class TLAdcModule (
  inputWidth: Int = 4,
  csrAddress: AddressSet = AddressSet(0x2000, 0xff), //TODO use this address offset properly
  beatBytes: Int = 8,
)(implicit p: Parameters) extends AdcModule(inputWidth) with TLHasCSR {
  val devname = "tlAdc" //TODO what is this and can it be an arbitrary name?
  val devcompat = Seq("ucb-art", "dsptools")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  //make diplomatic TL node for regmap
  //TODO what are the parts of this call?
  override val mem = Some(TLRegisterNode(address = Seq(csrAddress), device = device, beatByte = beatBytes))
}


class AXI4AdcModule (
  val inputWidth: Int = 4,
  val csrAddress: AddressSet = AddressSet(0x0, 0xffff)
  val beatBytes: Int = 8
)(implicit p: Parameters) extends AdcModule(inputWidth=inputWidth) with AXI4HasCSR {
  override val mem = Some(AXI4RegisterNode(address = csrAddress, beatBytes = beatBytes))
}
