package com.phd;

public class TestConstants {
    public static final String  classData =  "diff --git a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "index fd7e3e718f52d..989ecc64b6197 100644\n" +
            "--- a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "+++ b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsEc2Service.java\n" +
            "@@ -68,6 +68,18 @@ public synchronized AmazonEC2 client() {\n" +
            "             throw new ElasticSearchIllegalArgumentException(\"No s3 secret_key defined for s3 gateway\");\n" +
            "         }\n" +
            " \n" +
            "+        String proxyHost = settings.get(\"network.proxyHost\");\n" +
            "+        if (proxyHost != null) {\n" +
            "+            String portString = settings.get(\"network.proxyPort\",\"80\");\n" +
            "+            Integer proxyPort;\n" +
            "+            try {\n" +
            "+                proxyPort = Integer.parseInt(portString,10);\n" +
            "+            } catch (NumberFormatException ex) {\n" +
            "+                throw new ElasticSearchIllegalArgumentException(\"The configured proxy port value [\" + portString + \"] is invalid\",ex);\n" +
            "+            }\n" +
            "+            clientConfiguration.withProxyHost(proxyHost).setProxyPort(proxyPort);\n" +
            "+        }\n" +
            "+\n" +
            "         this.client = new AmazonEC2Client(new BasicAWSCredentials(account, key), clientConfiguration);\n" +
            " \n" +
            "         if (componentSettings.get(\"ec2.endpoint\") != null) {\n" +
            "diff --git a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "index 2796456d59705..28539e1be7ddf 100644\n" +
            "--- a/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "+++ b/plugins/cloud/aws/src/main/java/org/elasticsearch/cloud/aws/AwsS3Service.java\n" +
            "@@ -68,6 +68,18 @@ public synchronized AmazonS3 client() {\n" +
            "             throw new ElasticSearchIllegalArgumentException(\"No s3 secret_key defined for s3 gateway\");\n" +
            "         }\n" +
            " \n" +
            "+        String proxyHost = settings.get(\"network.proxyHost\");\n" +
            "+        if (proxyHost != null) {\n" +
            "+            String portString = settings.get(\"network.proxyPort\",\"80\");\n" +
            "+            Integer proxyPort;\n" +
            "+            try {\n" +
            "+                proxyPort = Integer.parseInt(portString,10);\n" +
            "+            } catch (NumberFormatException ex) {\n" +
            "+                throw new ElasticSearchIllegalArgumentException(\"The configured proxy port value [\" + portString + \"] is invalid\",ex);\n" +
            "+            }\n" +
            "+            clientConfiguration.withProxyHost(proxyHost).setProxyPort(proxyPort);\n" +
            "+        }\n" +
            "+\n" +
            "         this.client = new AmazonS3Client(new BasicAWSCredentials(account, key), clientConfiguration);\n" +
            " \n" +
            "         if (componentSettings.get(\"s3.endpoint\") != null) {\n";
}
