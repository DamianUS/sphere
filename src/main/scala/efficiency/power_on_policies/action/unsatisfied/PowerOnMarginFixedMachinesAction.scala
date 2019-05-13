package efficiency.power_on_policies.action.unsatisfied

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}
import efficiency.power_on_policies.action.PowerOnAction

import scala.util.control.Breaks

/**
 * Created by dfernandez on 15/1/16.
 */
class PowerOnMarginFixedMachinesAction(numMachinesMargin : Int) extends PowerOnAction{
  //On this policy, we will turn on the machines needed to give service to unsatisfied tasks plus a fixed number of machines as a security margin
  //FIXME: No tenemos en cuenta ni los conflicted delta ni el modo all or nothing, mejoras más adelante
  /*override def powerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Unit = {
    var machinesToPowerOn = 0
    val machinesNeeded = Math.max((job.cpusStillNeeded / cellState.cpusPerMachine).ceil.toInt, (job.memStillNeeded / cellState.memPerMachine).ceil.toInt) + numMachinesMargin
    if (cellState.numberOfMachinesOff >= machinesNeeded) {
      cellState.simulator.log(("There are enough machines turned off, turning on %d machines on %s policy").format(machinesNeeded + numMachinesMargin, name))
      machinesToPowerOn = machinesNeeded
    }
    else if (cellState.numberOfMachinesOff > 0) {
      cellState.simulator.log(("There are not enough machines turned off, turning on %d machines on %s policy").format(cellState.numberOfMachinesOff, name))
      machinesToPowerOn = cellState.numberOfMachinesOff
    }
    else {
      cellState.simulator.log(("All machines are on, cant turn on any machines on %d policy").format(name))
    }
    val loop = new Breaks;
    loop.breakable {
      for (i <- cellState.machinesLoad.length - 1 to 0 by -1) {
        if (machinesToPowerOn == 0) {
          loop.break
        }
        if (cellState.isMachineOff(cellState.machinesLoad(i))) {
          cellState.powerOnMachine(cellState.machinesLoad(i))
          machinesToPowerOn -= 1
        }
      }
    }
    assert(machinesToPowerOn == 0, ("Something went wrong on %s policy, there are still %d machines to turn on after powering on machines").format(name, machinesToPowerOn))
  }*/

  override val name: String = "power-on-fixed-machines-margin-action"

  override def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Int = {
    numMachinesMargin
  }
}
