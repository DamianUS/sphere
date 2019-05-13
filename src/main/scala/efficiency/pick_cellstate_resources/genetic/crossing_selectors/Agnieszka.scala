package efficiency.pick_cellstate_resources.genetic.crossing_selectors

import ClusterSchedulingSimulation.{CellState, Job, Seed}
import efficiency.pick_cellstate_resources.genetic.fitness_functions.FitnessFunction

import scala.collection.Searching._
import scala.collection.mutable.{HashMap, ListBuffer}

/**
  * Created by dfernandez on 11/1/16.
  */
object Agnieszka extends CrossingSelector{

  override def select(chromosomes: ListBuffer[ListBuffer[Int]], naturalFitnessScores: Boolean, selectionSize: Int, fitnessFunction: FitnessFunction, job: Job, cellState: CellState): ListBuffer[ListBuffer[Int]] = {
    assert(chromosomes.size > 1, "Needed at least two chromosomes")
    assert(chromosomes.size >= selectionSize, "The chromosomes size must be equal or greater than the selection")
    assert(selectionSize == 2, "Agnieszka selector always works with the best and worst chromosome")
    var bestFitness = 0.0
    var worstFitness = 0.0
    var bestChromosome : ListBuffer[Int] = null
    var worstChromosome : ListBuffer[Int] = null
    for (i <- 0 until chromosomes.size){
      val fitness = fitnessFunction.evaluate(chromosomes(i),job,cellState)
      if(worstFitness == 0.0 || fitness > worstFitness){
        worstFitness = fitness
        worstChromosome = chromosomes(i)
      }
      else if(bestFitness == 0.0 || fitness < bestFitness){
        bestFitness = fitness
        bestChromosome = chromosomes(i)
      }
    }

    val selection = new scala.collection.mutable.ListBuffer[ListBuffer[Int]]
    selection += bestChromosome
    selection += worstChromosome
    selection
  }

  override def newSelect(population: ListBuffer[HashMap[Int, ListBuffer[Int]]], naturalFitnessScores: Boolean, selectionSize: Int, fitnessFunction: FitnessFunction, job: Job, cellState: CellState): ListBuffer[HashMap[Int, ListBuffer[Int]]] = {
    assert(population.size > 1, "Needed at least two chromosomes")
    assert(population.size >= selectionSize, "The chromosomes size must be equal or greater than the selection")
    assert(selectionSize == 2, "Agnieszka selector always works with the best and worst chromosome")
    var bestFitness = 0.0
    var worstFitness = 0.0
    var bestChromosome : HashMap[Int, ListBuffer[Int]] = null
    var worstChromosome : HashMap[Int, ListBuffer[Int]] = null
    for (i <- 0 until population.size){
      val fitness = fitnessFunction.newEvaluate(population(i),job,cellState)
      if(worstFitness == 0.0 || fitness > worstFitness){
        worstFitness = fitness
        worstChromosome = population(i)
      }
      else if(bestFitness == 0.0 || fitness < bestFitness){
        bestFitness = fitness
        bestChromosome = population(i)
      }
    }

    val selection = new ListBuffer[HashMap[Int, ListBuffer[Int]]]()
    selection += bestChromosome
    selection += worstChromosome
    selection

  }

  override val name: String = "Agnieszka"
}
