package efficiency.pick_cellstate_resources

import ClusterSchedulingSimulation.{CellState, Job}

import scala.collection.mutable.IndexedSeq
import scala.util.control.Breaks

/**
  * Created by dfernandez on 11/1/16.
  */

//Spread margin is the "bucket size" for the random machine that will be elected in percentage of numMachines in cluster
//Margin Perc will be used as a "safety margin"
class SpreadMarginReversePickerCandidatePower(spreadMargin : Double = 0.1, marginPerc : Double = 0.1) extends CellStateResourcesPicker{
  override def pickResource(cellState: CellState, job: Job, candidatePool: IndexedSeq[Int], remainingCandidates: Int) = {
    assert(marginPerc >= 0.00 && marginPerc <= 1.00, "Margin in MarginReversePickerCandidatePower must be between 0 and 1")
    var machineID = -1
    var numTries =0
    var remainingCandidatesVar= cellState.numMachines
    val loop = new Breaks;
    loop.breakable {
      for(i <- remainingCandidatesVar-1 to 0 by -1){
        //FIXME: Putting a security margin of 0.01 because mesos is causing conflicts
        val mID = cellState.machinesLoad(i)
        val availableCpus = cellState.availableCpusPerMachine(mID)
        val availableMem = cellState.availableMemPerMachine(mID)
        if (cellState.isMachineOn(mID) && availableCpus >= (marginPerc * cellState.cpusPerMachine + job.cpusPerTask + 0.0001) && availableMem >= (marginPerc * cellState.memPerMachine + job.memPerTask + 0.0001)) {
          val machID=pickRandomResource(i, cellState, job)
          if(machID > -1){
            assert(cellState.isMachineOn(machID), "Trying to pick a powered off machine with picker : "+name)
            machineID = machID
            loop.break;
          }
          else{
            numTries+=1
            remainingCandidatesVar -=1
          }
        }
        else{
          numTries+=1
          remainingCandidatesVar -=1 // This is irrelevant in this implementation, as derivable of numTries. I'll use it in quicksort-like implementations
        }

      }
    }
    if(machineID == -1){
      assert(remainingCandidatesVar == 0, ("No ha encontrado un candidato en %s y sin embargo dice que hay %d candidatos aún disponibles").format(name, remainingCandidatesVar))
    }
    new Tuple4(machineID, numTries, remainingCandidatesVar, candidatePool)
  }

  def pickRandomResource (pivot : Int, cellState : CellState, job : Job): Int ={
    val range = Math.max(0 , (pivot - (cellState.numMachines * spreadMargin).toInt)) to pivot
    val loop = new Breaks;
    var machineID = -1
    loop.breakable {
      for (i <- 0 to 5 by 1) {
        val pseudoRandomMachine = range(scala.util.Random.nextInt(range length))
        val mID = cellState.machinesLoad(pseudoRandomMachine)
        if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (marginPerc * cellState.cpusPerMachine + job.cpusPerTask + 0.01) && cellState.availableMemPerMachine(mID) >= (marginPerc * cellState.memPerMachine + job.memPerTask + 0.01)) {
          machineID=mID
          assert(cellState.isMachineOn(machineID), "Trying to pick a powered off machine with picker : "+name)
          loop.break;
        }
      }
    }
    machineID
  }
  override val name: String = "spread-margin-reverse-power-picker-candidate-with-spreadMargin-"+spreadMargin+"-and-freeMargin-"+marginPerc
}
