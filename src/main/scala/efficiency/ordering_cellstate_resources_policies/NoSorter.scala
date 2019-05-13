package efficiency.ordering_cellstate_resources_policies

import ClusterSchedulingSimulation.CellState

/**
 * Created by dfernandez on 8/1/16.
 */
// Initially defined as an object for simplicity of usage
// TODO: Become a class if necessary
object NoSorter extends CellStateResourcesSorter{
  override def order(cellstate: CellState): Unit = {}

  override val name: String = "no-sorter"

  override def calculateLoad(machineID: Int, cellState: CellState): Double = {0.0}
}
