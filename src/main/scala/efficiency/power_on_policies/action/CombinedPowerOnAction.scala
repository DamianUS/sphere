package efficiency.power_on_policies.action

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.DistributionUtils

/**
  * Created by dfernandez on 15/1/16.
  */
class CombinedPowerOnAction(pOnActions: Seq[PowerOnAction], op: String) extends PowerOnAction with DistributionUtils{
  val powerOnActions : Seq[PowerOnAction] = pOnActions
  val operator: String = op
  val name : String = {powerOnActions.map(_.name).mkString("combined-action:",",","-operator:"+operator)}

  override def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Int = {
    assert(operator == "sum" || operator == "min" || operator == "max" || operator == "mean", "Operator must be sum, min, max or mean in "+name)
    var numberOfMachines = 0
    if(operator == "sum"){
      powerOnActions.foreach(powerOnAction => numberOfMachines += powerOnAction.numberOfMachinesToPowerOn(cellState, job, schedType, commitedDelta, conflictedDelta))
    }
    else if(operator == "min"){
      powerOnActions.map(powerOnAction => powerOnAction.numberOfMachinesToPowerOn(cellState, job, schedType, commitedDelta, conflictedDelta)).min
    }
    else if(operator == "max"){
      powerOnActions.map(powerOnAction => powerOnAction.numberOfMachinesToPowerOn(cellState, job, schedType, commitedDelta, conflictedDelta)).max
    }
    else{
      numberOfMachines = mean(powerOnActions.map(powerOnAction => powerOnAction.numberOfMachinesToPowerOn(cellState, job, schedType, commitedDelta, conflictedDelta))).ceil.toInt
    }
    numberOfMachines
  }
}
