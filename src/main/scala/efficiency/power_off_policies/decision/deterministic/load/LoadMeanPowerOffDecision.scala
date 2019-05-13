package efficiency.power_off_policies.decision.deterministic.load

import ClusterSchedulingSimulation.CellState
import efficiency.power_off_policies.decision.PowerOffDecision

/**
   * Created by dfernandez on 15/1/16.
   */
class LoadMeanPowerOffDecision(threshold : Double) extends PowerOffDecision{
    override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = {
        threshold > ((cellState.totalOccupiedCpus/cellState.totalCpus + cellState.totalOccupiedMem/cellState.totalMem)/2)
    }

    override val name: String = ("load-mean-power-off-decision-with-margin:%f").format(threshold)
  }
