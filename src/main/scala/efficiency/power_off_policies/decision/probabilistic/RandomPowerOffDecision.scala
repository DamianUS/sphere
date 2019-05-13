package efficiency.power_off_policies.decision.probabilistic

import ClusterSchedulingSimulation.CellState
import efficiency.power_off_policies.decision.PowerOffDecision

import scala.util.Random

/**
  * Created by dfernandez on 15/1/16.
  */
class RandomPowerOffDecision(threshold: Double = 0.5) extends PowerOffDecision{
   override def shouldPowerOff(cellState: CellState, machineID: Int): Boolean = {
     //println(("On : %f y ocupadas: %f").format(cellState.numberOfMachinesOn.toDouble/cellState.numMachines, cellState.numMachinesOccupied.toDouble/cellState.numMachines))
     Random.nextFloat() < threshold
   }

   override val name: String = ("random-power-off-decision-with-threshold:%f").format(threshold)
 }
