package org.ihtsdo.snowowl.authoring.scheduler.api.service;

import java.util.List;

import org.snomed.otf.scheduler.domain.*;
import org.springframework.stereotype.Service;

@Service
public interface ScheduleService {

	public List<JobType> listJobTypes();

	public List<JobCategory> listJobTypeCategories(String typeName);

	public Job getJob(String typeName, String jobName);
	
	public List<JobRun> listJobsRun(String typeName, String jobName, String user);

	public JobRun runJob(String jobType, String jobName, JobRun jobRun);

	public JobSchedule scheduleJob(String jobType, String jobName, JobSchedule jobSchedule);

	public JobSchedule deleteSchedule(String jobType, String jobName, String scheduleId);

	public JobRun getJobStatus(String typeName, String jobName, String runId);

}
