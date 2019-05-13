package efficiency.power_on_policies.decision.probabilistic

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.DistributionUtils
import efficiency.power_on_policies.decision.PowerOnDecision

/**
 * Created by dfernandez on 22/1/16.
 */
class GammaPowerOnDecision(threshold : Double, windowSize: Int, lostFactor : Double = 0.25) extends PowerOnDecision with DistributionUtils{

  override def shouldPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Boolean = {
    val allPastTuples = getPastTuples(cellState, windowSize)
    var should = false
    val jobAttributes = getJobAttributes(allPastTuples)

    if(jobAttributes._1 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._5 > 0.0){
      val alphaCpu = (cellState.availableCpus - (cellState.numberOfMachinesOn * cellState.cpusPerMachine * lostFactor)) / jobAttributes._5
      val alphaMem = (cellState.availableMem - (cellState.numberOfMachinesOn * cellState.memPerMachine * lostFactor)) / jobAttributes._3
      //FIXME: en la implementación anterior teníamos un floor de (alphacpu+alphamem) /2 y le sumábamos 1
      val lastTuple = allPastTuples.maxBy(tuple => tuple._1)
      val prob = getGammaDistributionCummulativeProbability( Math.min(alphaCpu,alphaMem), jobAttributes._1, Math.max(jobAttributes._1+cellState.powerOnTime, cellState.simulator.currentTime - lastTuple._1))
      should = prob > threshold
    }
    should
  }

  override val name: String = ("gamma-on-threshold:%f-window:%d-lost:%f").format(threshold,windowSize, lostFactor)
}
