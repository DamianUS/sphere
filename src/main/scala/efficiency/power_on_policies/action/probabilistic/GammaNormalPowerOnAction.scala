package efficiency.power_on_policies.action.probabilistic

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.DistributionUtils
import efficiency.power_on_policies.action.PowerOnAction

/**
 * Created by dfernandez on 15/1/16.
 */
class GammaNormalPowerOnAction(normalThreshold: Double, threshold : Double, windowSize: Int, lostFactor: Double = 0.3) extends PowerOnAction with DistributionUtils{

  override val name: String = ("gamma-normal:%f-threshold:%f-window:%d-lost:%f").format(normalThreshold,threshold,windowSize)

  override def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Int = {
    var numMachinesPowerOn = 0
    var gammaProbability = 10000.0;
    val allPastTuples = getPastTuples(cellState, windowSize)
    val jobAttributes = getJobAttributes(allPastTuples)
    do{
      if(jobAttributes._1 > 0.0 && jobAttributes._2 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._4 > 0.0 && jobAttributes._5 > 0.0 && jobAttributes._6 > 0.0){
        numMachinesPowerOn += 1
        val alphaCpu = (cellState.availableCpus + (cellState.cpusPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn) * cellState.cpusPerMachine * lostFactor)) / Math.max(0.01, getNormalDistributionInverseCummulativeProbability(jobAttributes._5, jobAttributes._6, normalThreshold))
        val alphaMem = (cellState.availableMem + (cellState.memPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn) * cellState.memPerMachine * lostFactor)) / Math.max(0.01, getNormalDistributionInverseCummulativeProbability(jobAttributes._3, jobAttributes._4, normalThreshold))
        var beta = getNormalDistributionInverseCummulativeProbability(jobAttributes._1, jobAttributes._2, 1-normalThreshold)
        if (beta < 0 )
          beta = 0.1
        //FIXME: en la implementación anterior teníamos un floor de (alphacpu+alphamem) /2 y le sumábamos 1
        if(alphaCpu > 0.0 || alphaMem > 0.0) {
          val lastTuple = allPastTuples.maxBy(tuple => tuple._1)
          val prob = getGammaDistributionCummulativeProbability( Math.min(alphaCpu,alphaMem), beta, Math.max(jobAttributes._1+cellState.powerOnTime, cellState.simulator.currentTime - lastTuple._1))
          gammaProbability = prob
        }
      }
    }while(jobAttributes._1 > 0.0 && jobAttributes._2 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._4 > 0.0 && jobAttributes._5 > 0.0 && jobAttributes._6 > 0.0 && gammaProbability > threshold && numMachinesPowerOn < cellState.numMachines)
    numMachinesPowerOn
  }
}
