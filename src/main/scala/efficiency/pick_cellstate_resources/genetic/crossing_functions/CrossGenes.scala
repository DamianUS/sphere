package efficiency.pick_cellstate_resources.genetic.crossing_functions

import ClusterSchedulingSimulation.{CellState, Job, Seed}
import efficiency.pick_cellstate_resources.genetic.fitness_functions.FitnessFunction

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.util.control.Breaks

/**
  * Created by dfernandez on 11/1/16.
  */
object CrossGenes extends CrossingFunction{

  val randomNumberGenerator = new util.Random(Seed())

  override def cross(chromosome1: ListBuffer[Int], chromosome2: ListBuffer[Int]): (ListBuffer[Int], ListBuffer[Int]) = {
    val numTasksC1 = chromosome1.filter(_ == 1).size
    val numTasksC2 = chromosome2.filter(_ == 1).size
    assert(numTasksC1 == numTasksC2, "Number of tasks not valid")
    var genesToCross = 1 + randomNumberGenerator.nextInt(numTasksC1/2)
    var balance = 0
    var mIDProcessed = new collection.mutable.ListBuffer[Int]()
    //var log = new collection.mutable.ListBuffer[String]()
    val loop = new Breaks;
    loop.breakable {
      while (genesToCross > 0 || balance != 0) {
        val crossingPoint = randomNumberGenerator.nextInt(chromosome1.size - 1)
        for (mID <- crossingPoint until chromosome1.size) {
          if (!mIDProcessed.contains(mID)) {
            if (balance == 0 && chromosome1(mID) == 1 && chromosome2(mID) == 1) {
              //If the value of this gene is the same, it remains equal
              genesToCross -= 1
              //log += "balance 0 C1 1 los dos 1"
              //assert(chromosome1.filter(_ == 1).size == chromosome2.filter(_ == 1).size, "Mal en balance 0 C1 1 los dos 1")
            }
            else if (balance == 0 && chromosome1(mID) == 1 && chromosome2(mID) == 0) {
              //Case: Chromosome 1 loses one tasks, new balance is -1
              chromosome1(mID) = 0
              chromosome2(mID) = 1
              //log += "balance 0 C1 1 C2 0 con c1" + chromosome1.filter(_ == 1).size + " y c2 " + chromosome2.filter(_ == 1).size
              //assert(chromosome1.filter(_ == 1).size == (chromosome2.filter(_ == 1).size) - 2, "Mal en balance 0 C1 1 C2 0 con c1 " + chromosome1.filter(_ == 1).size + " y c2 " + chromosome2.filter(_ == 1).size)
              balance -= 1
              genesToCross -= 1
            }
            else if (balance == 0 && chromosome1(mID) == 0 && chromosome2(mID) == 1) {
              //Case: Chromosome 1 gains one tasks, new balance is 1
              chromosome1(mID) = 1
              chromosome2(mID) = 0
              //log += "balance 0 C2 1 C1 0 con c1 " + chromosome1.filter(_ == 1).size + " y c2 " + chromosome2.filter(_ == 1).size
              //assert((chromosome1.filter(_ == 1).size) - 2 == chromosome2.filter(_ == 1).size, "Mal en balance 0 C2 1 C1 0 con c1 " + chromosome1.filter(_ == 1).size + " y c2 " + chromosome2.filter(_ == 1).size)
              balance += 1
              genesToCross -= 1
            }
            else if (balance == 1 && chromosome1(mID) == 1 && chromosome2(mID) == 0) {
              //Chromosome 1 has gained 1 task. We must exchange one 1 from the C1 with one 0 from the C2
              //Case: Chromosome 1 loses one tasks, new balance is 0
              chromosome1(mID) = 0
              chromosome2(mID) = 1
              //log +=  "balance 1"
              //assert(chromosome1.filter(_ == 1).size == chromosome2.filter(_ == 1).size, "Mal en balance 1")
              balance -= 1
              genesToCross -= 1
            }
            else if (balance == -1 && chromosome1(mID) == 0 && chromosome2(mID) == 1) {
              //Chromosome 2 has gained 1 task. We must exchange one 1 from the C2 with one 0 from the C1
              //Case: Chromosome 1 gains one tasks, new balance is 0
              chromosome1(mID) = 1
              chromosome2(mID) = 0
              //log +=  "balance -1"
              //assert(chromosome1.filter(_ == 1).size == chromosome2.filter(_ == 1).size, "Mal en balance -1")
              balance += 1
              genesToCross -= 1
            }

            if (genesToCross <= 0 && balance == 0) {
              loop.break;
            }

          }
          mIDProcessed += mID
        }
      }
    }
    val validC1 = chromosome1.filter(_ == 1).size == numTasksC1
    val validC2 = chromosome2.filter(_ == 1).size == numTasksC1
    assert(validC1, "Chromosome 1 numTasks not valid in "+name)
    assert(validC2, "Chromosome 2 numTasks not valid in "+name)
    (chromosome1, chromosome2)
  }

  override def newCross(chromosome1: HashMap[Int, ListBuffer[Int]], chromosome2: HashMap[Int, ListBuffer[Int]]): (HashMap[Int, ListBuffer[Int]], HashMap[Int, ListBuffer[Int]]) = {
    //println("Empiezo "+name)
    var numTasksC1 = 0
    for ((machineID,tasksMachine) <- chromosome1){
      numTasksC1 += tasksMachine.size
    }
    var numTasksC2 = 0
    for ((mID,tasksMachine2) <- chromosome2){
      numTasksC2 += tasksMachine2.size
    }
    //val numTasksC1 = chromosome1.values.map( x => x.size).sum
    //val numTasksC2 = chromosome2.values.map( x => x.size).sum
    assert(numTasksC1 == numTasksC2, "Number of tasks not valid")
    var genesToCross = randomNumberGenerator.nextInt(numTasksC1/2)
    assert(genesToCross >= 0, "no genes to cross")
    if(genesToCross > 0){
      var balance = 0
      var mIDProcessed1 = new ListBuffer[Int]()
      var mIDProcessed2 = new ListBuffer[Int]()

      //var log = new collection.mutable.ListBuffer[String]()
      val loop = new Breaks;
      loop.breakable {
        while (genesToCross > 0) {
          //Cross genes randomly between chromosome 1 and chromosome 2
          var numTries = 0 // avoid infinite loop
          var machine1IDfound = false
          var chromosome1MachineID = -1
          var machine2IDfound = false
          var chromosome2MachineID = -1
          while(!machine1IDfound && numTries < 50){
            chromosome1MachineID = chromosome1.keySet.toList(Random.nextInt(chromosome1.keySet.size))
            if(!mIDProcessed1.contains(chromosome1MachineID)){
              mIDProcessed1 += chromosome1MachineID
              machine1IDfound = true
            }
            numTries += 1
          }
          while(!machine2IDfound && numTries < 50){
            chromosome2MachineID = chromosome2.keySet.toList(Random.nextInt(chromosome2.keySet.size))
            if(!mIDProcessed2.contains(chromosome2MachineID)){
              mIDProcessed2 += chromosome2MachineID
              machine2IDfound = true
            }
            numTries += 1
          }
          if(chromosome1MachineID > -1 && chromosome2MachineID > -1){
            assert(chromosome1MachineID > -1, "Machine 1 ID not found")
            assert(chromosome2MachineID > -1, "Machine 2 ID not found")

            val chromosome1Tasks : ListBuffer[Int] = chromosome1.get(chromosome1MachineID).get
            val chromosome2Tasks : ListBuffer[Int] = chromosome2.get(chromosome2MachineID).get
            val chromosome1TaskID = if (chromosome1Tasks.size == 1) chromosome1Tasks(0) else chromosome1Tasks(Random.nextInt(chromosome1Tasks.size))
            val chromosome2TaskID = if (chromosome2Tasks.size == 1) chromosome2Tasks(0) else chromosome2Tasks(Random.nextInt(chromosome2Tasks.size))

            chromosome1Tasks -= chromosome1TaskID
            chromosome1Tasks += chromosome2TaskID

            chromosome2Tasks -= chromosome2TaskID
            chromosome2Tasks += chromosome1TaskID
          }
          genesToCross -= 1
        }
      }
    }
    //val validC1 = chromosome1.values.map( x => x.size).sum == numTasksC1
    //val validC2 = chromosome2.values.map( x => x.size).sum == numTasksC1
    //assert(validC1, "Chromosome 1 numTasks not valid in "+name)
    //assert(validC2, "Chromosome 2 numTasks not valid in "+name)
    //println("Llego a final de "+name)
    (chromosome1, chromosome2)
  }

  override val name: String = "cross-genes"
}
