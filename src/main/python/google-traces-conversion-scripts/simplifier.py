# this script removes unnecessary information from google traces csv files to accelerate further processes

import csv

jobs_simplificado_file = open('/Users/dfernandez/google/clusterdata-2011-2/job_events/jobs_simplificado.csv', 'wb')
tasks_simplificado_file = open('/Users/dfernandez/google/clusterdata-2011-2/task_events/task_events_simplificado.csv', 'wb')
jobs_simplificado = csv.writer(jobs_simplificado_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_NONE)
tasks_simplificado = csv.writer(tasks_simplificado_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_NONE)

with open('/Users/dfernandez/google/clusterdata-2011-2/job_events/jobs.csv', 'rb') as csvfile:
    jobsreader = csv.reader(csvfile)
    for row in jobsreader:
        event_type = int(row[3])
        if event_type < 6:
            job_row_simplificado = list()
            job_row_simplificado.append(row[0])
            job_row_simplificado.append(row[2])
            job_row_simplificado.append(row[3])
            job_row_simplificado.append(row[5])
            jobs_simplificado.writerow(job_row_simplificado)


with open('/Users/dfernandez/google/clusterdata-2011-2/task_events/task_events.csv', 'rb') as taskscsvfile:
    tasksreader = csv.reader(taskscsvfile)
    for task_row in tasksreader:
        event_type = int(task_row[5])
        if event_type == 0:
            task_row_simplificado = list()
            task_row_simplificado.append(task_row[2])
            task_row_simplificado.append(task_row[5])
            task_row_simplificado.append(task_row[9])
            task_row_simplificado.append(task_row[10])
            tasks_simplificado.writerow(task_row_simplificado)


jobs_simplificado_file.close()
tasks_simplificado_file.close()