package efficiency.ordering_cellstate_resources_policies

import ClusterSchedulingSimulation.CellState

/**
 * Created by dfernandez on 8/1/16.
 */
// Initially defined as an object for simplicity of usage
// TODO: Become a class if necessary
object PowerStateLoadSorter extends CellStateResourcesSorter{

  override val name: String = "power-load-sorter"

  override def calculateLoad(machineID: Int, cellState: CellState): Double = {
    var loadFactor = 2.0
    if(cellState.isMachineOn(machineID)) {
      val cpuLoad = cellState.allocatedCpusPerMachine(machineID) / cellState.cpusPerMachine
      val memLoad = cellState.allocatedMemPerMachine(machineID) / cellState.memPerMachine
      loadFactor = Math.max(cpuLoad, memLoad)
    }
    loadFactor
  }
}
