package efficiency.pick_cellstate_resources.genetic.mutating_functions

import ClusterSchedulingSimulation.{CellState, Job}

import scala.collection.mutable.{HashMap, ListBuffer}

/**
 * Created by dfernandez on 11/1/16.
 */
trait MutatingFunction {
  /**
    * Selects the required number of candidates from the population with
    * the probability of selecting any particular candidate being proportional
    * to that candidate's fitness score.  Selection is with replacement (the same
    * candidate may be selected multiple times).
    * @param chromosome The chromosome to mutate.
    * @return The mutated chromosomes.
    */
  def mutate(chromosome : ListBuffer[Int], mutationProbability : Double, cellState : CellState, job : Job): ListBuffer[Int]

  def newMutate(chromosome : HashMap[Int, ListBuffer[Int]], mutationProbability : Double, cellState : CellState, job : Job): HashMap[Int, ListBuffer[Int]]

  val name : String
}
