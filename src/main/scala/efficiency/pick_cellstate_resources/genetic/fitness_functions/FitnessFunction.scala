package efficiency.pick_cellstate_resources.genetic.fitness_functions

import ClusterSchedulingSimulation.{CellState, Job}

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

/**
 * Created by dfernandez on 11/1/16.
 */
trait FitnessFunction {
  /**
    * Selects the required number of candidates from the population with
    * the probability of selecting any particular candidate being proportional
    * to that candidate's fitness score.  Selection is with replacement (the same
    * candidate may be selected multiple times).
    * @param chromosome The solution to evaluate.
    * @return The grade. THE LOWER, THE BETTER.
    */
  def evaluate(chromosome : ListBuffer[Int], job : Job, cellState : CellState): Double

  def newEvaluate(chromosome : HashMap[Int, ListBuffer[Int]], job : Job, cellState : CellState): Double


  val name : String
}
