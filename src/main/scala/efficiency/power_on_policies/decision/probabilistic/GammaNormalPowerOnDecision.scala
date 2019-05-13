package efficiency.power_on_policies.decision.probabilistic

import ClusterSchedulingSimulation.{ClaimDelta, CellState, Job}
import efficiency.DistributionUtils
import efficiency.power_off_policies.decision.PowerOffDecision
import efficiency.power_on_policies.decision.PowerOnDecision
import org.apache.commons.math.distribution.{GammaDistributionImpl, NormalDistributionImpl}

/**
  * Created by dfernandez on 22/1/16.
  */
class GammaNormalPowerOnDecision(normalThreshold: Double, threshold : Double, windowSize: Int, lostFactor : Double = 0.25) extends PowerOnDecision with DistributionUtils{

  override def shouldPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Boolean = {
    var should = false
    val allPastTuples = getPastTuples(cellState, windowSize)
    val jobAttributes = getJobAttributes(allPastTuples)
    if(jobAttributes._1 > 0.0 && jobAttributes._2 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._4 > 0.0 && jobAttributes._5 > 0.0 && jobAttributes._6 > 0.0){
      val alphaCpu = (cellState.availableCpus - (cellState.numberOfMachinesOn * cellState.cpusPerMachine * lostFactor)) / getNormalDistributionInverseCummulativeProbability(jobAttributes._5, jobAttributes._6, normalThreshold)
      val alphaMem = (cellState.availableMem - (cellState.numberOfMachinesOn * cellState.memPerMachine * lostFactor))  / getNormalDistributionInverseCummulativeProbability(jobAttributes._3, jobAttributes._4, normalThreshold)
      //FIXME: en la implementación anterior teníamos un floor de (alphacpu+alphamem) /2 y le sumábamos 1
      if(alphaCpu > 0.0 || alphaMem > 0.0) {
        var beta = getNormalDistributionInverseCummulativeProbability(jobAttributes._1, jobAttributes._2, 1-normalThreshold)
        if (beta < 0)
          beta = 0.1
        val lastTuple = allPastTuples.maxBy(tuple => tuple._1)
        val prob = getGammaDistributionCummulativeProbability( Math.min(alphaCpu,alphaMem), beta, Math.max(jobAttributes._1+cellState.powerOnTime, cellState.simulator.currentTime - lastTuple._1))
        should = prob > threshold
      }
    }
    should
  }

  override val name: String = ("gamma-normal:%f-threshold:%f-window:%d-lost:%f").format(normalThreshold,threshold,windowSize,lostFactor)
}
