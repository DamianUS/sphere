package efficiency.pick_cellstate_resources.genetic.mutating_functions

import ClusterSchedulingSimulation.{CellState, Job, Seed}

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object Random extends MutatingFunction{

  val randomNumberGenerator = new util.Random(Seed())

  override def mutate(chromosome: ListBuffer[Int], mutationProbability: Double, cellState : CellState, job : Job): ListBuffer[Int] = {
    val mutatedChromosome = chromosome
    for( gene <- chromosome){
      if(randomNumberGenerator.nextDouble() < mutationProbability){
        val currentGeneValue = chromosome(gene)
        val newGeneValue= if(currentGeneValue==1) 0 else 1
        var mutID = -1
        while(mutID == -1){
          val range = 0 until cellState.numMachines
          val mutationMachineID = (range(randomNumberGenerator.nextInt(range length)))
          if (chromosome(mutationMachineID)==newGeneValue && cellState.availableCpusPerMachine(mutationMachineID) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mutationMachineID) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(mutationMachineID)) {
            mutID=mutationMachineID
            assert(cellState.isMachineOn(mutationMachineID), "Trying to pick a powered off machine with picker in mutation : "+name)
          }
        }
        chromosome(gene) = newGeneValue
        chromosome(mutID) = currentGeneValue
      }
    }
    mutatedChromosome
  }

  override def newMutate(chromosome: HashMap[Int, ListBuffer[Int]], mutationProbability: Double, cellState: CellState, job : Job): HashMap[Int, ListBuffer[Int]] = {
    val mutatedChromosome = chromosome
    for ((machineID,tasksMachine) <- mutatedChromosome){
      for(taskID <- tasksMachine){
        if(randomNumberGenerator.nextDouble() < mutationProbability){
          //Change task to another machine
          var scheduled = false
          while (!scheduled) {
            val range = 0 until cellState.numMachines
            val mID = (range(randomNumberGenerator.nextInt(range length)))
            val machineOcurrences = mutatedChromosome.getOrElse(mID, ListBuffer.empty[Int]).size
            if (cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (machineOcurrences + 1) * job.cpusPerTask + 0.0001 && cellState.availableMemPerMachine(mID) >= (machineOcurrences + 1) * job.memPerTask + 0.0001) {
              assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : " + name)
              val tasksScheduled = mutatedChromosome.getOrElseUpdate(mID, collection.mutable.ListBuffer.empty[Int])
              tasksScheduled += taskID
              val oldTasksScheduled = mutatedChromosome.get(machineID).get
              assert(oldTasksScheduled.size > 0, "Old tasks scheduled is empty")
              oldTasksScheduled -= taskID
              if (oldTasksScheduled.size == 0) mutatedChromosome.remove(machineID)
              scheduled = true
            }
          }
        }
      }
    }
    mutatedChromosome
  }

  override val name: String = "random"
}
