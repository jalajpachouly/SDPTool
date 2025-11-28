package com.phd.domain;

public class Comments {

    int issueId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    int id;
    String comment;
    String processedComments;


    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public String getComment() {
        return comment.replaceAll("\r", " ").replaceAll("\n"," ");
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getProcessedComments() {
        return processedComments;
    }

    public void setProcessedComments(String processedComments) {
        this.processedComments = processedComments;
    }




}
