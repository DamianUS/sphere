package efficiency.ordering_cellstate_resources_policies

import ClusterSchedulingSimulation.CellState

/**
 * Created by dfernandez on 8/1/16.
 */
// Initially defined as an object for simplicity of usage
// TODO: Become a class if necessary
object BasicLoadSorter extends CellStateResourcesSorter{
  override val name: String = "basic-load-sorter"

  override def calculateLoad(machineID: Int, cellState: CellState): Double = {
    val cpuLoad = cellState.allocatedCpusPerMachine(machineID) / cellState.cpusPerMachine
    val memLoad = cellState.allocatedMemPerMachine(machineID) / cellState.memPerMachine
    Math.max(cpuLoad, memLoad)
  }
}
