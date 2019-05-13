package efficiency.power_off_policies.decision

import ClusterSchedulingSimulation.CellState

/**
  * Created by dfernandez on 15/1/16.
  */
trait PowerOffDecision {
   def shouldPowerOff(cellState: CellState, machineID: Int) : Boolean
   val name : String
 }
