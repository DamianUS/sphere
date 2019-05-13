package efficiency.ordering_cellstate_resources_policies

import ClusterSchedulingSimulation.CellState

/**
 * Created by dfernandez on 8/1/16.
 */
trait CellStateResourcesSorter {
  def order(cellstate: CellState) : Unit ={
    cellstate.machinesLoad = cellstate.machinesLoadFactor.toSeq.sortBy(_._2).map(_._1).toArray
  }
  def orderResources(cellState: CellState) : Unit ={
    if(!cellState.machinesLoadOrdered){
      order(cellState)
      cellState.machinesLoadOrdered = true
    }
  }
  def updateLoadFactor(machineID : Int, cellState: CellState) : Unit ={
    cellState.machinesLoadFactor.update(machineID, calculateLoad(machineID, cellState))
    cellState.machinesLoadOrdered = false
  }
  def calculateLoad(machineID : Int, cellState: CellState) : Double
  val name: String
}
