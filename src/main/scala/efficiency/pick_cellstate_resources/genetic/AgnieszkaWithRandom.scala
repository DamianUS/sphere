package efficiency.pick_cellstate_resources.genetic

import ClusterSchedulingSimulation._
import efficiency.pick_cellstate_resources.CellStateResourcesPicker

import scala.collection.mutable.{IndexedSeq, ListBuffer}
import scala.util.Random
import scala.util.control.Breaks

/**
 * Created by dfernandez on 11/1/16.
 */
// This picker doesn't take into account yet shutted down machines nor capacity security margins nor performance
object AgnieszkaWithRandom extends CellStateResourcesPicker{
  val randomNumberGenerator = new util.Random(Seed())

  override def schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator): ListBuffer[ClaimDelta] = {
    //For batch jobs, this picker tries to minimize the makespan. Otherwise, random.
    if(job.workloadName == "Batch"){
      //Schedule: Machine ID -> [Task ID1, Task ID2,...]
      val schedule = collection.mutable.HashMap.empty[Int, collection.mutable.ListBuffer[Int]]
      val claimDeltas = collection.mutable.ListBuffer[ClaimDelta]()
      var candidatePool = scheduler.candidatePoolCache.getOrElseUpdate(cellState.numberOfMachinesOn, Array.range(0, cellState.numMachines))
      var numRemainingTasks = job.unscheduledTasks
      var remainingCandidates = math.max(0, cellState.numberOfMachinesOn - scheduler.numMachinesBlackList).toInt
      var numTries =0
      val makespanLog = scala.collection.mutable.ListBuffer.empty[Double]
      //First approach: Initialization: Iterate over the tasks and choose any machine randomly and then we only apply the crossing thing between ALL machines in cluster.

      //Initialization
      //var chosenMachines = scala.collection.mutable.ListBuffer.empty[Int]
      var initialMakespan = 0.0
      for(taskID <- 0 until job.numTasks){
        var scheduled = false
        while (!scheduled){
          val rnd = new scala.util.Random
          val range = 0 until cellState.numMachines
          val mID = (range(rnd.nextInt(range length)))
          val machineOcurrences = schedule.getOrElse(mID, scala.collection.mutable.ListBuffer.empty[Int]).size
          if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences+1) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences+1) * job.memPerTask + 0.0001) {
            assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : "+name)
            val tasksScheduled = schedule.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
            tasksScheduled += taskID
            if(cellState.machinesPerformance(mID) * job.taskDuration * job.tasksPerformance(taskID) > initialMakespan){
              initialMakespan = cellState.machinesPerformance(mID) * job.taskDuration * job.tasksPerformance(taskID)
            }
            scheduled = true
          }
          else{
            numTries+=1
          }
        }
      }

      makespanLog += initialMakespan
      //After this, we should have an array of machines that will host our tasks. We will then cross the worst machine with a random one

      for(epoch <- 0 until 2000){
        var worstMakespan = 0.0
        var worstParent = -1
        var bestMakespan = Double.MaxValue
        var bestParent = -1

        for ((machineID,tasksMachine) <- schedule){
          for(taskID <- tasksMachine){
            if(cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) > worstMakespan){
              worstMakespan = cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
              worstParent = machineID
            }
            if(cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) < bestMakespan){
              bestMakespan = cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
              bestParent = machineID
            }
          }
        }
        //Now we have the worst and the best parent, we should exchange it for another.
        assert(schedule.contains(bestParent), "Best parent is not present")
        assert(schedule.contains(worstParent), "Worst parent is not present")
        assert(cellState.isMachineOn(bestParent) && cellState.availableCpusPerMachine(bestParent) >= schedule.getOrElse(bestParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(bestParent) >= schedule.getOrElse(bestParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.memPerTask + 0.0001, "Not enough for bestParent before crossing")
        assert(cellState.isMachineOn(worstParent) && cellState.availableCpusPerMachine(worstParent) >= schedule.getOrElse(worstParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(worstParent) >= schedule.getOrElse(worstParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.memPerTask + 0.0001, "Not enough for worstParent before crossing")

        //Let's exchange a random task in worst parent for a random task in the best parent
        val bestTasks = schedule.get(bestParent).get
        val worstTasks = schedule.get(worstParent).get
        assert(bestTasks.size > 0, "Best tasks empty")
        assert(worstTasks.size > 0, "Worst tasks empty")
        val randomBestTask = bestTasks(Random.nextInt(bestTasks.size))
        val randomWorstTask = worstTasks(Random.nextInt(worstTasks.size))
        schedule.get(bestParent).get -= randomBestTask
        schedule.get(bestParent).get += randomWorstTask
        schedule.get(worstParent).get -= randomWorstTask
        schedule.get(worstParent).get += randomBestTask
        assert(cellState.isMachineOn(bestParent) && cellState.availableCpusPerMachine(bestParent) >= schedule.getOrElse(bestParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(bestParent) >= schedule.getOrElse(bestParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.memPerTask + 0.0001, "Not enough for bestParent after crossing")
        assert(cellState.isMachineOn(worstParent) && cellState.availableCpusPerMachine(worstParent) >= schedule.getOrElse(worstParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(worstParent) >= schedule.getOrElse(worstParent, scala.collection.mutable.ListBuffer.empty[Int]).size * job.memPerTask + 0.0001, "Not enough for worstParent after crossing")


        //Applying a random change to the new worst parent

        var worstMakespanRandom = 0.0
        var worstParentRandom = -1

        for ((machineID,tasksMachine) <- schedule){
          for(taskID <- tasksMachine){
            if(cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) > worstMakespanRandom){
              worstMakespanRandom = cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
              worstParentRandom = machineID
            }
          }
        }

        //Now we have the worst parent, we should exchange it for another.
        var scheduled = false
        while (!scheduled){
          val rnd = new scala.util.Random
          val range = 0 until cellState.numMachines
          val mID = (range(rnd.nextInt(range length)))
          val machineOcurrences = schedule.getOrElse(mID, scala.collection.mutable.ListBuffer.empty[Int]).size
          val worstMachineOcurrences = schedule.getOrElse(worstParentRandom, scala.collection.mutable.ListBuffer.empty[Int]).size
          if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences+worstMachineOcurrences) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences+worstMachineOcurrences) * job.memPerTask + 0.0001) {
            assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : "+name)
            val tasksScheduled = schedule.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
            tasksScheduled ++= schedule.getOrElse(worstParentRandom, scala.collection.mutable.ListBuffer.empty[Int])
            schedule.remove(worstParentRandom)
            scheduled = true
          }
          else{
            numTries+=1
          }
        }



        //Only for testing purposes
        var checkMakespan = 0.0

        for ((machineID,tasksMachine) <- schedule){
          for(taskID <- tasksMachine){
            if(cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) > checkMakespan){
              checkMakespan = cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
            }
          }
        }
        makespanLog += checkMakespan
        //End of testing purposes
      }
      for ((machineID,tasksMachine) <- schedule){
        for(taskID <- tasksMachine) {
          assert(machineID >= 0 && machineID < cellState.machineSeqNums.length, "Machine ID not valid")
          assert(taskID >= 0 && taskID < job.numTasks, "Task ID not valid")
          val claimDelta = new ClaimDelta(scheduler,
            machineID,
            cellState.machineSeqNums(machineID),
            if (cellState.machinesHeterogeneous && job.workloadName == "Batch") cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) else job.taskDuration,
            job.cpusPerTask,
            job.memPerTask,
            job = job)
          claimDelta.apply(cellState = cellState, locked = false)
          claimDeltas += claimDelta
        }
      }
      //At the end of the epochs, we return de applied scheduling
      claimDeltas
    }
    else{
      super.schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator)
    }
  }

  override def pickResource(cellState: CellState, job: Job, candidatePool: IndexedSeq[Int], remainingCandidates: Int) = {
    //YOU WILL CALL THIS FOR JOBS THAT ARE NOT BATCH.
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
  override val name: String = "agnieszka-picker"

}
