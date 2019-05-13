package efficiency.power_on_policies

import ClusterSchedulingSimulation.{CellState, Job}
import efficiency.power_on_policies.action.PowerOnAction
import efficiency.power_on_policies.decision.PowerOnDecision

import scala.util.control.Breaks

/**
 * Created by dfernandez on 13/1/16.
 */
class ComposedPowerOnPolicy(action : PowerOnAction, decision : PowerOnDecision) extends PowerOnPolicy{

  override var powerOnAction: PowerOnAction = action
  override var powerOnDecisionPolicy: PowerOnDecision = decision
  override val name: String = ("composed-decision:-%s-action:-%s").format(powerOnDecisionPolicy.name, powerOnAction.name)

}
