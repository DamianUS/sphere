package stackelberg

import ClusterSchedulingSimulation._
import efficiency.power_off_policies.PowerOffPolicy
import efficiency.power_off_policies.decision.PowerOffDecision

import scala.collection.mutable.ListBuffer

//This class swtich between the power off policy specified during the creation of the experiment and a given power off policy. The method shouldChange is based on a power off decision given. The rationale behind this class is to have 2 specified power off policies: the current, less aggressive, and the specified in this class, more aggresive
class SwitchBetweenCurrentAndSpecified(powerOff : PowerOffPolicy, decision : PowerOffDecision) extends StackelbergAgent{

  var powerOffPolicy: PowerOffPolicy = powerOff
  var powerOffDecision: PowerOffDecision = decision
  var currentPowerOffPolicy: PowerOffPolicy = null

  override def play(claimDeltas : Seq[ClaimDelta], cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator): Unit = {
    if(currentPowerOffPolicy == null){
      currentPowerOffPolicy = simulator.powerOff
    }
    if(shouldChange(cellState)){
      simulator.powerOff = powerOffPolicy
    }
    else{
      simulator.powerOff = currentPowerOffPolicy
    }
  }

  def shouldChange(cellState: CellState): Boolean ={
    return decision.shouldPowerOff(cellState = cellState, 0)
  }
  override val name: String = ("stackelberg-current-decision:-%s-to-policy:-%s").format(powerOffDecision.name, powerOffPolicy.name)

  override def duplicate() : StackelbergAgent = {
    return new SwitchBetweenCurrentAndSpecified(powerOffPolicy,powerOffDecision)
  }
}
