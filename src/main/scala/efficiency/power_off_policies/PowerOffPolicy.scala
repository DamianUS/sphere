package efficiency.power_off_policies

import ClusterSchedulingSimulation.{CellState, ClusterSimulator}
import efficiency.power_off_policies.action.PowerOffAction
import efficiency.power_off_policies.decision.PowerOffDecision

/**
  * Created by dfernandez on 13/1/16.
  */
trait PowerOffPolicy {
  var powerOffAction : PowerOffAction
  var powerOffDecisionPolicy : PowerOffDecision
  val shouldPerformGlobalCheck : Boolean = false
  val globalCheckPeriod : Double = 300.0
  var initiated : Boolean = false
  def powerOff(cellState: CellState, machineID: Int)= {
    if(!initiated && shouldPerformGlobalCheck){
      assert(globalCheckPeriod > 0.0, "Trying to perform global shut downs with incorrect period")
      initiated = true
      globalCheck(cellState)
    }
    if(powerOffDecisionPolicy.shouldPowerOff(cellState, machineID))
       powerOffAction.powerOff(cellState, machineID)
  }
  val name : String

  /*def globalCheck(cellState: CellState) :Unit ={
    cellState.simulator.afterDelay(globalCheckPeriod){
      var a = 0
      for (a <- 0 to cellState.numMachines-1){
        if(cellState.isMachineOn(a))
          powerOff(cellState, a)
      }
      globalCheck(cellState)
    }
  }*/
  def globalCheck(cellState: CellState) :Unit ={
      var a = 0
      for (a <- 0 to cellState.numMachines-1){
        if(cellState.isMachineOn(a))
          powerOff(cellState, a)
      }
  }
}
