package efficiency.pick_cellstate_resources.genetic

import ClusterSchedulingSimulation._
import efficiency.pick_cellstate_resources.CellStateResourcesPicker
import efficiency.pick_cellstate_resources.genetic.crossing_selectors.CrossingSelector
import efficiency.pick_cellstate_resources.genetic.fitness_functions.FitnessFunction
import efficiency.pick_cellstate_resources.genetic.mutating_functions.MutatingFunction

import scala.collection.mutable.{IndexedSeq, ListBuffer}
import scala.util.control.Breaks

/**
  * Created by dfernandez on 14/7/17.
  */
class GeneticStandardPicker(populationSize : Int = 20, crossoverProbability : Double = 0.7, mutationProbability : Double = 0.001, crossingSelector : CrossingSelector, fitnessFunction : FitnessFunction, epochNumber : Int = 500, mutatingFunction : MutatingFunction) extends CellStateResourcesPicker{
  val randomNumberGenerator = new util.Random(Seed())

  override def schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator): ListBuffer[ClaimDelta] = {
    //For batch jobs, this picker tries to minimize the makespan. Otherwise, random.
    if(job.workloadName == "Batch"){
      val makespanLog = scala.collection.mutable.ListBuffer.empty[Double]
      val claimDeltas = collection.mutable.ListBuffer[ClaimDelta]()
      var candidatePool = scheduler.candidatePoolCache.getOrElseUpdate(cellState.numberOfMachinesOn, Array.range(0, cellState.numMachines))
      var numRemainingTasks = job.unscheduledTasks
      var remainingCandidates = math.max(0, cellState.numberOfMachinesOn - scheduler.numMachinesBlackList).toInt
      var numTries =0
      val initialPopulation = scala.collection.mutable.ListBuffer.empty[ListBuffer[Int]]
      //Initialization
      for(i <- 1 to populationSize){
        var chromosome = scala.collection.mutable.ListBuffer.fill(cellState.numMachines)(0)
        for(task <- 0 until job.numTasks){
          var scheduled = false
          while (!scheduled){
            val range = 0 until cellState.numMachines
            val mID = (range(randomNumberGenerator.nextInt(range length)))
            if (chromosome(mID)==0 && cellState.isMachineOn(mID) && cellState.availableCpusPerMachine(mID) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mID) >= (job.memPerTask + 0.0001)) {
              assert(cellState.isMachineOn(mID), "Trying to pick a powered off machine with picker : "+name)
              chromosome(mID) = 1
              scheduled = true
            }
            else{
              numTries+=1
            }
          }
        }
        assert(chromosome.filter(_ == 1).size == job.numTasks, "The number of genes 1 and the number of tasks in the job don't match")
        initialPopulation += chromosome
      }
      // Now we have the initial Population. Let's get two chromosomes in order to crossing them if probabilty says so
      var population = initialPopulation
      for(epoch <- 1 to epochNumber){
        val newPopulation = scala.collection.mutable.ListBuffer.empty[ListBuffer[Int]]
        while(newPopulation.size < populationSize){
          val crossingChromosomes = crossingSelector.select(population = population, false, 2, fitnessFunction, job, cellState)
          assert(crossingChromosomes.size == 2, "2 chromosomes are needed to do the crossing process")
          var chromosome1 = crossingChromosomes(0)
          var chromosome2 = crossingChromosomes(1)
          if(randomNumberGenerator.nextDouble() < crossoverProbability){
            //Crossing
            val range = 0 until cellState.numMachines
            val crossingPoint = (range(randomNumberGenerator.nextInt(range length)))
            val (c1stays, c1crosses) = chromosome1.splitAt(crossingPoint)
            val (c2stays, c2crosses) = chromosome2.splitAt(crossingPoint)
            chromosome1 = c1stays ++ c2crosses
            chromosome2 = c2stays ++ c1crosses
          }
          //Let's introduce Mutation later
          //TODO : Mutation
          chromosome1 = mutatingFunction.mutate(chromosome1,mutationProbability,cellState,job)
          chromosome2 = mutatingFunction.mutate(chromosome2,mutationProbability,cellState,job)
          newPopulation += chromosome1
          newPopulation += chromosome2
        }
        population = newPopulation
        //Logging purposes
        var bestFitness = 0.0
        for (solution <- population){
          val fitness = fitnessFunction.evaluate(solution,job,cellState)
          if(bestFitness == 0.0 || fitness < bestFitness){
            bestFitness = fitness
          }
        }
        makespanLog += bestFitness
        //End of Logging purposes
      }
      //Choosing the best chromosome
      var bestFitness = 0.0
      var worstFitness = 0.0
      var bestSolution : ListBuffer[Int] = null
      var worstSolution : ListBuffer[Int] = null
      for (solution <- population){
        val fitness = fitnessFunction.evaluate(solution,job,cellState)
        if(worstFitness == 0.0 || fitness > worstFitness){
          worstFitness = fitness
          worstSolution = solution
        }
        else if(bestFitness == 0.0 || fitness < bestFitness){
          bestFitness = fitness
          bestSolution = solution
        }
      }
      //Now we have the best solution, let's create the ClaimDelta in order to deploy the tasks in those machines
      for(claimMachine <- 0 until cellState.numMachines){
        if(bestSolution(claimMachine)==1){
          assert(claimMachine >= 0 && claimMachine < cellState.machineSeqNums.length)
          val claimDelta = new ClaimDelta(scheduler,
            claimMachine,
            cellState.machineSeqNums(claimMachine),
            if (cellState.machinesHeterogeneous && job.workloadName == "Batch") job.taskDuration * cellState.machinesPerformance(claimMachine) else job.taskDuration,
            job.cpusPerTask,
            job.memPerTask,
            job = job)
          claimDelta.apply(cellState = cellState, locked = false)
          claimDeltas += claimDelta
        }
      }
      claimDeltas
    }
    else{
      super.schedule(cellState: CellState, job: Job, scheduler: Scheduler, simulator: ClusterSimulator)
    }
  }

  override def pickResource(cellState: CellState, job: Job, candidatePool: IndexedSeq[Int], remainingCandidates: Int) = {
    //THIS WOULD BE CALLED FOR NON-BATCH JOBS.
    var machineID = -1
    var numTries = 0
    var remainingCandidatesVar = remainingCandidates
    val candidatePoolVar = candidatePool
    val loop = new Breaks;
    loop.breakable {
      while (machineID == -1 && remainingCandidatesVar > 0) {
        val candidateIndex = randomNumberGenerator.nextInt(remainingCandidatesVar)
        val currMachID = candidatePoolVar(candidateIndex)
        if (cellState.availableCpusPerMachine(currMachID) >= (job.cpusPerTask + 0.0001) &&
          cellState.availableMemPerMachine(currMachID) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(currMachID)) {
          machineID=currMachID
          assert(cellState.isMachineOn(machineID), "Trying to pick a powered off machine with picker : "+name)
          loop.break;
        }
        else {
          numTries += 1
          candidatePoolVar(candidateIndex) = candidatePoolVar(remainingCandidatesVar - 1)
          candidatePoolVar(remainingCandidatesVar - 1) = currMachID
          remainingCandidatesVar -= 1
        }
      }
    }
    new Tuple4(machineID, numTries, remainingCandidatesVar, candidatePoolVar)
  }
  override val name: String = "genetic-standard-population:"+populationSize+"-cross:"+crossoverProbability+"-mutation:"+mutationProbability+"-epochs:"+epochNumber+"-fitness:"+fitnessFunction.name+"-selector:"+crossingSelector.name

}
