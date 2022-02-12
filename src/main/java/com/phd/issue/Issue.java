package com.phd.issue;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Issue {

    private String url;
    private String repositoryUrl;
    private String labelsUrl;
    private String commentsUrl;
    private String eventsUrl;
    private String htmlUrl;
    private Integer id;
    private String nodeId;
    private Integer number;
    private String title;
    private User user;
    private List<Label> labels = null;
    private String state;
    private Boolean locked;
    private Object assignee;
    private List<Object> assignees = null;
    private Object milestone;
    private Integer comments;
    private String createdAt;
    private String updatedAt;
    private String closedAt;
    private String authorAssociation;
    private Object activeLockReason;
    private String processedBody;
    private String body;
    private Object closedBy;
    private Object performedViaGithubApp;
    private long timeTakenToFix;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public long getTimeTakenToFix() {
        return timeTakenToFix;
    }

    public void setTimeTakenToFix(long timeTakenToFix) {
        this.timeTakenToFix = timeTakenToFix;
    }



    public String getProcessedTitle() {
        return processedTitle;
    }

    public void setProcessedTitle(String processedTitle) {
        this.processedTitle = processedTitle;
    }

    private String processedTitle;

    public String getProcessedBody() {
        return processedBody;
    }

    public void setProcessedBody(String processedBody) {
        this.processedBody = processedBody;
    }



    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }


    public String getLabelsUrl() {
        return labelsUrl;
    }


    public void setLabelsUrl(String labelsUrl) {
        this.labelsUrl = labelsUrl;
    }


    public String getCommentsUrl() {
        return commentsUrl;
    }

    public void setCommentsUrl(String commentsUrl) {
        this.commentsUrl = commentsUrl;
    }

    public String getEventsUrl() {
        return eventsUrl;
    }


    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }


    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }


    public Integer getNumber() {
        return number;
    }


    public void setNumber(Integer number) {
        this.number = number;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }


    public String getState() {
        return state;
    }


    public void setState(String state) {
        this.state = state;
    }


    public Boolean getLocked() {
        return locked;
    }


    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Object getAssignee() {
        return assignee;
    }


    public void setAssignee(Object assignee) {
        this.assignee = assignee;
    }


    public List<Object> getAssignees() {
        return assignees;
    }


    public void setAssignees(List<Object> assignees) {
        this.assignees = assignees;
    }

    public Object getMilestone() {
        return milestone;
    }


    public void setMilestone(Object milestone) {
        this.milestone = milestone;
    }


    public Integer getComments() {
        return comments;
    }


    public void setComments(Integer comments) {
        this.comments = comments;
    }


    public String getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }


    public String getUpdatedAt() {
        return updatedAt;
    }


    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }


    public String getClosedAt() {
        return closedAt;
    }


    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }


    public String getAuthorAssociation() {
        return authorAssociation;
    }


    public void setAuthorAssociation(String authorAssociation) {
        this.authorAssociation = authorAssociation;
    }


    public Object getActiveLockReason() {
        return activeLockReason;
    }


    public void setActiveLockReason(Object activeLockReason) {
        this.activeLockReason = activeLockReason;
    }


    public String getBody() {
        return body;
    }


    public void setBody(String body) {
        this.body = body;
    }


    public Object getClosedBy() {
        return closedBy;
    }


    public void setClosedBy(Object closedBy) {
        this.closedBy = closedBy;
    }


    public Object getPerformedViaGithubApp() {
        return performedViaGithubApp;
    }


    public void setPerformedViaGithubApp(Object performedViaGithubApp) {
        this.performedViaGithubApp = performedViaGithubApp;
    }


    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }


    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

