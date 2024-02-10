package com.phd.domain;

import com.phd.data.CSVHelper;

public class CSVModel {

    String desc;
    String comments;
    String resources;
    String defectType;
    int complexity;
    String codeChanges;
    String tags;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    String body;

    public int getEstimate() {
        return estimate;
    }

    public void setEstimate(int estimate) {
        this.estimate = estimate;
    }

    int estimate;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getDefectType() {
        return defectType;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public String getCodeChanges() {
        return codeChanges;
    }

    public void setCodeChanges(String codeChanges) {
        this.codeChanges = codeChanges;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTagsData() {
        String str = "["
                + "'" + CSVHelper.toStr(this.getDefectType()) + "'" + ","
                + "'" + CSVHelper.toStr(this.getResources()) + "'" + ","
                + "'" + this.getComplexity() + "'" + ","
                + "'" + CSVHelper.toStr(this.getCodeChanges()) + "'" + "]\n";
        char chArr[] = str.toCharArray();
        char prevChar = 'a';
        StringBuilder builder = new StringBuilder();
        for (char ch : chArr) {
            if (prevChar == ' ' && ch== ' ') {
                continue;
            } else {
                builder.append(ch);
                prevChar = ch;
            }
        }
        return builder.toString();
    }

    public String getText() {
        String str = this.getDesc() + "" + this.getComments() + " " + CSVHelper.toStr(this.getBody()) +"\n";
        char chArr[] = str.toCharArray();
        char prevChar = 'a';
        StringBuilder builder = new StringBuilder();
        for (char ch : chArr) {
            if (prevChar == ' ' && ch== ' ') {
                continue;
            } else {
                builder.append(ch);
                prevChar = ch;
            }
        }
        return builder.toString();
    }

}
