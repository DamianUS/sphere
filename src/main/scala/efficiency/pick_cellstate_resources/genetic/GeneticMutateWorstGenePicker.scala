package efficiency.pick_cellstate_resources.genetic

import ClusterSchedulingSimulation._
import efficiency.pick_cellstate_resources.CellStateResourcesPicker
import efficiency.pick_cellstate_resources.genetic.crossing_functions.CrossingFunction
import efficiency.pick_cellstate_resources.genetic.crossing_selectors.CrossingSelector
import efficiency.pick_cellstate_resources.genetic.fitness_functions.{FitnessFunction, Makespan}

import scala.collection.mutable.{IndexedSeq, ListBuffer}
import scala.util.control.Breaks

/**
  * Created by dfernandez on 14/7/17.
  */
class GeneticMutateWorstGenePicker(populationSize : Int = 20, crossoverProbability : Double = 0.7, mutationProbability : Double = 0.5, crossingSelector : CrossingSelector, fitnessFunction : FitnessFunction, crossingFunction : CrossingFunction, epochNumber : Int = 500) extends CellStateResourcesPicker{
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
            /*//We can't cross like a standard genetic. Our random is the amount of tasks that we remain
            val range = 0 until cellState.numMachines
            val crossingPoint = (range(randomNumberGenerator.nextInt(range length)))
            val (c1stays, c1crosses) = chromosome1.splitAt(crossingPoint)
            val (c2stays, c2crosses) = chromosome2.splitAt(crossingPoint)
            chromosome1 = c1stays ++ c2crosses
            chromosome2 = c2stays ++ c1crosses*/
            val crossedChromosomes = crossingFunction.cross(chromosome1,chromosome2)
            chromosome1 = crossedChromosomes._1
            chromosome2 = crossedChromosomes._2
          }
          //Mutation chromosome 1
          //In this algo we change only 1 gene, the worst in terms of makespan
          if(randomNumberGenerator.nextDouble() < mutationProbability){
            var worstGene1 = -1
            var worstMakespan1 = 0.0
            for( worstMID1 <- 0 until cellState.numMachines){
              if(chromosome1(worstMID1)==1){
                if (worstMakespan1 == 0.0 || job.taskDuration * cellState.machinesPerformance(worstMID1) > worstMakespan1){
                  worstMakespan1 = job.taskDuration * cellState.machinesPerformance(worstMID1)
                  worstGene1 = worstMID1
                }
              }
            }
            assert(worstGene1 != -1, "Worst gene 1 not found")
            val currentGeneValue1 = chromosome1(worstGene1)
            val newGeneValue1= if(currentGeneValue1==1) 0 else 1
            var mutID1 = -1
            while(mutID1 == -1){
              val range1 = 0 until cellState.numMachines
              val mutationMachineID1 = (range1(randomNumberGenerator.nextInt(range1 length)))
              if (chromosome1(mutationMachineID1)==newGeneValue1 && cellState.availableCpusPerMachine(mutationMachineID1) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mutationMachineID1) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(mutationMachineID1)) {
                mutID1=mutationMachineID1
                assert(cellState.isMachineOn(mutationMachineID1), "Trying to pick a powered off machine with picker in mutation : "+name)
              }
            }
            chromosome1(worstGene1) = newGeneValue1
            chromosome1(mutID1) = currentGeneValue1
          }
          assert(chromosome1.filter(_ == 1).size == job.numTasks, "Chromosome 1 numTasks not valid after mutation")

          //Mutation chromosome 2
          //In this algo we change only 1 gene, the worst in terms of makespan
          if(randomNumberGenerator.nextDouble() < mutationProbability){
            var worstGene2 = -1
            var worstMakespan2 = 0.0
            for( worstMID2 <- 0 until cellState.numMachines){
              if(chromosome2(worstMID2)==1){
                if (worstMakespan2 == 0.0 || job.taskDuration * cellState.machinesPerformance(worstMID2) > worstMakespan2){
                  worstMakespan2 = job.taskDuration * cellState.machinesPerformance(worstMID2)
                  worstGene2 = worstMID2
                }
              }
            }
            assert(worstGene2 != -1, "Worst gene 2 not found")
            val currentGeneValue = chromosome2(worstGene2)
            val newGeneValue= if(currentGeneValue==1) 0 else 1
            var mutID = -1
            while(mutID == -1){
              val range = 0 until cellState.numMachines
              val mutationMachineID = (range(randomNumberGenerator.nextInt(range length)))
              if (chromosome2(mutationMachineID)==newGeneValue && cellState.availableCpusPerMachine(mutationMachineID) >= (job.cpusPerTask + 0.0001) && cellState.availableMemPerMachine(mutationMachineID) >= (job.memPerTask + 0.0001) && cellState.isMachineOn(mutationMachineID)) {
                mutID=mutationMachineID
                assert(cellState.isMachineOn(mutationMachineID), "Trying to pick a powered off machine with picker in mutation : "+name)
              }
            }
            chromosome2(worstGene2) = newGeneValue
            chromosome2(mutID) = currentGeneValue
          }
          assert(chromosome2.filter(_ == 1).size == job.numTasks, "Chromosome 2 numTasks not valid after mutation")

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
  override val name: String = "genetic-mutate-worst-population:"+populationSize+"-cross:"+crossoverProbability+"-mutation:"+mutationProbability+"-epochs:"+epochNumber+"-fitness:"+fitnessFunction.name+"-selector:"+crossingSelector.name

}
