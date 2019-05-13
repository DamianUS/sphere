package efficiency.pick_cellstate_resources.genetic

import ClusterSchedulingSimulation._
import efficiency.pick_cellstate_resources.CellStateResourcesPicker

import scala.collection.mutable.{HashMap, IndexedSeq, ListBuffer}
import scala.util.Random
import scala.util.control.Breaks

/**
  * Created by dfernandez on 11/1/16.
  */
// This picker doesn't take into account yet shutted down machines nor capacity security margins nor performance
object AgnieszkaEnergySecurityWithRandom extends CellStateResourcesPicker{
  val randomNumberGenerator = new util.Random(Seed())
  val spreadMargin = 0.05
  val marginPerc = 0.01

  override def schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator): ListBuffer[ClaimDelta] = {
    //For batch jobs, this picker tries to minimize the makespan. Otherwise, random.
    if(job.workloadName == "Batch"){
      //println("Entra en schedule")
      //Schedule: Machine ID -> [Task ID1, Task ID2,...]
      val schedule = collection.mutable.HashMap.empty[Int, collection.mutable.ListBuffer[Int]]
      val claimDeltas = collection.mutable.ListBuffer[ClaimDelta]()
      var candidatePool = scheduler.candidatePoolCache.getOrElseUpdate(cellState.numberOfMachinesOn, Array.range(0, cellState.numMachines))
      var numRemainingTasks = job.unscheduledTasks
      var remainingCandidates = math.max(0, cellState.numberOfMachinesOn - scheduler.numMachinesBlackList).toInt
      var numTries =0
      val maxTries = 50
      val energyLog = scala.collection.mutable.ListBuffer.empty[Double]
      //First approach: Initialization: Iterate over the tasks and choose any machine randomly and then we only apply the crossing thing between ALL machines in cluster.
      var stop = false
      //Initialization
      //var chosenMachines = scala.collection.mutable.ListBuffer.empty[Int]
      var initialEnergy = 0.0
      val loop = new Breaks;
      loop.breakable {
        for (taskID <- 0 until job.numTasks) {
          var scheduled = false
          numTries = 0
          while (!stop && !scheduled && numTries < maxTries) {
            val secMachines = cellState.machinesSecurityMap.filterKeys(_ >= job.security).values
            val availableMachines = ListBuffer.empty[Int]
            for ( array <- secMachines){
              availableMachines ++= array
            }
            val onAvailableMachines = availableMachines.filter(cellState.isMachineOn(_)).filter( x => cellState.availableCpusPerMachine(x) >= (schedule.getOrElse(x, scala.collection.mutable.ListBuffer.empty[Int]).size +1) * job.cpusPerTask + 0.0001).filter( x => cellState.availableMemPerMachine(x) >= (schedule.getOrElse(x, scala.collection.mutable.ListBuffer.empty[Int]).size +1) * job.memPerTask + 0.0001)
            if(onAvailableMachines.length > 0){
              val range = 0 until onAvailableMachines.length
              val index = (range(randomNumberGenerator.nextInt(range length)))
              val mID = onAvailableMachines(index)
              val machineOcurrences = schedule.getOrElse(mID, scala.collection.mutable.ListBuffer.empty[Int]).size
              assert(cellState.machinesSecurity(mID) >= job.security, "Machines security not correct")
              if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences + 1) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences + 1) * job.memPerTask + 0.0001) {
                assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : " + name)
                val tasksScheduled = schedule.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
                tasksScheduled += taskID
                scheduled = true
              }
              else {
                numTries += 1
                if (numTries >= maxTries-1) {
                  stop = true;
                }
              }
            }
            else{
              loop.break;
            }
          }
        }
      }
      initialEnergy = getEnergyFromScheduleLogging(schedule,cellState,job,simulator)
      if(schedule.size > 0) {
        assert(schedule.size > 0, "Empty schedule")
        //if(!stop){
        energyLog += initialEnergy
        //After this, we should have an array of machines that will host our tasks. We will then cross the worst machine with a random one
        for (epoch <- 0 until 40) {
          var worstEnergy = 0.0
          var worstParent = -1
          var bestEnergy = Double.MaxValue
          var bestParent = -1

          for ((machineID, tasksMachine) <- schedule) {
            var energy = getEnergyFromMachine(schedule,cellState,job,machineID,simulator)
            if(energy > worstEnergy){
              worstEnergy = energy
              worstParent = machineID
            }
            if(energy < bestEnergy){
              bestEnergy = energy
              bestParent = machineID
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

          var worstEnergyRandom = 0.0
          var worstParentRandom = -1

          for ((machineID, tasksMachine) <- schedule) {
            val energy = getEnergyFromMachine(schedule,cellState,job,machineID,simulator)
            if (energy > worstEnergyRandom) {
              worstEnergyRandom = energy
              worstParentRandom = machineID
            }
          }
          numTries = 0
          //Now we have the worst parent, we should try toexchange it for another.
          var scheduled = false
          var shouldBeStopped = false
          while (!shouldBeStopped && !scheduled && numTries < maxTries) {
            val secMachines = cellState.machinesSecurityMap.filterKeys(_ >= job.security).values
            val availableMachines = ListBuffer.empty[Int]
            for (array <- secMachines) {
              availableMachines ++= array
            }
            val onAvailableMachines = availableMachines.filter(cellState.isMachineOn(_)).filter(x => cellState.availableCpusPerMachine(x) >= (schedule.getOrElse(x, scala.collection.mutable.ListBuffer.empty[Int]).size + 1) * job.cpusPerTask + 0.0001).filter(x => cellState.availableMemPerMachine(x) >= (schedule.getOrElse(x, scala.collection.mutable.ListBuffer.empty[Int]).size + 1) * job.memPerTask + 0.0001)
            if (onAvailableMachines.length > 0) {
              val range = 0 until onAvailableMachines.length
              val index = (range(randomNumberGenerator.nextInt(range length)))
              val mID = onAvailableMachines(index)
              val machineOcurrences = schedule.getOrElse(mID, scala.collection.mutable.ListBuffer.empty[Int]).size
              val worstMachineOcurrences = schedule.getOrElse(worstParentRandom, scala.collection.mutable.ListBuffer.empty[Int]).size
              assert(cellState.machinesSecurity(mID) >= job.security, "Machines security not correct")
              if (mID != worstParentRandom && cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences + worstMachineOcurrences) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences + worstMachineOcurrences) * job.memPerTask + 0.0001) {
                assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : " + name)
                val tasksScheduled = schedule.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
                tasksScheduled ++= schedule.getOrElse(worstParentRandom, scala.collection.mutable.ListBuffer.empty[Int])
                schedule.remove(worstParentRandom)
                scheduled = true
              }
              else {
                numTries += 1
                if (numTries >= maxTries - 1) {
                  shouldBeStopped = true;
                }
              }
            }
            else {
              shouldBeStopped = true
            }
          }
          energyLog += getEnergyFromScheduleLogging(schedule,cellState,job,simulator)
          //End of testing purposes
        }
      }
      //if(!stop){
      /*var securityTime = 0.0
      if(job.security == 1)
        securityTime = simulator.securityLevel1Time
      if(job.security == 2)
        securityTime = simulator.securityLevel2Time
      else if(job.security == 3)
        securityTime = simulator.securityLevel3Time*/
      for ((machineID,tasksMachine) <- schedule){
        var securityTime = 0.0
        if(cellState.machinesSecurity(machineID) == 1)
          securityTime = simulator.securityLevel1Time
        if(cellState.machinesSecurity(machineID) == 2)
          securityTime = simulator.securityLevel2Time
        else if(cellState.machinesSecurity(machineID) == 3)
          securityTime = simulator.securityLevel3Time
        for(taskID <- tasksMachine) {
          assert(machineID >= 0 && machineID < cellState.machineSeqNums.length, "Machine ID not valid")
          assert(taskID >= 0 && taskID < job.numTasks, "Task ID not valid")
          val claimDelta = new ClaimDelta(scheduler,
            machineID,
            cellState.machineSeqNums(machineID),
            if (cellState.machinesHeterogeneous && job.workloadName == "Batch") ((job.taskDuration * cellState.machinesPerformance(machineID)) +  (securityTime * cellState.machinesPerformance(machineID))) else job.taskDuration + securityTime,
            job.cpusPerTask,
            job.memPerTask,
            job = job)
          claimDelta.apply(cellState = cellState, locked = false)
          claimDeltas += claimDelta
        }
      }
      //}
      //}
      //At the end of the epochs, we return de applied scheduling
      job.makespanLogArray += energyLog
      //println("Sale de schedule")
      claimDeltas
    }
    else{
      super.schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator)
    }
  }

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
  override val name: String = "agnieszka-energy-security-picker-random"


  def getEnergyFromSchedule(schedule: HashMap[Int, ListBuffer[Int]], cellState: CellState, job: Job, simulator: ClusterSimulator): Double ={
    var energy = 0.0
    /*var securityTime = 0.0
    if(job.security == 1)
      securityTime = simulator.securityLevel1Time
    if(job.security == 2)
      securityTime = simulator.securityLevel2Time
    else if(job.security == 3)
      securityTime = simulator.securityLevel3Time*/
    for ((machineID,tasksMachine) <- schedule) {
      var securityTime = 0.0
      if(cellState.machinesSecurity(machineID) == 1)
        securityTime = simulator.securityLevel1Time
      if(cellState.machinesSecurity(machineID) == 2)
        securityTime = simulator.securityLevel2Time
      else if(cellState.machinesSecurity(machineID) == 3)
        securityTime = simulator.securityLevel3Time
      for (taskID <- tasksMachine) {
        var taskTime = (cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID))+(securityTime * cellState.machinesPerformance(machineID))
        energy += job.cpusPerTask * cellState.simulator.powerPerCpuOn * cellState.machinesEnergy(machineID) * taskTime
      }
    }
    return energy * (1+(schedule.keySet.size/10)) / 3600000
  }

  def getEnergyFromMachine(schedule: HashMap[Int, ListBuffer[Int]], cellState: CellState, job: Job, machineID : Int, simulator: ClusterSimulator): Double ={
    var energy = 0.0
    /*var securityTime = 0.0
    if(job.security == 1)
      securityTime = simulator.securityLevel1Time
    if(job.security == 2)
      securityTime = simulator.securityLevel2Time
    else if(job.security == 3)
      securityTime = simulator.securityLevel3Time*/
    var securityTime = 0.0
    if(cellState.machinesSecurity(machineID) == 1)
      securityTime = simulator.securityLevel1Time
    if(cellState.machinesSecurity(machineID) == 2)
      securityTime = simulator.securityLevel2Time
    else if(cellState.machinesSecurity(machineID) == 3)
      securityTime = simulator.securityLevel3Time
    for (taskID <- schedule.get(machineID).get) {
      var taskTime = (cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID))+(securityTime * cellState.machinesPerformance(machineID))
      energy += job.cpusPerTask * simulator.powerPerCpuOn * cellState.machinesEnergy(machineID) * taskTime
    }
    return energy * (1+(schedule.keySet.size/10)) / 3600000
  }

  def getEnergyFromScheduleLogging(schedule: HashMap[Int, ListBuffer[Int]], cellState: CellState, job: Job, simulator: ClusterSimulator): Double ={
    var energy = 0.0
    /*var securityTime = 0.0
    if(job.security == 1)
      securityTime = simulator.securityLevel1Time
    if(job.security == 2)
      securityTime = simulator.securityLevel2Time
    else if(job.security == 3)
      securityTime = simulator.securityLevel3Time*/
    for ((machineID,tasksMachine) <- schedule) {
      var securityTime = 0.0
      if(cellState.machinesSecurity(machineID) == 1)
        securityTime = simulator.securityLevel1Time
      if(cellState.machinesSecurity(machineID) == 2)
        securityTime = simulator.securityLevel2Time
      else if(cellState.machinesSecurity(machineID) == 3)
        securityTime = simulator.securityLevel3Time
      for (taskID <- tasksMachine) {
        var taskTime = (cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID))+(securityTime * cellState.machinesPerformance(machineID))
        energy += job.cpusPerTask * simulator.powerPerCpuOn * cellState.machinesEnergy(machineID) * taskTime
      }
    }
    return energy / 3600000
  }
}
