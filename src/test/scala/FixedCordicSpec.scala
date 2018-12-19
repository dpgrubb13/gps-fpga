package cordic

import dsptools.numbers._
import org.scalatest.{FlatSpec, Matchers}

class FixedCordicSpec extends FlatSpec with Matchers {
  behavior of "FixedIterativeCordic"

  val params = FixedCordicParams(
    xyWidth = 12,
    zWidth = 12,
    correctGain = true,
    stagesPerCycle = 1,
    nStages = 10
  )
  it should "rotate" in {
    val baseTrial = XYZ(xin=1.0, yin=0.0, zin=0.0, vectoring=false)
    val angles = Seq(-1, -0.5, 0, 0.25, 0.5, 1)
    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi))) }
    FixedCordicTester(params, trials) should be (true)
  }

// <<<<<<< lab2release
//  behavior of "RealIterativeCordic"
//
//  val realParams = new CordicParams[DspReal] {
//    val protoXY = DspReal()
//    val protoZ = DspReal()
//    val nStages = 28
//    val correctGain = true
//    val stagesPerCycle = 1
//  }
//  it should "rotate" in {
//    val baseTrial = XYZ(xin=1.0, yin=0.0, zin=0.0, vectoring=false)
//    val angles = Seq(-1, -0.5, 0, 0.25, 0.5, 1)
//    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi))) }
//    RealCordicTester(realParams, trials) should be (true)
//  }
// =======
  //test loop unrolling for rotation mode
//  val params = FixedCordicParams(
//    xyWidth = 10,
//    zWidth = 16,
//    correctGain = true,
//    stagesPerCycle = 2,
//    nStages = 10
//  )
//  it should "rotate" in {
//    val baseTrial = XYZ(xin=1.0, yin=0.0, zin=0.0, vectoring=false)
//    val angles = Seq(-1, -0.5, 0, 0.25, 0.5, 1)
//    val trials = angles.map { phi => baseTrial.copy(zin = phi, xout = Some(math.cos(phi))) }
//    FixedCordicTester(params, trials) should be (true)
//  }

//  //test vectoring mode without loop unrolling
//  val params = FixedCordicParams(
//    xyWidth = 10,
//    zWidth = 16,
//    correctGain = true,
//    stagesPerCycle = 1,
//    nStages = 10
//  )
//  it should "vector" in {
//    val baseTrial = XYZ(xin=1.0, yin=0.5, zin=0.0, vectoring=true)
//    val x_seq = Seq(1.0, 0.5, 0.25)
//    val y_seq = Seq(0.25, 0.5, 1.0)
//    val trials = x_seq.zip(y_seq).map { case (x, y) => baseTrial.copy(xin = x, yin = y, zout = Some(math.atan(y/x))) }
//    FixedCordicTester(params, trials) should be (true)
//  }

  //test vectoring mode with loop unrolling
//  val params = FixedCordicParams(
//    xyWidth = 10,
//    zWidth = 16,
//    correctGain = true,
//    stagesPerCycle = 2,
//    nStages = 10
//  )
//  it should "vector" in {
//    val baseTrial = XYZ(xin=1.0, yin=0.0, zin=0.0, vectoring=true)
//    val x_seq = Seq(1.0, 0.5, 0.25)
//    val y_seq = Seq(0.25, 0.5, 1.0)
//    val trials = x_seq.zip(y_seq).map { case (x, y) => baseTrial.copy(xin = x, yin = y, zout = Some(math.atan(y/x))) }
//    FixedCordicTester(params, trials) should be (true)
//  }
// >>>>>>> master

}
