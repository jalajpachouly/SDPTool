package com.phd.issue;


import java.awt.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.util.List;

import com.phd.config.Configuration;
import com.phd.db.DBManager;
import com.phd.ui.ConfigurationPanel;
import com.phd.ui.Home;
import org.kohsuke.github.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;
import javax.swing.*;

public class FetchData  {

    public static void getData() throws Exception {
        disableSSLCertificateChecking();
        Connection conn=null;

            conn = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
           // GitHub github = new GitHubBuilder().withOAuthToken(Configuration.getConfig().getAccessToken()).build();
            GitHub github = new GitHubBuilder().build();
            GHRepository repo = github.getRepository(Configuration.getConfig().getRepoName());

                for (int issueNo = Configuration.getConfig().getRecordFrom(); issueNo <= Configuration.getConfig().getRecordTo(); issueNo++) {
                    try {
                        System.out.println("Processing Id :" + issueNo);
                        processIssue(conn, repo, issueNo);
                    }
                    catch(Exception ex){
                        System.out.println("Not able to fetch the ID :" + issueNo + " Message :"+ ex.getMessage());
                    }
                }
            }


    private static void handleDataException(Exception ex) {
        ex.printStackTrace();
    }

    /**
     * Disables the SSL certificate checking for new instances of {@link HttpsURLConnection} This has been created to
     * aid testing on a local box, not for use on production.
     */
    private static void disableSSLCertificateChecking() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private static void processIssue(Connection conn, GHRepository repo, int issueNo) throws Exception {
        GHIssue issue = repo.getIssue(issueNo);
        DBManager.insertIssue(issue, conn);
        DBManager.insertLabels(issue, conn);
        DBManager.insertComments(issue, conn);
        DBManager.getAndInsertCodeChanges(issue, conn);
    }





    public void start() {
        try {
            SwingWorker sw1 = new SwingWorker()
            {
                @Override
                protected String doInBackground() throws Exception
                {
                    int totalRecordToFetch = Configuration.getConfig().getRecordTo() -Configuration.getConfig().getRecordFrom();
                    int recordProgressCounter = 0;
                    disableSSLCertificateChecking();
                    Connection conn=null;
                        conn = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
                        GitHub github = new GitHubBuilder().withOAuthToken(Configuration.getConfig().getAccessToken()).build();
                       // GitHub github = new GitHubBuilder().build();
                        GHRepository repo = github.getRepository(Configuration.getConfig().getRepoName());
                        for(int issueNo=Configuration.getConfig().getRecordFrom(); issueNo<=Configuration.getConfig().getRecordTo();issueNo++) {
                            try {
                                System.out.println("Processing Id :" + issueNo);
                                processIssue(conn, repo, issueNo);
                            }
                            catch(Exception ex){
                                System.out.println("Not able to fetch the ID :" + issueNo + " Message :"+ ex.getMessage());
                            }
                            recordProgressCounter++;
                            publish(100*recordProgressCounter/totalRecordToFetch);
                        }

                    return null;
                }

                @Override
                protected void process(List chunks)
                {
                    int val = (int) chunks.get(chunks.size()-1);
                    ConfigurationPanel.progressBar.setValue(val);

                }

                @Override
                protected void done()
                {
                    // this method is called when the background
                    // thread finishes execution
                    ConfigurationPanel.progressBar.setValue(100);
                    ConfigurationPanel.createDataSet.setBackground(Color.CYAN);

                }
            };

            // executes the swingworker on worker thread
            sw1.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
