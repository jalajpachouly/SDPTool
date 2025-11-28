package com.phd.domain;

public class CodeChangeDetails {
    int issueId;
    int noOfClass;
    int noOfAuthors;
    int noOfLines;
    int noOfPackages;
    int changeComplexity;

    public int getChangeComplexity() {
        return changeComplexity;
    }

    public void setChangeComplexity(int changeComplexity) {
        this.changeComplexity = changeComplexity;
    }


    public int getNoOfPackages() {
        return noOfPackages;
    }

    public void setNoOfPackages(int noOfPackages) {
        this.noOfPackages = noOfPackages;
    }

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getNoOfClass() {
        return noOfClass;
    }

    public void setNoOfClass(int noOfClass) {
        this.noOfClass = noOfClass;
    }

    public int getNoOfAuthors() {
        return noOfAuthors;
    }

    public void setNoOfAuthors(int noOfAuthors) {
        this.noOfAuthors = noOfAuthors;
    }

    public int getNoOfLines() {
        return noOfLines;
    }

    public void setNoOfLines(int noOfLines) {
        this.noOfLines = noOfLines;
    }
}
