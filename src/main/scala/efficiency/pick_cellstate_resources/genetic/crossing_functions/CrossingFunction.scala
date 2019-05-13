package efficiency.pick_cellstate_resources.genetic.crossing_functions

import ClusterSchedulingSimulation._
import efficiency.pick_cellstate_resources.genetic.fitness_functions.FitnessFunction

import scala.collection.mutable.{ListBuffer, HashMap}

/**
 * Created by dfernandez on 11/1/16.
 */
trait CrossingFunction {
  /**
    * Selects the required number of candidates from the population with
    * the probability of selecting any particular candidate being proportional
    * to that candidate's fitness score.  Selection is with replacement (the same
    * candidate may be selected multiple times).
    * @param chromosome1 The first chromosome to cross.
    * @param chromosome2 The second chromosome to cross.
    * @return The crossed chromosomes.
    */
  def cross(chromosome1 : ListBuffer[Int], chromosome2 : ListBuffer[Int]): (ListBuffer[Int],ListBuffer[Int])

  def newCross(chromosome1 : HashMap[Int, ListBuffer[Int]], chromosome2 : HashMap[Int, ListBuffer[Int]]): (HashMap[Int, ListBuffer[Int]],HashMap[Int, ListBuffer[Int]])

  val name : String
}
