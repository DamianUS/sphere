package efficiency.power_on_policies.action.probabilistic

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.DistributionUtils
import efficiency.power_on_policies.action.PowerOnAction

/**
  * Created by dfernandez on 15/1/16.
  */
class ExponentialPowerOnAction(threshold : Double, windowSize: Int, lostFactor : Double = 0.25) extends PowerOnAction with DistributionUtils{


  override val name: String = ("exponential-power-on-action-with-threshold:%f-and-window-size:%d").format(threshold,windowSize)

  override def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Int = {
    var numMachinesPowerOn = 0
    val allPastTuples = getPastTuples(cellState, windowSize)
    var dangerousPastTuples = allPastTuples.filter(pastTuple => (pastTuple._2.numTasks * pastTuple._2.cpusPerTask > cellState.availableCpus - (cellState.numberOfMachinesOn * cellState.cpusPerMachine * lostFactor)) || (pastTuple._2.numTasks * pastTuple._2.memPerTask > cellState.availableMem - (cellState.numberOfMachinesOn * cellState.memPerMachine * lostFactor)))
    var exponentialProbability = 1000000.0
    if (dangerousPastTuples.size > 0) {
      do {
        numMachinesPowerOn += 1
        dangerousPastTuples = allPastTuples.filter(pastTuple => (pastTuple._2.numTasks * pastTuple._2.cpusPerTask > cellState.availableCpus + (cellState.cpusPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn) * cellState.cpusPerMachine * lostFactor)) || (pastTuple._2.numTasks * pastTuple._2.memPerTask > cellState.availableMem + (cellState.memPerMachine * numMachinesPowerOn) - ((cellState.numberOfMachinesOn + numMachinesPowerOn) * cellState.memPerMachine * lostFactor)))
        //val jobAttributes = getJobAttributes(allPastTuples)
        var interArrival = 0.0
        if (dangerousPastTuples.size > 1) {
          interArrival = dangerousPastTuples(0)._1
        }
        else {
          interArrival = generateJobAtributes(dangerousPastTuples)._1
        }
        if (interArrival > 0.0) {
          val lastDangerousTuple = dangerousPastTuples.maxBy(tuple => tuple._1)
          val interArrivalDecision = generateJobAtributes(allPastTuples)._1
          val prob = getExponentialDistributionCummulativeProbability(interArrival, Math.max(interArrivalDecision+cellState.powerOnTime, cellState.simulator.currentTime - lastDangerousTuple._1))
          exponentialProbability = prob
        }
      }while(exponentialProbability > threshold && numMachinesPowerOn < cellState.numberOfMachinesOff)
    }
    numMachinesPowerOn
  }
}
