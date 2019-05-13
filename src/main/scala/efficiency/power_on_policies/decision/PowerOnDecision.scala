package efficiency.power_on_policies.decision

import ClusterSchedulingSimulation.{ClaimDelta, Job, CellState}

/**
 * Created by dfernandez on 15/1/16.
 */
trait PowerOnDecision {
  def shouldPowerOn(cellState: CellState, job: Job, schedType: String, commitedDelta: Seq[ClaimDelta] = Seq[ClaimDelta](), conflictedDelta: Seq[ClaimDelta] =Seq[ClaimDelta]()) : Boolean
  def name : String
}
