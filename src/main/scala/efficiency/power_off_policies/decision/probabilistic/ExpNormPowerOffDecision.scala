package efficiency.power_off_policies.decision.probabilistic

import ClusterSchedulingSimulation.{CellState, Job}
import efficiency.DistributionUtils
import efficiency.power_off_policies.decision.PowerOffDecision
import org.apache.commons.math.distribution.{ExponentialDistributionImpl, GammaDistributionImpl, NormalDistributionImpl}

/**
 * Created by dfernandez on 22/1/16.
 */
class ExpNormPowerOffDecision(normalThreshold: Double, threshold : Double, windowSize: Int, ts: Double = 130.0) extends PowerOffDecision with DistributionUtils{
  override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = {
    var should = false
    val allPastTuples = getPastTuples(cellState, windowSize)
    val jobAttributes = getJobAttributes(allPastTuples)
    if(jobAttributes._1 > 0.0){
      val interArrival = getNormalDistributionInverseCummulativeProbability(jobAttributes._1, jobAttributes._2, 1-normalThreshold)
      val prob = getExponentialDistributionCummulativeProbability(interArrival, ts)
      should = prob >= threshold
    }
    should
  }

  override val name: String = ("exponential-normal-power-off-decision-with-normal-threshold:%f-ts:%f-threshold:%f-and-window-size:%d").format(normalThreshold,ts,threshold,windowSize)
}
