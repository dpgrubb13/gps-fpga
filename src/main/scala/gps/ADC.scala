package gps

import chisel3._
import chisel3.util._
import dspblocks._
import dsptools.numbers._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._


class AdcModule
(
  val inputWidth: Int = 3
  val csrAddress: AddressSet = AddressSet(0x2000, 0xff) //TODO use this address offset properly
  val streamParameters: AXI4StreamMasterParameters = AXI4MasterSlaveParameters()
)(implicit p: Parameters) extends LazyModule with HasCSR { //TODO with HasCSR?
  val streamNode = AXI4MasterSlaveNode(streamParameters)

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.out.length == 1)

    val out = streamNode.out.head

    BoringUtils.addSink(out.bits.data, "adcIO")
    
    regmap(
      0x0 -> Seq(RegField.r(1, out.valid)),  //TODO connect to valid?
    )
  }

}
