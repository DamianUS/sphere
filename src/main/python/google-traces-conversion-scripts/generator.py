#  This script generates the files needed by cluster simulator from google traces
#  input 1: Google simulator job_events file (we assume that all csv files are concatenated in one file)
#  input 2: Google simulator task_events file (we assume that all csv files are concatenated in one file)
#  output 1: Debug file that prints all data from all jobs. Each row represents one job
#  Format: job_id | scheduling_class | submit_timestamp | schedule_timestamp | finish_timestamp
#   | aggregated_cpu (num cores) | aggregated_memory (in bytes, assumed 4gb RAM) | interarrival_time
#   | task_number
#  output 2: init-cluster-state file containing jobs that started before simulation time window as
#  described in https://github.com/google/cluster-scheduler-simulator/tree/master/traces
#  output 3: all-cluster-state file containing all jobs as in order to generate more realistic synthetic jobs
#  described in https://github.com/google/cluster-scheduler-simulator/tree/master/traces
#  output 4: csizes_cmb as described in https://github.com/google/cluster-scheduler-simulator/tree/master/traces/job-distribution-traces
#  output 5: runtimes_cmb as described in https://github.com/google/cluster-scheduler-simulator/tree/master/traces/job-distribution-traces
#  output 6: runtimes_cmb as described in https://github.com/google/cluster-scheduler-simulator/tree/master/traces/job-distribution-traces
#

import collections
import csv


class Job:
    def __init__(self, s_timestamp, j_id, s_class):
        self.submit_timestamp = s_timestamp
        self.finish_timestamp = None
        self.schedule_timestamp = None
        self.interarrival_time = None
        self.job_id = j_id
        self.scheduling_class = s_class
        self.task_number = int(0)
        self.aggregated_cpu = float(0)
        self.aggregated_memory = float(0)


# In this script, we will iterate over google traces job events and capture relevant information to create
# google simulator log format

machine_ram = int(8)
prefill_dict = {}
last_interarrival_timestamp = None  # we assume interarrival as time between submit events

# job csv file columns constants

job_events_file_path = '/Users/dfernandez/google/clusterdata-2011-2/job_events/jobs.csv'

job_timestamp_column = 0
job_id_column = 2
job_event_type_column = 3
job_scheduling_class = 5

# task csv file columns constants

task_events_file_path = '/Users/dfernandez/google/clusterdata-2011-2/task_events/task_events.csv'

task_job_id_column = 2
task_event_type_column = 5
task_requested_cpu_column = 9
task_requested_memory_column = 10

# simplified job csv file columns constants

job_events_file_path = '/Users/dfernandez/google/clusterdata-2011-2/job_events/jobs_simplificado.csv'

job_timestamp_column = 0
job_id_column = 1
job_event_type_column = 2
job_scheduling_class = 3

# simplified task csv file columns constants

task_events_file_path = '/Users/dfernandez/google/clusterdata-2011-2/task_events/task_events_simplificado.csv'

task_job_id_column = 0
task_event_type_column = 1
task_requested_cpu_column = 2
task_requested_memory_column = 3

with open(job_events_file_path, 'rb') as csvfile:
    jobsreader = csv.reader(csvfile)
    for row in jobsreader:
        timestamp = float(row[job_timestamp_column])
        # 0 means before running simmulation, 2^63-1 means after running simulation
        if timestamp > float(0):
            timestamp = float(timestamp) / float(10 ** 6) - float(600)
            # converted to seconds and substracted 600 seconds as google says
        job_id = int(row[job_id_column])
        event_type = int(row[job_event_type_column])
        # 0 -> submit, 1-> schedule, 2-> evict, 3-> fail, 4-> finish
        # 5-> kill, 6-> lost, 7-> update_pending, 8-> update_running
        scheduling_class = int(row[job_scheduling_class])
        # 0 -> non production, 1 -> the less latency-sensitive on production
        # #  2 -> medium latency-sensitive on production, 3 -> the most latency-sensitive on production
        # Lost jobs will be ignored
        if event_type == 0:
            job = Job(timestamp, job_id, scheduling_class)
            prefill_dict[job_id] = job
            if timestamp > float(0):
                # on the first event in window time we can not assume that previous job arrived at second 0
                # so we cant determine the first inter arrival time
                if last_interarrival_timestamp is not None:
                    prefill_dict[job_id].interarrival_time = timestamp - last_interarrival_timestamp
                last_interarrival_timestamp = timestamp
        elif event_type == 1:
            prefill_dict[job_id].schedule_timestamp = timestamp
        elif 2 <= event_type <= 5:
            prefill_dict[job_id].finish_timestamp = timestamp
            # now we can calculate job runtime as job.finish_timestamp - job.schedule_timestamp
            # once we have iterated over all jobs, we will fill jobs with aggregated cpu & mem info, num_tasks, etc.
            # we will assume that each task asks for some cpu and some memory. As known, some tasks does not require
            # none of them. We can later ignore jobs with aggr_cpu and aggr_memory equals to 0 if necessary
            # We will assume too (to simplify) that each task event with event_type 0 means a new tasks,
            # without taking into account errors nor resubmits / reschedulings

with open(task_events_file_path, 'rb') as taskscsvfile:
    tasksreader = csv.reader(taskscsvfile)
    for task_row in tasksreader:
        job_id = int(task_row[task_job_id_column])
        event_type = int(task_row[task_event_type_column])
        requested_cpu = float(0)
        requested_memory = float(0)
        if task_row[task_requested_cpu_column] and not task_row[task_requested_cpu_column].isspace():
            requested_cpu = float(task_row[task_requested_cpu_column])
        if task_row[task_requested_memory_column] and not task_row[task_requested_memory_column].isspace():
            requested_memory = float(task_row[task_requested_memory_column])
        if event_type == 0:
            prefill_dict[job_id].aggregated_cpu += requested_cpu
            prefill_dict[job_id].aggregated_memory += requested_memory
            prefill_dict[job_id].task_number += 1


debug_file = open('/Users/dfernandez/simulacion_python/debug.log', 'wb')
debug_writer = csv.writer(debug_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_NONE)
init_state_file = open('/Users/dfernandez/simulacion_python/example-init-cluster-state.log', 'wb')
all_state_file = open('/Users/dfernandez/simulacion_python/example-all-cluster-state.log', 'wb')
sizes_file = open('/Users/dfernandez/simulacion_python/example_csizes_cmb.log', 'wb')
interarrivals_file = open('/Users/dfernandez/simulacion_python/example_interarrival_cmb.log', 'wb')
runtimes_file = open('/Users/dfernandez/simulacion_python/example_runtimes_cmb.log', 'wb')
prefill_writer = csv.writer(init_state_file, delimiter=' ', quotechar='"', quoting=csv.QUOTE_NONE)
all_writer = csv.writer(all_state_file, delimiter=' ', quotechar='"', quoting=csv.QUOTE_NONE)
sizes_writer = csv.writer(sizes_file, delimiter=' ', quotechar='"', quoting=csv.QUOTE_NONE)
interarrivals_writer = csv.writer(interarrivals_file, delimiter=' ', quotechar='"', quoting=csv.QUOTE_NONE)
runtimes_writer = csv.writer(runtimes_file, delimiter=' ', quotechar='"', quoting=csv.QUOTE_NONE)

cluster_name = 'example_cluster'
assignment_policy = 'cmb-new'

ordered_jobs = collections.OrderedDict(sorted(prefill_dict.items()))

for key, print_job in ordered_jobs.items():

    # # we generate an intermediate file to debug results
    # debug_row = list()
    # debug_row.append(print_job.job_id)
    # debug_row.append(print_job.scheduling_class)
    # debug_row.append(print_job.submit_timestamp)
    # debug_row.append(print_job.schedule_timestamp)
    # debug_row.append(print_job.finish_timestamp)
    # debug_row.append(print_job.aggregated_cpu)
    # debug_row.append(print_job.aggregated_memory)
    # debug_row.append(print_job.interarrival_time)
    # debug_row.append(print_job.task_number)
    # if print_job.schedule_timestamp is not None and print_job.finish_timestamp is not None:
    #     debug_row.append(print_job.finish_timestamp - print_job.schedule_timestamp)
    # else:
    #     debug_row.append('')
    # debug_writer.writerow(debug_row)

    job_row = list()
    sizes_row = list()
    interarrivals_row = list()
    runtimes_row = list()
    finish_job_row = list()

    # init cluster state
    if print_job.job_id is not None and print_job.scheduling_class is not None \
            and print_job.task_number > int(0) \
            and print_job.aggregated_cpu > float(0) \
            and print_job.aggregated_memory > float(0):
        time_window = 11
        job_row.append(time_window)
        if print_job.schedule_timestamp is None:
            job_row.append(float(0))
        else:
            job_row.append(print_job.schedule_timestamp)
        job_row.append(print_job.job_id)
        job_row.append(int(print_job.scheduling_class > 0))
        job_row.append(print_job.scheduling_class)
        job_row.append(print_job.task_number)
        job_row.append(print_job.aggregated_cpu)  # num cores
        job_row.append(print_job.aggregated_memory * 1024 * 1024 * 1024 * machine_ram)
        all_writer.writerow(job_row)
        if not print_job.submit_timestamp > float(0):
            prefill_writer.writerow(job_row)
        if print_job.finish_timestamp is not None and print_job.finish_timestamp > float(0):
            finish_job_row.append(12)
            finish_job_row.append(print_job.finish_timestamp)
            finish_job_row.append(print_job.job_id)
            finish_job_row.append(int(print_job.scheduling_class > 0))
            finish_job_row.append(print_job.scheduling_class)
            finish_job_row.append(print_job.task_number)
            all_writer.writerow(finish_job_row)
            if not print_job.submit_timestamp > float(0):
                prefill_writer.writerow(finish_job_row)
                # conversion to byte and multiply by RAM
                # we assume that machines have 1gb ram, we should multiply by some reasonable value (4-8gb)
                # because google does not show the RAM of their machines

    # job distribution traces

    sizes_row.append(cluster_name)
    interarrivals_row.append(cluster_name)
    runtimes_row.append(cluster_name)
    sizes_row.append(assignment_policy)
    interarrivals_row.append(assignment_policy)
    runtimes_row.append(assignment_policy)
    scheduler_id = 0
    if print_job.scheduling_class > 1:
        scheduler_id = 1
    sizes_row.append(scheduler_id)
    interarrivals_row.append(scheduler_id)
    runtimes_row.append(scheduler_id)

    # inter arrival

    if print_job.interarrival_time is not None:
        interarrivals_row.append(print_job.interarrival_time)
        interarrivals_writer.writerow(interarrivals_row)
    if print_job.task_number > int(0):
        sizes_row.append(print_job.task_number)
        sizes_writer.writerow(sizes_row)
    if print_job.schedule_timestamp is not None and print_job.finish_timestamp is not None:
        runtimes_row.append(print_job.finish_timestamp - print_job.schedule_timestamp)
        runtimes_writer.writerow(runtimes_row)

init_state_file.close()
all_state_file.close()
interarrivals_file.close()
sizes_file.close()
runtimes_file.close()