package efficiency.power_off_policies.action

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}

import scala.util.control.Breaks

/**
 * Created by dfernandez on 15/1/16.
 */
object DefaultPowerOffAction extends PowerOffAction{
  override def powerOff(cellState: CellState, machineID: Int): Unit = {
    val state = cellState.isMachineOn(machineID)
    //assert(cellState.isMachineOn(machineID), ("The machine with ID %d is not powered on when arriving to power off policy: %s").format(machineID, name))
    //println(("Entra en apagar la máquina %d cuando quedan %d máquinas encendidas").format(machineID, cellState.numberOfMachinesOn))
    if(cellState.isMachineOn(machineID) && cellState.allocatedCpusPerMachine(machineID) <= 0.00001 && cellState.allocatedMemPerMachine(machineID) <= 0.00001) {
      cellState.powerOffMachine(machineID)
      cellState.simulator.log(("Shutting down the machine with machine ID : %d in the power off policy : %s").format(machineID, name))
      //println(("Tras apagar la máquina %d quedan %d máquinas encendidas").format(machineID, cellState.numberOfMachinesOn))
    }
    else{
      cellState.simulator.log(("Can not shut down the machine with machine ID : %d in the power off policy : %s because it has allocated %f cpus and %f mem").format(machineID, name, cellState.allocatedCpusPerMachine(machineID), cellState.allocatedMemPerMachine(machineID)))
      //println(("No puede apagar la máquina %d porque tiene ocupadas %f cpus and %f mem así que quedan %d máquinas encendidas").format(machineID, cellState.allocatedCpusPerMachine(machineID), cellState.allocatedMemPerMachine(machineID), cellState.numberOfMachinesOn))
    }
  }

  override val name: String = "default"
}
