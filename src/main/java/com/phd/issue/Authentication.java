package com.phd.issue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class Authentication {


        public static void main(String[] args) {

            // Replace this token with your actual token
            String token = "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";

            String url = "raw.githubusercontent.com/Crunchify/All-in-One-Webmaster/master/readme.txt";

            // HttpClient Method to get Private Github content with Basic OAuth token
            getGithubContentUsingHttpClient(token, url);

            // URLConnection Method to get Private Github content with Basic OAuth token
            getGithubContentUsingURLConnection(token, url);

        }

        @SuppressWarnings("resource")
        public static void getGithubContentUsingHttpClient(String token, String url) {
            String newUrl = "https://" + token + ":x-oauth-basic@" + url;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(newUrl);
            System.out.println(newUrl);
            try {
                HttpResponse response = client.execute(request);
                String responseString = new BasicResponseHandler().handleResponse(response);
                System.out.println(responseString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public  static void getGithubContentUsingURLConnection(String token, String url) {
            String newUrl = "https://" + url;
            System.out.println(newUrl);
            try {
                URL myURL = new URL(newUrl);
                URLConnection connection = myURL.openConnection();
                token = token + ":x-oauth-basic";
                String authString = "Basic " + Base64.encodeBase64String(token.getBytes());
                connection.setRequestProperty("Authorization", authString);
                InputStream crunchifyInStream = connection.getInputStream();
                System.out.println(crunchifyGetStringFromStream(crunchifyInStream));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ConvertStreamToString() Utility - we name it as crunchifyGetStringFromStream()
        public static String crunchifyGetStringFromStream(InputStream crunchifyStream) throws IOException {
            if (crunchifyStream != null) {
                Writer crunchifyWriter = new StringWriter();

                char[] crunchifyBuffer = new char[2048];
                try {
                    Reader crunchifyReader = new BufferedReader(new InputStreamReader(crunchifyStream, "UTF-8"));
                    int counter;
                    while ((counter = crunchifyReader.read(crunchifyBuffer)) != -1) {
                        crunchifyWriter.write(crunchifyBuffer, 0, counter);
                    }
                } finally {
                    crunchifyStream.close();
                }
                return crunchifyWriter.toString();
            } else {
                return "No Contents";
            }
        }
    }


