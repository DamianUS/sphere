package efficiency.power_on_policies.decision

import ClusterSchedulingSimulation.{ClaimDelta, Job, CellState}

/**
  * Created by dfernandez on 15/1/16.
  */
class CombinedPowerOnDecision(pOffDecisions: Seq[PowerOnDecision], op: String) extends PowerOnDecision{
  val powerOffDecisions : Seq[PowerOnDecision] = pOffDecisions
  val operator: String = op
   def shouldPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Boolean = {
     var powerOff = false
      assert(operator == "and" || operator == "or", "Operator must be on or and in "+name)
     assert (powerOffDecisions!=null && powerOffDecisions.length > 0 , "No power off decisions found in "+name)
     if (operator == "and"){
       powerOff = powerOffDecisions.foldLeft(true)(_ && _.shouldPowerOn(cellState,job,schedType,commitedDelta,conflictedDelta))
     }
     else{
       powerOff = powerOffDecisions.foldLeft(false)(_ || _.shouldPowerOn(cellState,job,schedType,commitedDelta,conflictedDelta))
     }
     powerOff
   }
   val name : String = {powerOffDecisions.map(_.name).mkString("combined-power-on-decision-with-power-on-decision-policies:",",","and-operator:"+operator)}
 }
