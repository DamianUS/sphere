package efficiency.power_off_policies.decision

import ClusterSchedulingSimulation.CellState

/**
  * Created by dfernandez on 15/1/16.
  */
class CombinedPowerOffDecision(pOffDecisions: Seq[PowerOffDecision], op: String) extends PowerOffDecision{
  val powerOffDecisions : Seq[PowerOffDecision] = pOffDecisions
  val operator: String = op
   def shouldPowerOff(cellState: CellState, machineID: Int) : Boolean = {
     var powerOff = false
      assert(operator == "and" || operator == "or", "Operator must be on or and in "+name)
     assert (powerOffDecisions!=null && powerOffDecisions.length > 0 , "No power off decisions found in "+name)
     if (operator == "and"){
       powerOff = powerOffDecisions.foldLeft(true)(_ && _.shouldPowerOff(cellState, machineID))
     }
     else{
       powerOff = powerOffDecisions.foldLeft(false)(_ || _.shouldPowerOff(cellState, machineID))
     }
     powerOff
   }
   val name : String = {powerOffDecisions.map(_.name).mkString("combined-power-off-decision with power-off-decision-policies:",",","and_operator:"+operator)}
 }
