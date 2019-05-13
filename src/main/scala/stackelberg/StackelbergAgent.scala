package stackelberg

import ClusterSchedulingSimulation.{ClaimDelta, _}

trait StackelbergAgent {
  val name : String
  def play(claimDeltas : Seq[ClaimDelta], cellState : CellState, job : Job, scheduler : Scheduler, simulator : ClusterSimulator)
  def duplicate() : StackelbergAgent
}
