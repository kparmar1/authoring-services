package org.ihtsdo.snowowl.authoring.scheduler.api.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.snomed.otf.scheduler.domain.*;

public class ScheduleServiceStub implements ScheduleService {
	
	private static final String TYPE_REPORT = "Report";
	private static final String JOB_CS = "Case Sensitivity";
	private URL resultURL;
	
	Map<String, JobType> jobTypes = new HashMap<>();
	Map<Job, List<JobRun>> jobRuns = new HashMap<>();
	
	public ScheduleServiceStub() throws MalformedURLException {
		resultURL = new URL ("https://docs.google.com/spreadsheets/d/1kLPbzaF05H0sg22wPL8NHp1mfd5MlUW7Uqfh-87wSDY/edit#gid=0");
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
		Job job = getJob(typeName, jobName);
		//Are we filtering?
		if (user == null) {
			return jobRuns.get(job);
		} 
		
		List<JobRun> filteredRuns = new ArrayList<>();
		for (JobRun run : jobRuns.get(job)) {
			if (run.getUser().equals(user)) {
				filteredRuns.add(run);
			}
		}
		return filteredRuns;
	}

	@Override
	public JobRun runJob(String jobType, String jobName, JobRun jobRun) throws BusinessServiceException {
		//Make sure we know what this job is before we run it!
		Job job = getJob(jobType, jobName);
		if (job == null) {
			throw new BusinessServiceException("Unknown job : " + jobType + "/" + jobName);
		}
		jobRun.setId(UUID.randomUUID());
		jobRun.setRequestTime(new Date());
		jobRun.setStatus(JobStatus.Scheduled);
		List<JobRun> runs = jobRuns.get(job);
		if (runs == null) {
			runs = new ArrayList<>();
			jobRuns.put(job, runs);
		}
		runs.add(jobRun);
		return jobRun;
	}

	@Override
	public JobSchedule scheduleJob(String jobType, String jobName, JobSchedule jobSchedule) {
		jobSchedule.setId(UUID.randomUUID());
		return jobSchedule;
	}

	@Override
	public void deleteSchedule(String jobType, String jobName, String scheduleId) {
		return;
	}

	@Override
	public JobRun getJobRun(String typeName, String jobName, UUID runId) {
		for (JobRun run : listJobsRun(typeName, jobName, null)) {
			if (run.getId().equals(runId)) {
				//Complete all running jobs
				run.setResult(resultURL);
				run.setStatus(JobStatus.Complete);
				return run;
			}
		}
		return null;
	}

	
	/***********************  DUMMY DATA *************************/

	private void createDummyData() {
		try {
			createJobs();
			createRuns();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to set up schedule service dummy data", e);
		}
	}
	
	private void createJobs() {
		String[] params = new String[] { "subHierarchy", "project" };
		Job csReport = new Job ("Case Sensitivity", "Produces a list of...", params);
		JobCategory qaReports = new JobCategory("QA");
		qaReports.addJob(csReport);
		JobType reports = new JobType(TYPE_REPORT);
		reports.addCategory(qaReports);
		jobTypes.put(reports.getName(), reports);
	}
	
	private void createRuns() throws MalformedURLException  {
		
		JobRun scheduledJob = JobRun.create(JOB_CS, "system");
		scheduledJob.setStatus(JobStatus.Scheduled);
		
		JobRun completeJob = JobRun.create(JOB_CS, "system");
		completeJob.setStatus(JobStatus.Complete);
		completeJob.setResult(resultURL);
	
		List <JobRun> csRuns = new ArrayList<>();
		csRuns.add(scheduledJob);
		csRuns.add(completeJob);
		
		Job csJob = getJob(TYPE_REPORT, JOB_CS);
		jobRuns.put(csJob, csRuns);
	}
}
