package gps

import chisel3._
import chisel3.util._
import dspblocks._
import freechips.rocketchip.amba.axi4stream._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

//start by writing ReadQueue that is purely an AXI version
class AXI4ReadQueue[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4EdgeParameters, AXI4EdgeParameters, AXI4Bundle](
  val depth: Int = 8, 
  val streamParameters: AXI4StreamSlaveParameters = AXI4StreamSlaveParameters()
  val csrAddress: AddressSet = AddressSet(0x0, 0xffff)
  val beatBytes: Int = 8
)(implicit p: Parameters)
  extends DspBlock[AXI4MasterPortParameters, AXI4SlavePortParameters, AXI4EdgeParameters, AXI4EdgeParameters, AXI4Bundle] with AXI4HasCSR {

  val streamNode = AXI4StreamSlaveNode(streamParameters)
  
  lazy val module  new LazyModuleImp(this) {
    val (in, _) = streamNode.in.unzip

    val width = in.params.n * 8
    val queue = Module(new Queue(UInt(in.params.dataBits.W), depth)) //TODO checkout definition of dataBits
    queue.io.enq.valid := in.valid
    queue.io.enq.bits := in.bits.data
    in.bits.last := false.B
    in.ready := queue.io.enq.ready
    
    regmap(
      0x0 -> Seq(RegField.r(width, queue.io.deq)),
      (width+7)/8 -> Seq(RegField.r(width, queue.io.count))
    )

  }

  override val mem = Some(AXI4RegisterNode(address = csrAddress, beatBytes = beatBytes))

}



/**
  * The streaming interface adds elements into the queue.
  * The memory interface can read elements out of the queue.
  * @param depth number of entries in the queue
  * @param streamParameters parameters for the stream node
  * @param p
  */
abstract class ReadQueue
(
  val depth: Int = 8,
  val streamParameters: AXI4StreamSlaveParameters = AXI4StreamSlaveParameters()
)(implicit p: Parameters) extends LazyModule with HasCSR {
  val streamNode = AXI4StreamSlaveNode(streamParameters)

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.in.length == 1)

    val in = streamNode.in(0)._1
    val width = in.params.n * 8
    val queue = Module(new Queue(UInt(in.params.dataBits.W), depth))
    queue.io.enq.valid := in.valid
    queue.io.enq.bits := in.bits.data
    in.bits.last := false.B
    in.ready := queue.io.enq.ready

    regmap(
      0x0 -> Seq(RegField.r(width, queue.io.deq)),
      (width+7)/8 -> Seq(RegField.r(width, queue.io.count)),
    )
  }
}


//TODO need to make an AXI flavor of read queue instead of TL
/**
  * TLDspBlock specialization of ReadQueue
  * @param depth number of entries in the queue
  * @param csrAddress address range
  * @param beatBytes beatBytes of TL interface
  * @param p
  */
class TLReadQueue
(
  depth: Int = 8,
  csrAddress: AddressSet = AddressSet(0x2100, 0xff), //TODO change addressing?
  beatBytes: Int = 8
)(implicit p: Parameters) extends ReadQueue(depth) with TLHasCSR {
  val devname = "tlQueueOut"
  val devcompat = Seq("ucb-art", "dsptools")
  val device = new SimpleDevice(devname, devcompat) {
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(name, mapping)
    }
  }
  // make diplomatic TL node for regmap
  override val mem = Some(TLRegisterNode(address = Seq(csrAddress), device = device, beatBytes = beatBytes))

}


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




abstract class GpsBlock[D, U, EO, EI, B <: Data, T <: Data : Ring : Order : ConvertableTo]
(
  val GPSParams: GPSParams[T]
)(implicit p: Parameters) extends DspBlock[D, U, EO, EI, B] {
  val streamNode = AXI4StreamIdentityNode()
  val mem = None

  lazy val module = new LazyModuleImp(this) {
    require(streamNode.in.length == 1)
    require(streamNode.out.length == 1)

    val in = streamNode.in.head._1
    val out = streamNode.out.head._1

    val gps = Module(new GPS[T](GPSParams))

    gps.io.in.valid := in.valid
    in.ready := gps.io.in.ready
    gps.io.in.bits := in.bits.data
    in.bits.last := false.B

    out.valid := gps.io.out.valid
    gps.io.out.ready := out.ready
    out.bits.data := gps.io.out.bits
    out.bits.last := false.B
  }
}

class TLGpsBlock[T <: Data : Ring : Order : ConvertableTo]
(
GPSParams: GPSParams[T]
)(implicit p: Parameters) extends
  GpsBlock[TLClientPortParameters, TLManagerPortParameters, TLEdgeOut, TLEdgeIn, TLBundle, T](GPSParams) with TLDspBlock

//TODO create AXI version of GpsBlock

