package efficiency.power_on_policies.decision

import ClusterSchedulingSimulation.{CellState, ClaimDelta, Job}

/**
  * Created by dfernandez on 15/1/16.
  */
object NoPowerOnDecision extends PowerOnDecision{
   override def shouldPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta], conflictedDelta: Seq[ClaimDelta]): Boolean = false

   override val name: String = "no-power-on-decision"
 }
