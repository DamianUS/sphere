package efficiency.power_off_policies.decision.deterministic.security_margin

import ClusterSchedulingSimulation.CellState
import efficiency.power_off_policies.decision.PowerOffDecision

/**
   * Created by dfernandez on 15/1/16.
   */
class FreeCapacityMinPonderatedMarginPowerOffDecision(percentage : Double) extends PowerOffDecision{
    override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = {
      //TODO: Las políticas no ponderadas FreeCapacity(Mean/Min)MarginPowerOffDecision no tienen en cuenta la "utilidad" de los recursos
      //según la distribución de los mismos. La intuición nos dicta que una máquina con 8gb y 8 cores libres es mucho más útil que
      //8 máquinas con 1gb de ram y 1 core libre, ya que un task con la exigencia de 1.1gb de RAM y 1.1 cores no podría ser satisfecha con el último caso

      //TODO: Decidir cómo ponderar esa "utilidad", desde % de recursos disponibles en esa máquina para decir que un 100% de libertad (máquina entera disponible)
      //es lo que tiene más utilidad hasta compararlo con los requisitos de las tareas pasadas para saber la utilidad
      assert(percentage > 0 && percentage < 1, "Security margin percentage value must be between 0.001 and 0.999")
        percentage < 1 - Math.max(cellState.totalOccupiedCpus/cellState.totalCpus, cellState.totalOccupiedMem/cellState.totalMem)
    }

    override val name: String = ("free-capacity-min-ponderated-margin-power-off-decision-with-perc:%f").format(percentage)
  }
