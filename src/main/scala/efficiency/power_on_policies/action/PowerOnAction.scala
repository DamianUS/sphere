package efficiency.power_on_policies.action

import ClusterSchedulingSimulation.{ClaimDelta, Job, CellState}

import scala.util.control.Breaks

/**
 * Created by dfernandez on 15/1/16.
 */
trait PowerOnAction {
  //def powerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta] = Seq[ClaimDelta](), conflictedDelta: Seq[ClaimDelta] =Seq[ClaimDelta]())
  val name : String
  /*def powerOnMachines(cellState: CellState, numMachines: Int, schedType: String): Unit = {
    var machinesToPowerOn = numMachines
    val loop = new Breaks;
    if((schedType == "mesos" || schedType == "omega") && machinesToPowerOn > 0){
      cellState.simulator.sorter.orderResources(cellState)
    }
    loop.breakable {
      for (i <- cellState.numMachines - 1 to 0 by -1) {
        if (machinesToPowerOn == 0) {
          loop.break
        }
        if (cellState.isMachineOff(cellState.machinesLoad(i))) {
          cellState.powerOnMachine(cellState.machinesLoad(i))
          machinesToPowerOn -= 1
        }
      }
    }
    //println(("Tras encender quedan %d máquinas apagadas").format(cellState.numberOfMachinesOff))
    assert(machinesToPowerOn == 0, ("Something went wrong on %s policy, there are still %d machines to turn on after powering on machines").format(name, machinesToPowerOn))
  }*/

  def numberOfMachinesToPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta] = Seq[ClaimDelta](), conflictedDelta: Seq[ClaimDelta] =Seq[ClaimDelta]()): Int
}
