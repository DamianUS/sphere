package efficiency.pick_cellstate_resources.genetic.fitness_functions

import ClusterSchedulingSimulation.{CellState, Job}

import scala.collection.mutable.{HashMap, ListBuffer}


/**
 * Created by dfernandez on 11/1/16.
 */
object MakespanMedian extends FitnessFunction{

  override def evaluate(chromosome: ListBuffer[Int], job: Job, cellState: CellState): Double = {
    val makespans = collection.mutable.ListBuffer.empty[Double]
    if (job.workloadName == "Batch"){
      for(mID <- 0 until cellState.numMachines){
        if(chromosome(mID) == 1){
          makespans += job.taskDuration * cellState.machinesPerformance(mID)
        }
      }
    }
    else{
      makespans += job.taskDuration
    }
    makespans.sortWith(_ < _).drop(makespans.length/2).head
  }

  override def newEvaluate(chromosome: HashMap[Int, ListBuffer[Int]], job: Job, cellState: CellState): Double ={
    val makespans = collection.mutable.ListBuffer.empty[Double]
    if (job.workloadName == "Batch"){
      for ((machineID,tasksMachine) <- chromosome){
        for(taskID <- tasksMachine){
          makespans += cellState.machinesPerformance(machineID) * job.taskDuration * job.tasksPerformance(taskID)
        }
      }
    }
    else{
      makespans += job.taskDuration
    }
    makespans.sortWith(_ < _).drop(makespans.length/2).head
  }

  override val name: String = "makespan-median"
}
