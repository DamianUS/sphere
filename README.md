# Cluster scheduler simulator overview

This simulator can be used to prototype and compare different cluster scheduling strategies and policies. It generates synthetic cluster workloads from empirical parameter distributions (thus generating unique workloads even from a small amount of input data), simulates their scheduling and execution using a discrete event simulator, and finally permits analysis of scheduling performance metrics.

The simulator was originally written as part of research on the "Omega" shared-state cluster scheduling architecture at Google. A paper on Omega, published at EuroSys 2013, uses of this simulator for the comparative evaluation of Omega and other alternative architectures (referred to as a "lightweight" simulator there) [1]. As such, the simulators design is somewhat geared towards the comparative evaluation needs of this paper, but it does also permit more general experimentation with:

 * scheduling policies and logics (i.e. "what machine should a task be bound to?"),
 * resource models (i.e. "how are machines represented for scheduling, and how are they shared between tasks?"),
 * shared-cluster scheduling architectures (i.e. "how can multiple independent schedulers be supported for a large shared, multi-framework cluster?").

While the simulator will simulate job arrival, scheduler decision making and task placement, it does **not** simulate the actual execution of the tasks or variation in their runtime due to shared resources.

## Core concepts

**CellState**: The actual cluster. It is defined by the general **CellStateDesc** and overloaded and created by the different ClusterSimulatorDesc (Omega, Mesos, Monolithic).

**ClaimDelta**: Represents a "transaction" in the parallel schedulers (Omega and Mesos). It claims an amount of cpu and memory from the **CellState** (cluster).

**Job**: That unit of work that is enqueued. Each job is composed of many tasks. Each of these tasks are equal in duration, cpu and memory consumption. There are **Batch** and **Service** jobs, which are only distinguished by their duration and ram/cpu consumption and a label. At the execution level, they work the same way, although at the results level (queue times and so on), they are separated according to this type.

**Simulator**: The actual simulator that can be run. IT has the current simulation time and the job that must be executed. This is the superclass of one of the most important classes of the simulator, i.e.: **ClusterSimulator**.

**ClusterSimulator**: Child of **Simulator**, this class receives the **CellState**, the set of **Scheduler**s, **Workload**s, and (added by us), the **Sorter**, **Picker**, **Power Off Policy** and **Power On Policy**  used in that experiment. This class hold almost all the energy-efficiency related data of the simulation, i.e: average cpu and memory utilization, average number of machines on, kWh saved per shut-down, and so on. This class does measure the cluster state every X seconds in order to produce the results. 

**Workload**: Set of jobs of one type (Batch or Service). This class has the aggregated attributes as queue times, average inter arrival times, and so on.

**Scheduler**: Given a **Job** and a **CellState**, find machines that the tasks of the job will fit into, and allocate the resources on that machine to those tasks, accounting those resoures to this scheduler, modifying the provided **CellState** (by calling apply() on the created **ClaimDelta**s). However, this class does not implement the specifics of each scheduling framework (Monolithic, Mesos, Omega) scheduling logic. This logic is present in child classes: **MonolithicScheduler**, **MesosScheduler**, and **OmegaScheduler**, which handle the arrival of new jobs from the simulator's agenda and the actual scheduling process.
 
Energy-efficiency related concepts: (can be found in **/src/main/scala/efficiency**):
 
**Power Off Policy**: The strategy used in order to shut down a machine when a task is finished (if there are no more tasks running in this server). The **Power Off Policy** is composed of: A **Decision** that tells whether a machine should be turned off; and an **Action** that actually performs the shut-down process.  

**Power On Policy**: The strategy used in order to power a (set of) machine(s) when a task is scheduled It is responsible of actually perform the powering on process. The **Power On Policy** is composed of: A **Decision** that tells whether current machines are not sufficient to perform the load; and an **Action** that tells the number of machines that should be powered on.  

**Sorter**: Performs a sorting of the machines according to the suitability to host an incoming task.

**Picker**: Decides which machine should be the right candidate to host a task after the sorting process.

## Workflow

1)The workload generators are defined in Workloads.scala. In this file we have multiple configurations: from pure synthetic "flat" workloads whose jobs' attributes are generated by following exponential distributions to realistic workloads that are read from a csv file.

2)In Simulation.scala we can find the real configuration. This file is packed with the simulator results (.protobuf files) in order to see what was executed. Here we can set, among other things:
    
    * The number of concurrent schedulers that will serve each workload type 
    * Which workload configuration from Workloads.scala is used. 
    * How many parameters will be swept over (e.g: **constant think time**: the time spent by the scheduler to make a decision at the job level, **per task think time**: the time spent by the scheduler to make a decision at the task level and so on)
    * Simulation time
    * Which scheduling frameworks must be executed
    * Prefill (amount of charge that will be executed as a minimum during all the execution time) charge limit
    
**Energy-efficiency related configuration**:
    
    * Which sorting policies must be executed
    * Which picking policies must be executed
    * Which power-off policies must be executed
    * Which power-off-policy attributes must be swept over
    * Which power-on policies must be executed
    * Which power-on-policy attributes must be swept over
    
3)Experiment running will read the Simulation.scala file, generate the simulations described in this file and run them.

4)We will get a folder with the .protobuf files (you can see in /src/protocolbuffers/cluster_simulation_protos.proto the structure and in https://developers.google.com/protocol-buffers/ how it works if you are not familiar with this format) and the Simulation.scala file.

5).csv files and graphics may be then generated from the .protobuf files (in /scr/main/python. The more interesting is generate-txt-from-protobuff.py). Our developed graphing scripts can be found in: https://github.com/DamianUS/graphs-scripts


## Scheduling Logic

### Monolithic (**/src/main/scala/MonolithicSimulation.scala**)

As an omniscient and only scheduler, the monolithic scheduling framework checks to see if there is currently a job in this scheduler's job queue. If there is, and this scheduler is not currently scheduling a job, then pop that job off of the queue and "begin scheduling it". Scheduling a job consists of setting this scheduler's state to scheduling = true, and adding a finishSchedulingJobAction to the simulators event queue by calling afterDelay(). The "scheduling process" (i.e. deciding which machine should host a given task) has been divided into the "sorting" and "picking" subprocesses which are executed by the **Sorter** and **Picker** passed as parameters to the **Simulator**.

### Mesos (**/src/main/scala/MesosSimulation.scala**)

The mesos scheduling logic ( https://cs.stanford.edu/~matei/papers/2011/nsdi_mesos.pdf summarizing a pessimistic locking concurrent strategy) is coded in **MesosAllocator** and **MesosScheduler** classes. When a job arrives, the allocator is notified, so that it can make us offers until we notify it that we don't have any more jobs, at which time it can stop sending us offers.

### Omega (**/src/main/scala/OmegaSimulation.scala**)

The omega scheduling logic ( summarizing an optimistic locking concurrent strategy) is coded in **OmegaScheduler** class. When a job arrives, it creates an internal copy of the **CellState**, it schedules the job using this (probably out of date) internal copy of the **CellState** and it submits a transaction to the common (real) **CellState** for it. If not all tasks in the job are successfully committed, put it back in the pendingQueue to be scheduled again.


## Power off policies families (**/src/main/scala/efficiency/power_off_policies**)

### Never Power Off

This power-off decision policy disables the power-off process, and therefore represents the current scenario. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/deterministic/NoPowerOffDecision.scala**)

### Always Power Off 

This power-off decision policy will shut down every machine after freeing all the resources under use, whenever possible. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/deterministic/AlwzPowerOffDecision.scala**) 

### Random Power Off 

This policy switches off and randomly leaves the resources idle by following a Bernoulli distribution whose parameter is equal to 0.5. This policy is useful to ascertain the accuracy of the predictions made by the following probabilistic policies. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/probabilistic/RandomPowerOffDecision.scala**)

### Load 

This power-off decision policy takes into account the maximum resource pressure of the data-center load and compares it to a given threshold. If the current load is less than this given threshold, then the machine will be powered off. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/deterministic/load/LoadMaxPowerOffDecision.scala**)

### Security Margin 

This power-off decision policy assures that at least a given percentage of resources is turned on, free, and available in order to respond to peak loads. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/deterministic/security_margin/FreeCapacityMinMarginPowerOffDecision.scala**)

### Exponential

Under the hypothesis that the arrival of new jobs that could harm the data center performance follows an Exponential distribution, this energy policy attempts to predict the arrival of new jobs that can harm the data-center performance due to the lack of sufficient resources for their execution. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/probabilistic/ExponentialPowerOffDecision.scala**)

### Gamma 

Under the hypothesis that the arrival of new jobs follows a Gamma distribution, this energy policy attempts to predict the arrival of the amount of new jobs required to oversubscribe the available resources. It can be found in (**/src/main/scala/efficiency/power_off_policies/decision/probabilistic/GammaPowerOffDecision.scala**)

## Sorting (**/src/main/scala/efficiency/ordering_cellstate_resources_policies**)

These agents are responsible for computing the suitability of every machine in the data center in order to place a task thereon, and for sorting these machines in terms of this suitability (to create an array of ordered machines as an attribute of the **CellState**). Once all the machines are sorted in terms of these strategies, the **Picker** can choose the more suitable machine to host the incoming tasks.
 accept new jobs. To this end, the level of occupation of a machine is denoted by its CPU and RAM load in a range of [0,1], where 0 represents an idle machine, and 1 a fully occupied machine. It can be found in (**/src/main/scala/efficiency/ordering_cellstate_resources_policies/PowerStateLoadSorter.scala**)

## Picking (**/src/main/scala/efficiency/pick_cellstate_resources**)

These agents are responsible for choosing a candidate from a previously created candidate pool in order to place a task thereon. The most relevant picker can be found in (**/src/main/scala/efficiency/pick_cellstate_resources/SpreadMarginReversePickerCandidatePower.scala**). 

This approach is of major importance in avoiding resource conflicts and eliminating resource contention in distributed shared-state schedulers, such as Omega, which can arise due to the same machine being picked from various concurrent schedulers. 
One way that this can be achieved is by adding a certain randomness to the picking-decision process. This policy selects the machines with the highest level of occupation from the available candidate pool that can host the task, but once the most occupied machine is found, a machine pivot that meets a specified safety margin is selected.

This can be very useful in the prevention of runtime resource oversubscription.

## Downloading, building, and running

The source code for the simulator is available in a Git repository hosted on Google Code. Instructions for downloading  can be found at at https://code.google.com/p/cluster-scheduler-simulator/source/checkout.

The simulator is written in Scala, and requires the Simple Build Tool (`sbt`) to run. A copy of `sbt` is package with the source code, but you will need the following prerequisites in order to run the simulator:

 * a working JVM (`openjdk-6-jre` and `openjdk-6-jdk` packages in mid-2013 Ubuntu packages),
 * a working installation of Scala (`scala` Ubuntu package),
 * Python 2.6 or above and matplotlib 1.0 or above for generation of graphs (`python-2.7` and `python-matplotlib` Ubuntu packages).

Once you have ensured that all of these exist, simply type `bin/sbt run` from the project home directory in order to run the simulator:

    $ bin/sbt run
    [...]
    [info] Compiling 9 Scala sources and 1 Java source to ${WORKING_DIR}/target/scala-2.9.1/classes...
    [...]
    [info] Running Simulation 
    
    RUNNING CLUSTER SIMULATOR EXPERIMENTS
    ------------------------
    [...]

### Using command line flags

The simulator can be passed some command-line arugments via configuration flags, such as `--thread-pool-size NUM_THREADS_INT` and `--random-seed SEED_VAL_INT`. To view all options run:

    $ bin/sbt "run --help"

Note that when passing command line options to the `sbt run` command you need to include the word `run` and all of the options that follow it within a single set of quotes. `sbt` can also be used via the `sbt` console by simply running `bin/sbt` which will drop you at a prompt. If you are using this `sbt` console option, you do not need to put quotes around the run command and any flags you pass.

### Configuration file
If a file `conf/cluster-sim-env.sh` exists, it will be sourced in the shell before the simulator is run. This was added as a way of setting up the JVM (e.g. heap size) for simulator runs. Check out `conf/cluster-sim-env.sh.template` as a starting point; you will need to uncomment and possibly modify the example configuration value set in that template file (and, of course, you will need to create a copy of the file removing the ".template" suffix).


## Configuring experiments

The simulation is controlled by the experiments configured in the `src/main/scala/Simulation.scala` setup file. Comments in the file explain how to set up different workloads, workload-to-scheduler mappings and simulated cluster and machine sizes.

Most of the workload setup happens in `src/main/scala/Workloads.scala`, so read through that file and make modifications there to have the simulator read from a trace file of your own (see more below about the type of trace files the simulator uses, and the example files included).

Workloads in the simulator are generated from *empirical parameter distributions*. These are typically based on cluster *snapshots* (at a point in time) or *traces* (sequences of events over time). We unfortunately cannot provide the full input data used for our experiments with the simulator, but we do provide example input files in the `traces` subdirectory, illustrating the expected data format (further explained in the local README file in `traces`). The following inputs are required:

 * **initial cluster state**: when the simulation starts, the simulated cluster obviously cannot start off empty. Instead, we pre-load it with a set of running jobs (and tasks) at this point in time. These jobs start before the beginning of simulation, and may end during the simulation or after. The example file `traces/example-init-cluster-state.log` shows the input format for the jobs in the initial cluster state, as well as the departure events of those of them which end during the simulation. The resource footprints of tasks generated at simulation runtime will also be sampled from the distribution of resource footprints of tasks in the initial cluster state.
 * **job parameters**: the simulator samples three key parameters for each job from empirical distributions (i.e. randomly picks values from a large set):
    1. Job sizes (`traces/job-distribution-traces/example_csizes_cmb.log`): the number of tasks in the generated job. We assume for simplicity that all tasks in a job have the same resource footprint.
    2. Job inter-arrival times (`traces/job-distribution-traces/example_interarrival_cmb.log`): the time in between job arrivals for each workload (in seconds). The value drawn from this distribution indicates how many seconds elapse until another job arrives, i.e. the "gaps" in between jobs.
    3. Job runtimes (`traces/job-distribution-traces/example_runtimes_cmb.log`): total job runtime. For simplicity, we assume that all tasks in a job run for exactly this long (although if a task gets scheduled later, it will also finish later).

For further details, see `traces/README.txt` and `traces/job-distribution-traces/README.txt`.

**Please note that the resource amounts specified in the example data files, and the example cluster machines configured in `Simulation.scala` do *not* reflect Google configurations. They are made-up numbers, so please do not quote them or try to interpret them!**

A possible starting point for generating realistic input data is the public Google cluster trace [2, 3]. It should be straightforward to write scripts that extract the relevant data from the public trace's event logs. Although we do not provide such scripts, it is worth noting that the "cluster C" workload in the EuroSys paper [1] represents the same workload as the public trace. (If you do write scripts for converting the public trace into simulator format, please let us know, and we will happily include them in the simulator code release!)

## Experimental results: post-processing

Experimental results are stored in serialized Protocol Buffers in the `experiment_results` directory at the root of the source tree by default: one subdirectory for each experiment, and with a unique name identifying the experimental setup as well as the start time. The schemas for the `.protobuf` files are stored in `src/main/protocolbuffers`.

A script for post-processing and graphing experimental results is located in `src/main/python/graphing-scripts`, and `src/main/python` also contains scripts for converting the protobuf-encoded results into ASCII CSV files. See the README file in the `graphing-scripts` directory for detailed explanation.

## NOTES

### Changing and compiling the protocol buffers

If you make changes to the protocol buffer file (in `src/main/protocolbuffers`), you will need to recompile them, which will generate updated Java files in `src/main/java`. To do so, you must install the protcol buffer compiler and run `src/main/protocolbuffers/compile_protobufs.sh`, which itself calls `protoc` (which it assumes is on your `PATH`).

### Known issues

- The `schedulePartialJobs` option is used in the current implementation of the `MesosScheduler` class. Partial jobs are always scheduled (even if this flag is set to false). Hence the `mesosSimulatorSingleSchedulerZeroResourceJobsTest` currently fails to pass.

## Contributing, Development Status, and Contact Info

Please use  the Google Code [project issue tracker](https://code.google.com/p/cluster-scheduler-simulator/issues/list) for all bug reports, pull requests and patches, although we are unlikely to be able to respond to feature requests. You can also send any kind of feedback to the developers, [Andy Konwinski](http://andykonwinski.com/) and [Malte Schwarzkopf](http://www.cl.cam.ac.uk/~ms705/).

## References

[1] Malte Schwarzkopf, Andy Konwinski, Michael Abd-El-Malek and John Wilkes. **[Omega: flexible, scalable schedulers for large compute clusters](http://eurosys2013.tudos.org/wp-content/uploads/2013/paper/Schwarzkopf.pdf)**. In *Proceedings of the 8th European Conference on Computer Systems (EuroSys 2013)*.

[2] Charles Reiss, Alexey Tumanov, Gregory Ganger, Randy Katz and Michael Kotzuch. **[Heterogeneity and Dynamicity of Clouds at Scale: Google Trace Analysis](http://www.pdl.cmu.edu/PDL-FTP/CloudComputing/googletrace-socc2012.pdf)**. In *Proceedings of the 3rd ACM Symposium on Cloud Computing (SoCC 2012)*.

[3] Google public cluster workload traces. [https://code.google.com/p/googleclusterdata/](https://code.google.com/p/googleclusterdata/).
