package efficiency.power_off_policies.decision.deterministic

import ClusterSchedulingSimulation.CellState
import efficiency.power_off_policies.decision.PowerOffDecision

/**
 * Created by dfernandez on 15/1/16.
 */
object NoPowerOffDecision extends PowerOffDecision{
  override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = false

  override val name: String = "no-power-off-decision"
}
