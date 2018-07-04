package org.ihtsdo.snowowl.authoring.scheduler.api.service;

import java.util.*;
import java.util.stream.Collectors;

import org.snomed.otf.scheduler.domain.*;

public class ScheduleServiceStub implements ScheduleService {
	
	Map<String, JobType> jobTypes = new HashMap<>();
	Map<Job, JobRun> jobRuns = new HashMap<>();
	
	public ScheduleServiceStub() {
		createDummyData();
	}

	@Override
	public List<JobType> listJobTypes() {
		return jobTypes.values().stream().collect(Collectors.toList()); 
	}

	@Override
	public List<JobCategory> listJobTypeCategories(String typeName) {
		if (jobTypes.containsKey(typeName)) {
			return jobTypes.get(typeName).getCategories();
		}
		return null;
	}

	@Override
	public Job getJob(String typeName, String jobName) {
		for (JobCategory category : listJobTypeCategories(typeName)) {
			for (Job job : category.getJobs()) {
				if (job.getName().equals(jobName)) {
					return job;
				}
			}
		}
		return null;
	}

	@Override
	public List<JobRun> listJobsRun(String typeName, String jobName, String user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobRun runJob(String jobType, String jobName, JobRun jobRun) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobSchedule scheduleJob(String jobType, String jobName, JobSchedule jobSchedule) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobSchedule deleteSchedule(String jobType, String jobName, String scheduleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobRun getJobStatus(String typeName, String jobName, String runId) {
		// TODO Auto-generated method stub
		return null;
	}

	private void createDummyData() {
		createJobs();
		createRuns();
	}
	
	private void createJobs() {
		String[] params = new String[] { "subHierarchy", "project" };
		Job csReport = new Job ("Case Sensitivity", "Produces a list of...", params);
		JobCategory qaReports = new JobCategory("QA");
		qaReports.addJob(csReport);
		JobType reports = new JobType("Report");
		reports.addCategory(qaReports);
		jobTypes.put(reports.getName(), reports);
	}
	
	private void createRuns() {
	}
}
