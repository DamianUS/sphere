package efficiency.power_off_policies.action

import ClusterSchedulingSimulation.CellState

/**
  * Created by dfernandez on 15/1/16.
  */
trait PowerOffAction {
   def powerOff(cellState: CellState, machineID: Int)
   val name : String
 }
