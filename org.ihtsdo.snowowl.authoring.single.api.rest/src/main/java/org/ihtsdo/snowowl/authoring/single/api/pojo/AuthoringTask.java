package org.ihtsdo.snowowl.authoring.single.api.pojo;

import com.fasterxml.jackson.annotation.JsonRawValue;

import net.rcarz.jiraclient.Issue;
import net.sf.json.JSONObject;

public class AuthoringTask implements AuthoringTaskCreateRequest, AuthoringTaskUpdateRequest {

	public static String JIRA_REVIEWER_FIELD;
	public static void setJiraReviewerField(String jiraReviewerField) {
		JIRA_REVIEWER_FIELD = jiraReviewerField;
	}

	public static final String JIRA_CREATED_FIELD = "created";
	public static final String JIRA_UPDATED_FIELD = "updated";

	private String key;
	private String projectKey;
	private String summary;
	private String status;
	private String description;
	private User assignee;
	private User reviewer;
	private String created;
	private String updated;
	private String latestClassificationJson;
	private String latestValidationStatus;
	private boolean unreadFeedbackMessages;

	public AuthoringTask() {
	}

	public AuthoringTask(Issue issue) {
		key = issue.getKey();
		projectKey = issue.getProject().getKey();
		summary = issue.getSummary();
		status = issue.getStatus().getName();
		description = issue.getDescription();
		net.rcarz.jiraclient.User assignee = issue.getAssignee();
		if (assignee != null) {
			this.assignee = new User(assignee);
		}
		created = (String) issue.getField(JIRA_CREATED_FIELD);
		updated = (String) issue.getField(JIRA_UPDATED_FIELD);
		
		Object reviewerObj = issue.getField(JIRA_REVIEWER_FIELD);
		if (reviewerObj != null && reviewerObj instanceof JSONObject) {
			reviewer = new User((JSONObject)reviewerObj);
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	@Override
	public String getSummary() {
		return summary;
	}

	@Override
	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	public User getAssignee() {
		return assignee;
	}

	public void setAssignee(User assignee) {
		this.assignee = assignee;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}

	@JsonRawValue
	public String getLatestClassificationJson() {
		return latestClassificationJson;
	}
	
	public void setLatestClassificationJson(String json) {
		latestClassificationJson = json;
	}

	public void setLatestValidationStatus(String latestValidationStatus) {
		this.latestValidationStatus = latestValidationStatus;
	}

	public String getLatestValidationStatus() {
		return latestValidationStatus;
	}

	public User getReviewer() {
		return reviewer;
	}

	public void setReviewer(User reviewer) {
		this.reviewer = reviewer;
	}

	public void setUnreadFeedbackMessages(boolean unreadFeedbackMessages) {
		this.unreadFeedbackMessages = unreadFeedbackMessages;
	}

	public boolean isUnreadFeedbackMessages() {
		return unreadFeedbackMessages;
	}
}
