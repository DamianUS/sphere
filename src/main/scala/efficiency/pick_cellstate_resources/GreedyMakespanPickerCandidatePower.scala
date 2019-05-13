package efficiency.pick_cellstate_resources

import ClusterSchedulingSimulation.{CellState, Job, Seed}

import scala.collection.mutable.IndexedSeq
import scala.util.control.Breaks

/**
 * Created by dfernandez on 11/1/16.
 */
// This picker doesn't take into account yet shutted down machines nor capacity security margins nor performance
// TODO: Make a Quicksort-like strategy or pass a candidate index to iterate (e.g. the last successful candidate)
object GreedyMakespanPickerCandidatePower extends CellStateResourcesPicker{
  val randomNumberGenerator = new util.Random(Seed())

  override def pickResource(cellState: CellState, job: Job, candidatePool: IndexedSeq[Int], remainingCandidates: Int) = {
    var machineID = -1
    var numTries =0
    var remainingCandidatesVar= cellState.numMachines
    val loop = new Breaks;
    if(job.workloadName == "Batch"){
      loop.breakable {
        for(i <- 0 until cellState.numMachines){
          //FIXME: Putting a security margin of 0.01 because mesos is causing conflicts
          val mID = cellState.machinesPerformanceOrdered(i)
          if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mID) >= (job.memPerTask + 0.0001)) {
            machineID=mID
            assert(cellState.isMachineOn(machineID), "Trying to pick a powered off machine with picker : "+name)
            loop.break;
          }
          else{
            numTries+=1
            remainingCandidatesVar -=1 // This is irrelevant in this implementation, as derivable of numTries. I'll use it in quicksort-like implementations
          }

        }
      }
      new Tuple4(machineID, numTries, remainingCandidatesVar, candidatePool)
    }
    else{
      var machineID = -1
      var numTries = 0
      var remainingCandidatesVar = remainingCandidates
      val candidatePoolVar = candidatePool
      val loop = new Breaks;
      loop.breakable {
        while (machineID == -1 && remainingCandidatesVar > 0) {
          val candidateIndex = randomNumberGenerator.nextInt(remainingCandidatesVar)
          val currMachID = candidatePoolVar(candidateIndex)
          if (cellState.availableCpusPerMachine(currMachID) >= (job.cpusPerTask + 0.0001) &&
            cellState.availableMemPerMachine(currMachID) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(currMachID)) {
            machineID=currMachID
            assert(cellState.isMachineOn(machineID), "Trying to pick a powered off machine with picker : "+name)
            loop.break;
          }
          else {
            numTries += 1
            candidatePoolVar(candidateIndex) = candidatePoolVar(remainingCandidatesVar - 1)
            candidatePoolVar(remainingCandidatesVar - 1) = currMachID
            remainingCandidatesVar -= 1
          }
        }
      }
      new Tuple4(machineID, numTries, remainingCandidatesVar, candidatePoolVar)
    }
  }
  override val name: String = "greedy-makespan-picker-candidate"
}
