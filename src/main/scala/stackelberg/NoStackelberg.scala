package stackelberg
import ClusterSchedulingSimulation._

object NoStackelberg extends StackelbergAgent {
  override def play(claimDeltas : Seq[ClaimDelta], cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator): Unit = {

  }


  override val name = "no-stackelberg"

  override def duplicate() : StackelbergAgent = {return this}
}
