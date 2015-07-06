package org.ihtsdo.snowowl.authoring.single.api.service;

import com.b2international.snowowl.core.exceptions.NotFoundException;

import net.rcarz.jiraclient.*;

import org.ihtsdo.otf.im.utility.SecurityService;
import org.ihtsdo.snowowl.authoring.single.api.pojo.AuthoringProject;
import org.ihtsdo.snowowl.authoring.single.api.pojo.AuthoringTask;
import org.ihtsdo.snowowl.authoring.single.api.pojo.AuthoringTaskCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class TaskService {

	@Autowired
	private BranchService branchService;
	
	@Autowired
	SecurityService ims;
	
	private final JiraClient jiraClient;
	private static final String AUTHORING_TASK_TYPE = "SCA Authoring Task";

	public TaskService(JiraClient jiraClient) {
		this.jiraClient = jiraClient;
	}

	public List<AuthoringProject> listProjects() throws JiraException {
		List<AuthoringProject> authoringProjects = new ArrayList<>();
		for (Issue issue : jiraClient.searchIssues("type = \"SCA Authoring Project\"").issues) {
			Project project = issue.getProject();
			authoringProjects.add(new AuthoringProject(project.getKey(), project.getName()));
		}
		return authoringProjects;
	}

	public List<AuthoringTask> listTasks(String projectKey) throws JiraException {
		getProjectOrThrow(projectKey);
		List<Issue> issues = jiraClient.searchIssues(getProjectJQL(projectKey)).issues;
		return convertToAuthoringTasks(issues);
	}

	public AuthoringTask retrieveTask(String projectKey, String taskKey) throws JiraException {
		getProjectOrThrow(projectKey);
		List<Issue> issues = jiraClient.searchIssues(getProjectJQL(projectKey) + " AND key = " + taskKey).issues;
		if (!issues.isEmpty()) {
			return new AuthoringTask(issues.get(0));
		} else {
			throw new NotFoundException("Task", taskKey);
		}
	}

	public List<AuthoringTask> listMyTasks(String username) throws JiraException {
		List<Issue> issues = jiraClient.searchIssues("assignee = \"" + username + "\" AND type = \"" + AUTHORING_TASK_TYPE + "\"").issues;
		return convertToAuthoringTasks(issues);
	}

	private String getProjectJQL(String projectKey) {
		return "project = " + projectKey + " AND type = \"" + AUTHORING_TASK_TYPE + "\"";
	}

	public AuthoringTask createTask(String projectKey, AuthoringTaskCreateRequest taskCreateRequest) throws JiraException, ServiceException {
		//The task should be assigned to the currently logged in user
		String currentUser = ims.getCurrentLogin();
		
		Issue jiraIssue = jiraClient.createIssue(projectKey, AUTHORING_TASK_TYPE)
				.field(Field.SUMMARY, taskCreateRequest.getSummary())
				.field(Field.DESCRIPTION, taskCreateRequest.getDescription())
				.field(Field.ASSIGNEE, currentUser)
				.execute();

		AuthoringTask authoringTask = new AuthoringTask(jiraIssue);
		branchService.createTaskBranchAndProjectBranchIfNeeded(authoringTask.getProjectKey(), authoringTask.getKey());
		return authoringTask;
	}

	private List<AuthoringTask> convertToAuthoringTasks(List<Issue> issues) {
		List<AuthoringTask> tasks = new ArrayList<>();
		for (Issue issue : issues) {
			tasks.add(new AuthoringTask(issue));
		}
		return tasks;
	}

	private void getProjectOrThrow(String projectKey) {
		try {
			jiraClient.getProject(projectKey);
		} catch (JiraException e) {
			throw new NotFoundException("Project", projectKey);
		}
	}
}