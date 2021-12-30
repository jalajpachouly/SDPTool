package com.phd.config;

public class Configuration {
    boolean useHttps = true;
    boolean validateServeCertificate = false;
    String accessToken;
    String repoName;
    String dbLocation;
    public static  Configuration config;
    int recordFrom;
    int recordTo;



    public  int getRecordFrom() {
        return recordFrom;
    }

    public  void setRecordFrom(int recordFrom) {
        this.recordFrom = recordFrom;
    }

    public  int getRecordTo() {
        return recordTo;
    }

    public  void setRecordTo(int recordTo) {
        this.recordTo = recordTo;
    }


    public static Configuration getConfig() {
        if(Configuration.config==null){
            Configuration.config = new Configuration();
            return Configuration.config;
        }
        else {
            return Configuration.config;
        }
    }

    public static void setConfig(Configuration config) {
        Configuration.config = config;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    public boolean isValidateServeCertificate() {
        return validateServeCertificate;
    }

    public void setValidateServeCertificate(boolean validateServeCertificate) {
        this.validateServeCertificate = validateServeCertificate;
    }



    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getDbLocation() {
        return dbLocation;
    }

    public void setDbLocation(String dbLocation) {
        this.dbLocation = dbLocation;
    }
}
