package efficiency.power_on_policies.action.probabilistic

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.DistributionUtils
import efficiency.power_on_policies.action.PowerOnAction

/**
  * Created by dfernandez on 15/1/16.
  */
class GammaPowerOnAction(threshold : Double, windowSize: Int, lostFactor : Double = 0.25) extends PowerOnAction with DistributionUtils{


  override val name: String = ("gamma-threshold:%f-window:%d-lost:%f").format(threshold,windowSize,lostFactor)

  override def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Int = {
    var numMachinesPowerOn = 0
    val allPastTuples = getPastTuples(cellState, windowSize)
    val jobAttributes = getJobAttributes(allPastTuples)
    var gammaProbability = 1000000.0
    do{
      if(jobAttributes._1 > 0.0 && jobAttributes._2 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._4 > 0.0 && jobAttributes._5 > 0.0 && jobAttributes._6 > 0.0){
        numMachinesPowerOn += 1
        val alphaCpu = (cellState.availableCpus + (cellState.cpusPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn) * cellState.cpusPerMachine * lostFactor)) / jobAttributes._5
        val alphaMem = (cellState.availableMem + (cellState.memPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn ) * cellState.memPerMachine * lostFactor)) / jobAttributes._3
        if(alphaCpu > 0.0 || alphaMem > 0.0) {
          val lastTuple = allPastTuples.maxBy(tuple => tuple._1)
          val prob = getGammaDistributionCummulativeProbability( Math.min(alphaCpu,alphaMem), jobAttributes._1, Math.max(jobAttributes._1+cellState.powerOnTime, cellState.simulator.currentTime - lastTuple._1))
          gammaProbability = prob
        }
      }
    }while(jobAttributes._1 > 0.0 && jobAttributes._2 > 0.0 && jobAttributes._3 > 0.0 && jobAttributes._4 > 0.0 && jobAttributes._5 > 0.0 && jobAttributes._6 > 0.0 && gammaProbability > threshold && numMachinesPowerOn < cellState.numberOfMachinesOff)
    numMachinesPowerOn
  }

}
