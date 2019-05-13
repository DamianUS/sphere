package efficiency.power_off_policies.decision.deterministic.security_margin

import ClusterSchedulingSimulation.CellState
import efficiency.DistributionUtils
import efficiency.power_off_policies.decision.PowerOffDecision

/**
  * Created by dfernandez on 15/1/16.
  */
class WeightedFreeCapacityMarginPowerOffDecision(percentage : Double, windowSize : Int) extends PowerOffDecision with DistributionUtils{
  override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = {
    assert(percentage > 0 && percentage < 1, "Security margin percentage value must be between 0.001 and 0.999")
    val allPastTuples = getPastTuples(cellState, windowSize)
    val jobAttributes = getJobAttributes(allPastTuples)
    val ramCpuMachineRatio = cellState.memPerMachine / cellState.cpusPerMachine
    val ramCpuUsedRatio = jobAttributes._3 / jobAttributes._5
    val usedAvailableRatio = Math.min(100.0, ramCpuUsedRatio/ramCpuMachineRatio)
    var should = false
    if(usedAvailableRatio > 1){
      should = percentage < Math.min((cellState.availableCpus/cellState.onCpus), (cellState.availableMem/cellState.onMem)/usedAvailableRatio)

    }
    else{
      should = percentage < Math.min((cellState.availableCpus/cellState.onCpus)/usedAvailableRatio, (cellState.availableMem/cellState.onMem))
    }
    should
  }

  override val name: String = ("weighted-free-capacity-margin-max-power-off-decision-with-perc:%f-and-window-size:%d").format(percentage, windowSize)
}
