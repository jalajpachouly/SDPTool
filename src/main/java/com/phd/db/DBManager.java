package com.phd.db;

import com.phd.config.Configuration;
import com.phd.domain.CodeChangeDetails;
import com.phd.domain.CodeChanges;
import com.phd.domain.Comments;
import com.phd.issue.FetchData;
import com.phd.issue.Issue;
import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class DBManager {

    public static void fetchData() {
        try {
            FetchData.getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertIssue(GHIssue issue, Connection conn) {
        String sql = "INSERT INTO ISSUE(TITLE,REPORTER,OPEN_DATE, CLOSE_DATE,BODY,ISSUE_ID,CLOSED_BY) VALUES(?,?,?,?,?,?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if (issue.getTitle() != null) {
                pstmt.setString(1, issue.getTitle());
            }
            if (issue.getUser() != null) {
                pstmt.setString(2, issue.getUser().getLogin());
            }
            if (issue.getCreatedAt() != null) {
                pstmt.setString(3, issue.getCreatedAt().toString());
            }
            if (issue.getClosedAt() != null) {
                pstmt.setString(4, issue.getClosedAt().toString());
            }
            if (issue.getBody() != null) {
                pstmt.setString(5, issue.getBody());
            }
            pstmt.setInt(6, issue.getNumber());

            if (issue.getClosedBy() != null) {
                pstmt.setString(7, issue.getClosedBy().getLogin());
            }
            pstmt.executeUpdate();
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void insertLabels(GHIssue issue, Connection conn) {
        String sql = "INSERT INTO LABEL(ISSUE_ID, NAME, COLOR) VALUES(?,?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            Collection<GHLabel> labels = issue.getLabels();
            for (GHLabel label : labels) {
                pstmt.setInt(1, issue.getNumber());
                pstmt.setString(2, label.getName());
                pstmt.setString(3, label.getColor());
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public static void insertComments(GHIssue issue, Connection conn) {

        String sql = "INSERT INTO COMMENTS(ISSUE_ID, COMMENT) VALUES(?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            List<GHIssueComment> comments = issue.getComments();
            for (GHIssueComment comment : comments) {
                pstmt.setInt(1, issue.getNumber());
                pstmt.setString(2, comment.getBody());
                pstmt.executeUpdate();
            }

        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void insertCodeChanges(GHIssue issue, Connection conn, String codeChange) {

        String sql = "INSERT INTO CODE(ISSUE_ID, CHANGES) VALUES(?,?)";
        //Get Author who has written or updated this code

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, issue.getNumber());
            pstmt.setString(2, codeChange);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void getAndInsertCodeChanges(GHIssue issue, Connection conn) throws Exception {
        if (issue.getPullRequest() != null && issue.getPullRequest().getDiffUrl() != null) {
            StringBuffer response = new StringBuffer();
            URL url = issue.getPullRequest().getDiffUrl();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Accept", "application/json");
            // http.setRequestProperty("Authorization", "Bearer " + "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");
            http.setRequestProperty("Authorization", "Bearer " + Configuration.getConfig().getAccessToken());

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(http.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                response.append(System.lineSeparator());
            }
            in.close();
            insertCodeChanges(issue, conn, response.toString());
        }


    }

    public static void saveConfiguration(Configuration config) {
        Connection con = null;
        try {
            Configuration prevConfig = loadConfiguration(config);
            con = com.phd.db.Connect.getConnection(config.getDbLocation());
            if (prevConfig != null) {
                updateConfig(config, con);
            } else {
                insertConfig(config, con);
            }
            Configuration.setConfig(config);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            Connect.closeConnection(con);
        }

    }

    private static void insertConfig(Configuration config, Connection con) throws SQLException {
        {
            String sql = "INSERT INTO CONFIGURATION (DBLOC, USE_SSL, IGNORE_CERT, ACCESS_TOKEN, REPONAME,RECORD_FROM, RECORD_TO ) VALUES(?,?,?,?,?,?,?)";
            PreparedStatement pstmt = null;

            try {
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, config.getDbLocation());
                pstmt.setBoolean(2, config.isUseHttps());
                pstmt.setBoolean(3, config.isValidateServeCertificate());
                pstmt.setString(4, config.getAccessToken());
                pstmt.setString(5, config.getRepoName());
                pstmt.setInt(6, config.getRecordFrom());
                pstmt.setInt(7, config.getRecordTo());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } finally {
                pstmt.close();
            }
        }
    }

    private static void updateConfig(Configuration config, Connection con) throws SQLException {
        String sql = "UPDATE CONFIGURATION SET DBLOC=?, USE_SSL=?, IGNORE_CERT=?, ACCESS_TOKEN=?, REPONAME=?, RECORD_FROM=?, RECORD_TO=? where DBLOC is not NULL";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, config.getDbLocation());
            pstmt.setBoolean(2, config.isUseHttps());
            pstmt.setBoolean(3, config.isValidateServeCertificate());
            pstmt.setString(4, config.getAccessToken());
            pstmt.setString(5, config.getRepoName());
            pstmt.setInt(6, config.getRecordFrom());
            pstmt.setInt(7, config.getRecordTo());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }
    }

    public static Configuration loadConfiguration(Configuration config) {
        Connection con = com.phd.db.Connect.getConnection(config.getDbLocation());
        String query = "SELECT DBLOC,USE_SSL,IGNORE_CERT,ACCESS_TOKEN,REPONAME, RECORD_FROM, RECORD_TO FROM CONFIGURATION";
        Configuration prevConfig = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set

            while (rs.next()) {
                prevConfig = new Configuration();
                prevConfig.setDbLocation(rs.getString("DBLOC"));
                prevConfig.setUseHttps(rs.getBoolean("USE_SSL"));
                prevConfig.setValidateServeCertificate(rs.getBoolean("IGNORE_CERT"));
                prevConfig.setAccessToken(rs.getString("ACCESS_TOKEN"));
                prevConfig.setRepoName(rs.getString("REPONAME"));
                prevConfig.setRecordFrom(rs.getInt("RECORD_FROM"));
                prevConfig.setRecordTo(rs.getInt("RECORD_TO"));
                if (prevConfig == null) {
                    return null;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
                stmt.close();
                com.phd.db.Connect.closeConnection(con);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return prevConfig;
    }

    public static List<Comments> getListOfComments() {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<Comments> commentList = new ArrayList<Comments>();
        String query = "SELECT COMMENT,ISSUE_ID,PROCESSED_COMMENTS, ID FROM COMMENTS";
        Statement stmt = null;
        ResultSet rs = null;
        System.out.println(query);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set

            while (rs.next()) {
                Comments comments = new Comments();
                comments.setComment(rs.getString("COMMENT"));
                comments.setIssueId(rs.getInt(("ISSUE_ID")));
                comments.setProcessedComments(rs.getString("PROCESSED_COMMENTS"));
                comments.setId(rs.getInt(("ID")));
                commentList.add(comments);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
                stmt.close();
                com.phd.db.Connect.closeConnection(con);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return commentList;
    }

    public static List<Issue> getListOfIssues() {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<Issue> issueList = new ArrayList<Issue>();
        String query = "SELECT ISSUE_ID,TITLE, BODY, OPEN_DATE, CLOSE_DATE FROM ISSUE";
        Statement stmt = null;
        ResultSet rs = null;
        System.out.println(query);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set

            while (rs.next()) {
                Issue issue = new Issue();
                issue.setId(rs.getInt("ISSUE_ID"));
                issue.setTitle(rs.getString(("TITLE")));
                issue.setBody(rs.getString("BODY"));
                issue.setCreatedAt(rs.getString("OPEN_DATE"));
                issue.setClosedAt(rs.getString("CLOSE_DATE"));
                issueList.add(issue);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
                stmt.close();
                com.phd.db.Connect.closeConnection(con);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return issueList;
    }

    public static void updateComment(Comments comments) throws SQLException {
            Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
            String sql = "UPDATE Comments SET PROCESSED_COMMENTS=? where ID=?";
            PreparedStatement pstmt = null;
            try {
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, comments.getProcessedComments());
                pstmt.setInt(2, comments.getId());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } finally {
                pstmt.close();
            }
        }

    public static void updateIssue(Issue issue) throws SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        String sql = "UPDATE ISSUE SET PROCESSED_TITLES=?, PROCESSED_BODY=?, TIME_TAKEN=? where ISSUE_ID=?";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, issue.getProcessedTitle());
            pstmt.setString(2, issue.getProcessedBody());
            pstmt.setLong(3, issue.getTimeTakenToFix());
            pstmt.setInt(4, issue.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }
    }

    public static List<CodeChanges> getListOfCodeChanges() {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<CodeChanges> codeChanges = new ArrayList<CodeChanges>();
        String query = "SELECT ISSUE_ID,CHANGES FROM CODE";
        Statement stmt = null;
        ResultSet rs = null;
        System.out.println(query);
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set

            while (rs.next()) {
                CodeChanges codeChange = new CodeChanges();
                codeChange.setCodeChange(rs.getString("CHANGES"));
                codeChange.setIssueId(rs.getInt(("ISSUE_ID")));
                codeChanges.add(codeChange);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
                stmt.close();
                com.phd.db.Connect.closeConnection(con);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return codeChanges;
    }

    public static void insertAuthor(Map<Integer, String> authorMap) throws SQLException  {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO AUTHORS (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : authorMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name = (String) authorMap.get(id);
                pstmt = con.prepareStatement(sql);
                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.executeUpdate();

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }
    }


    public static void insertPackageDetails(Map<Integer, String> packageMap) throws SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO PACKAGES (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : packageMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name = (String) packageMap.get(id);
                pstmt = con.prepareStatement(sql);
                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.executeUpdate();

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }
    }

    public static void insertCodeChangeDetails(CodeChangeDetails codeDetails) throws SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        String sql = "INSERT INTO CODE_DETAILS (ISSUE_ID, NO_OF_CLASSES, NO_OF_LINES, NO_OF_AUTHORS, COMPLEXITY ) VALUES(?,?,?,?,?)";
        PreparedStatement pstmt = null;
        try {
            Integer id = codeDetails.getIssueId();
            Integer noOfClasses = codeDetails.getNoOfClass();
            Integer noOfLines = codeDetails.getNoOfLines();
            Integer noOfAuthors = codeDetails.getNoOfAuthors();
            Integer complexity = codeDetails.getChangeComplexity();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.setInt(2, noOfClasses);
            pstmt.setInt(3, noOfLines);
            pstmt.setInt(4, noOfAuthors);
            pstmt.setInt(5, complexity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }

    }

    public static void insertClassDetails(Map<Integer, String> classMap) throws  SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO CLASSES (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : classMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name = (String) classMap.get(id);
                pstmt = con.prepareStatement(sql);
                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.executeUpdate();

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            pstmt.close();
        }
    }
}

