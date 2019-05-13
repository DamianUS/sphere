package efficiency.power_off_policies

import efficiency.power_off_policies.action.PowerOffAction
import efficiency.power_off_policies.decision.PowerOffDecision

/**
 * Created by dfernandez on 13/1/16.
 */
class ComposedPowerOffPolicy(action : PowerOffAction, decision : PowerOffDecision, doGlobalCheck : Boolean = false, globalPeriod : Double = 300.0) extends PowerOffPolicy{

  override var powerOffAction : PowerOffAction = action
  override var powerOffDecisionPolicy : PowerOffDecision = decision
  override val shouldPerformGlobalCheck : Boolean = doGlobalCheck
  override val globalCheckPeriod : Double = globalPeriod

  override val name: String = ("composed-off-decision:%s-action:%s").format(powerOffDecisionPolicy.name, powerOffAction.name)

}
