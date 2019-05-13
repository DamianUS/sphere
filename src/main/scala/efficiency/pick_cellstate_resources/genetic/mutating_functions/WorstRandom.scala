package efficiency.pick_cellstate_resources.genetic.mutating_functions

import ClusterSchedulingSimulation.{CellState, Job, Seed}

import scala.collection.mutable.{HashMap, ListBuffer}

object WorstRandom extends MutatingFunction{

  val randomNumberGenerator = new util.Random(Seed())

  override def mutate(chromosome: ListBuffer[Int], mutationProbability: Double, cellState : CellState, job : Job): ListBuffer[Int] = {
    val mutatedChromosome = chromosome
    if(randomNumberGenerator.nextDouble() < mutationProbability){
      var worstGene = -1
      var worstMakespan = 0.0
      for( worstMID <- 0 until cellState.numMachines){
        if(mutatedChromosome(worstMID)==1){
          if (job.taskDuration * cellState.machinesPerformance(worstMID) > worstMakespan){
            worstMakespan = job.taskDuration * cellState.machinesPerformance(worstMID)
            worstGene = worstMID
          }
        }
      }
      val currentGeneValue = mutatedChromosome(worstGene)
      val newGeneValue= if(currentGeneValue==1) 0 else 1
      var mutID = -1
      while(mutID == -1){
        val range = 0 until cellState.numMachines
        val mutationMachineID = (range(randomNumberGenerator.nextInt(range length)))
        if (mutatedChromosome(mutationMachineID)==newGeneValue && cellState.availableCpusPerMachine(mutationMachineID) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mutationMachineID) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(mutationMachineID)) {
          mutID=mutationMachineID
          assert(cellState.isMachineOn(mutationMachineID), "Trying to pick a powered off machine with picker in mutation : "+name)
        }
      }
      mutatedChromosome(worstGene) = newGeneValue
      mutatedChromosome(mutID) = currentGeneValue
    }
    mutatedChromosome
  }

  override def newMutate(chromosome: HashMap[Int, ListBuffer[Int]], mutationProbability: Double, cellState: CellState, job : Job): HashMap[Int, ListBuffer[Int]] = {
    assert(mutationProbability >= 0.0 && mutationProbability <= 1.0, "Mutation probability not valid")
    //val tasks = chromosome.values.map( x => x.size).sum
    //println("Empiezo "+name)

    var tasks = 0
    for ((machineID,tasksMachine) <- chromosome){
      tasks += tasksMachine.size
    }
    val mutatedChromosome = chromosome
    if(randomNumberGenerator.nextDouble() < mutationProbability){
      var worstMakespan = 0.0
      var worstParent = -1
      for((machineID,tasksMachine) <- mutatedChromosome) {
        for (taskID <- tasksMachine) {
          if (cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID) > worstMakespan) {
            worstMakespan = cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
            worstParent = machineID
          }
        }
      }
      assert(worstParent > -1, "Worst parent not found")
      //Change task to another machine
      var scheduled = false
      while (!scheduled) {
        val range = 0 until cellState.numMachines
        val mID = (range(randomNumberGenerator.nextInt(range length)))
        val machineOcurrences = mutatedChromosome.getOrElse(mID, ListBuffer.empty[Int]).size
        val worstMachineOcurrences = mutatedChromosome.get(worstParent).get.size
        if (mID != worstParent && cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences+worstMachineOcurrences) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences+worstMachineOcurrences) * job.memPerTask + 0.0001) {
          assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : "+name)
          val hola = 1
          val tasksScheduled = mutatedChromosome.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
          tasksScheduled ++= mutatedChromosome.get(worstParent).get
          val hola2 = 2
          mutatedChromosome.remove(worstParent)
          scheduled = true
        }
      }
    }
    //val newTasks= mutatedChromosome.values.map( x => x.size).sum
    var newTasks = 0
    for ((mID,newTasksMachine) <- mutatedChromosome){
      newTasks += newTasksMachine.size
    }
    assert(newTasks == tasks, "Tasks number changed in mutation in "+name+". Former was: "+tasks+" and chromosome: "+chromosome.toString+" and new is: "+newTasks+" in: "+name+" and chromosome: "+mutatedChromosome.toString())
    //println("Llego al final de "+name)
    mutatedChromosome
  }

  override val name: String = "worst-machine-to-random-machine"
}
