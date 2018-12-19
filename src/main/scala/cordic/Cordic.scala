package cordic

import chisel3._
import chisel3.experimental.FixedPoint
import chisel3.util.Decoupled
import dsptools.numbers._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.subsystem.BaseSubsystem
import chisel3.util._

/**
 * Base class for CORDIC parameters
 *
 * These are type generic
 */
trait CordicParams[T <: Data] {
  val protoXY: T
  val protoZ: T
  val nStages: Int
  val correctGain: Boolean
  val stagesPerCycle: Int
}

/**
 * CORDIC parameters object for fixed-point CORDICs
 */
case class FixedCordicParams(
  // width of X and Y
  xyWidth: Int,
  // width of Z
  zWidth: Int,
  // scale output by correction factor?
  correctGain: Boolean = true,
  // number of CORDIC stages to perform per clock cycle
  stagesPerCycle: Int = 1,
  nStages:Int = 10,
) extends CordicParams[FixedPoint] {
  // prototype for x and y
  // binary point is (xyWidth-2) to represent 1.0 exactly
  val protoXY = FixedPoint(xyWidth.W, (xyWidth-2).BP)
  // prototype for z
  // binary point is (xyWidth-3) to represent Pi/2 exactly

  val protoZ = FixedPoint(zWidth.W, (zWidth-2).BP)
  val minNumber = math.pow(2.0, -(zWidth-2))
  // number of cordic stages
  private var n = 0
  while (breeze.numerics.tan(math.pow(2.0, -n)) >= minNumber) {
    n += 1
  }

}

/**
 * Bundle type that describes the input, state, and output of CORDIC
 */
class CordicBundle[T <: Data](params: CordicParams[T]) extends Bundle {
  val x: T = params.protoXY.cloneType
  val y: T = params.protoXY.cloneType
  val z: T = params.protoZ.cloneType
  val vectoring: Bool = Bool()

  override def cloneType: this.type = CordicBundle(params).asInstanceOf[this.type]
}
object CordicBundle {
  def apply[T <: Data](params: CordicParams[T]): CordicBundle[T] = new CordicBundle(params)
}

/**
 * Bundle type as IO for iterative CORDIC modules
 */
class IterativeCordicIO[T <: Data](params: CordicParams[T]) extends Bundle {
  val in = Flipped(Decoupled(CordicBundle(params)))
  val out = Decoupled(CordicBundle(params))

//  val vectoring = Input(Bool())

  override def cloneType: this.type = IterativeCordicIO(params).asInstanceOf[this.type]
}
object IterativeCordicIO {
  def apply[T <: Data](params: CordicParams[T]): IterativeCordicIO[T] =
    new IterativeCordicIO(params)
}

object AddSub {
  def apply[T <: Data : Ring](sel: Bool, a: T, b: T): T = {
    Mux(sel, a + b, a - b)
  }
}

/**
  * Mixin for top-level rocket to add a PWM
  *
  */
trait HasPeripheryCordic extends BaseSubsystem {
  // instantiate cordic chain
  val cordicChain = LazyModule(new CordicThing(FixedCordicParams(8, 10)))
  // connect memory interfaces to pbus
  pbus.toVariableWidthSlave(Some("cordicWrite")) {
    cordicChain.writeQueue.mem.get
  }
  pbus.toVariableWidthSlave(Some("cordicRead")) {
    cordicChain.readQueue.mem.get
  }
}

class IterativeCordic[T <: Data : Ring : Order : BinaryRepresentation : ConvertableTo](val params: CordicParams[T]) extends Module {
  val io = IO(IterativeCordicIO(params))

  val cycles = params.nStages / params.stagesPerCycle
  val cycles_counter = RegInit(0.U(6.W))

  when ((io.in.valid && io.out.ready && cycles_counter === 0.U) || (cycles_counter > 0.U && cycles_counter < cycles.U)) {
    cycles_counter := cycles_counter + 1.U
  } .otherwise {
    cycles_counter := 0.U
  }

  when (cycles_counter === 0.U) {
    io.in.ready := true.B
  } .otherwise {
    io.in.ready := false.B
  }


  val powers_2 = VecInit(Constants.linear(params.nStages).map(x => ConvertableTo[T].fromDouble(x)))
  val gain_factor = ConvertableTo[T].fromDouble(Constants.gain(params.nStages*params.stagesPerCycle))
  val arctans = VecInit(Constants.arctan(params.nStages).map(x => ConvertableTo[T].fromDouble(x)))

  val x = RegInit(params.protoXY.cloneType, io.in.bits.x)
  val y = RegInit(params.protoXY.cloneType, io.in.bits.y)
  val z = RegInit(params.protoZ.cloneType, io.in.bits.z)

  val x_vec = Wire(Vec(params.stagesPerCycle+1, params.protoXY.cloneType))
  val y_vec = Wire(Vec(params.stagesPerCycle+1, params.protoXY.cloneType))
  val z_vec = Wire(Vec(params.stagesPerCycle+1, params.protoZ.cloneType))

  x_vec(0) := x
  y_vec(0) := y
  z_vec(0) := z

  when (cycles_counter === 0.U) {
    x := io.in.bits.x
    y := io.in.bits.y
    z := io.in.bits.z
  } .otherwise {
    x := x_vec(params.stagesPerCycle)
    y := y_vec(params.stagesPerCycle)
    z := z_vec(params.stagesPerCycle)
  }

  val vectoringReg = Reg(Bool())

  when (cycles_counter === 0.U) {
    vectoringReg := io.in.bits.vectoring
  }

  for (i <- 1 until (params.stagesPerCycle + 1)) {
    when (vectoringReg) {
      when (y_vec(i-1) > Ring[T].zero) {
        x_vec(i) := AddSub(true.B, x_vec(i-1), y_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        y_vec(i) := AddSub(false.B, y_vec(i-1), x_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        z_vec(i) := AddSub(true.B, z_vec(i-1), arctans(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
      } .otherwise {
        x_vec(i) := AddSub(false.B, x_vec(i-1), y_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        y_vec(i) := AddSub(true.B, y_vec(i-1), x_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        z_vec(i) := AddSub(false.B, z_vec(i-1), arctans(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
      }
    } .otherwise {
      when (z_vec(i-1) < Ring[T].zero) {
        x_vec(i) := AddSub(true.B, x_vec(i-1), y_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        y_vec(i) := AddSub(false.B, y_vec(i-1), x_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        z_vec(i) := AddSub(true.B, z_vec(i-1), arctans(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
      } .otherwise {
        x_vec(i) := AddSub(false.B, x_vec(i-1), y_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        y_vec(i) := AddSub(true.B, y_vec(i-1), x_vec(i-1) * powers_2(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
        z_vec(i) := AddSub(false.B, z_vec(i-1), arctans(i + (cycles_counter*params.stagesPerCycle) - (params.stagesPerCycle + 1)))
      }
    }
  }

  when (cycles_counter === cycles.U) {
    io.out.valid := true.B
  } .otherwise {
    io.out.valid := false.B
  }

  val gain_final = Mux(params.correctGain.B, ConvertableTo[T].fromDouble(1/Constants.gain(params.nStages)), Ring[T].one)

  io.out.bits.x := gain_final * x
  io.out.bits.y := gain_final * y
  io.out.bits.z := z
  io.out.bits.vectoring := io.in.bits.vectoring

}
