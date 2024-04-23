package com.phd.db;

import com.phd.config.Configuration;
import com.phd.data.CSVHelper;
import com.phd.data.validation.ValidateFinalRecord;
import com.phd.domain.CSVModel;
import com.phd.domain.CodeChangeDetails;
import com.phd.domain.CodeChanges;
import com.phd.domain.Comments;
import com.phd.issue.FetchData;
import com.phd.issue.Issue;
import org.kohsuke.github.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

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

    /**
     * Extracts the PR number from an issue that is a pull request.
     * @param issue The issue from which to extract the PR number.
     * @return PR number as a String or null if not a pull request or if number cannot be extracted.
     */
    public static Integer extractPrNumberFromIssue(GHIssue issue) {
        if (issue.isPullRequest()) { // Ensure it's a pull request

                String prUrl = issue.getPullRequest().getUrl().toString();
                Pattern pattern = Pattern.compile(".*/pull/(\\d+)$");
                Matcher matcher = pattern.matcher(prUrl);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1)); // Return the extracted PR number
                }


        }
        return null; // Return null if it's not a pull request or if no number is found
    }

    public static void getAndInsertCodeChanges(GHIssue issue, Connection conn) throws Exception {
        if (issue.getPullRequest() != null && issue.getPullRequest().getDiffUrl() != null) {
           int  prNo = extractPrNumberFromIssue(issue);
           GHPullRequest pr = issue.getRepository().getPullRequest(prNo);
            insertPullRequestsIntoDatabase(pr,conn, issue.getNumber());
            insertReviewComments(pr.listReviewComments().toList(), pr.getNumber(), issue.getNumber(), conn);
            insertReviewers(pr.listReviewComments().toList(), pr.getNumber(), issue.getNumber(), conn);
            insertFilesChanged(pr.listFiles(),pr.getNumber(), issue.getNumber(), conn);
            insertTeams(pr.getRequestedTeams(),pr.getNumber(), issue.getNumber(), conn);
        }
    }
        private static void insertTeams(List<GHTeam> requestedTeams, int pr, int issue_id, Connection conn) {
            // SQL statement to insert team name, PR number, and issue ID
            String sql = "INSERT INTO TEAMS (TEAM_NAME, PR_NO, ISSUE_ID) VALUES (?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (GHTeam team : requestedTeams) {
                    // Get the name of the team from GHTeam object
                    String teamName = team.getName();  // Assuming getName() is the method to get team name

                    // Set the parameters for the PreparedStatement
                    pstmt.setString(1, teamName);
                    pstmt.setInt(2, pr);
                    pstmt.setInt(3, issue_id);

                    // Execute the SQL statement
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("Error occurred while inserting teams: " + e.getMessage());
                e.printStackTrace();
            }
        }

    public static void insertPullRequestsIntoDatabase(GHPullRequest  pr,Connection connection, int issue_id) throws SQLException, Exception {

        String sql = "INSERT INTO PULL_RQ (PR_ID, ISSUE_ID, REPO_ID, USER_ID, TITLE, BODY, STATE, CREATED_AT, UPDATED_AT, CLOSED_AT, MERGED_AT, MERGE_COMMIT_SHA, HEAD_REF, BASE_REF, MERGEABLE, MERGED, COMMENTS_COUNT, REVIEW_COMMENTS_COUNT, COMMITS_COUNT, ADDITIONS, DELETIONS, CHANGED_FILES_COUNT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = connection.prepareStatement(sql);

            statement.setInt(1, pr.getNumber());
            statement.setInt(2, issue_id); // Assuming the Issue ID is linked to the PR ID
            statement.setLong(3, pr.getRepository().getId());
            statement.setLong(4, pr.getUser().getId());
            statement.setString(5, pr.getTitle());
            statement.setString(6, pr.getBody());
            statement.setString(7, pr.getState().toString());
            statement.setTimestamp(8, new java.sql.Timestamp(pr.getCreatedAt().getTime()));
            statement.setTimestamp(9, new java.sql.Timestamp(pr.getUpdatedAt().getTime()));
            statement.setTimestamp(10, pr.getClosedAt() != null ? new java.sql.Timestamp(pr.getClosedAt().getTime()) : null);
            statement.setTimestamp(11, pr.getMergedAt() != null ? new java.sql.Timestamp(pr.getMergedAt().getTime()) : null);
            statement.setString(12, pr.getMergeCommitSha());
            statement.setString(13, pr.getHead().getRef());
            statement.setString(14, pr.getBase().getRef());
            statement.setBoolean(15, pr.isMerged());
            statement.setBoolean(16, pr.isMerged());
            statement.setInt(17, pr.getCommentsCount());
            statement.setInt(18, pr.getReviewComments());
            statement.setInt(19, pr.getCommits());
            statement.setInt(20, pr.getAdditions());
            statement.setInt(21, pr.getDeletions());
            statement.setInt(22, pr.getChangedFiles());

            statement.executeUpdate();

        statement.close();
    }

    private static void insertReviewComments(List<GHPullRequestReviewComment> comments, int prNo, int issueId, Connection conn)  throws Exception {
        String sql = "INSERT INTO pr_review_comments (PR_NO, ISSUE_ID, USER_ID, COMMENT, CREATED_AT) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (GHPullRequestReviewComment comment : comments) {
                statement.setInt(1, prNo);
                statement.setInt(2, issueId);
                statement.setLong(3, comment.getUser().getId());
                statement.setString(4, comment.getBody());
                statement.setTimestamp(5, new Timestamp(comment.getCreatedAt().getTime()));
                statement.executeUpdate();
            }
        }
    }

    private static void insertReviewers(List<GHPullRequestReviewComment> reviewers, int prNo, int issueId, Connection conn)  throws Exception {
        String sql = "INSERT INTO pr_reviewers (PR_NO, ISSUE_ID, USER_ID) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (GHPullRequestReviewComment reviewer : reviewers) {
                statement.setInt(1, prNo);
                statement.setInt(2, issueId);
                statement.setString(3, reviewer.getUser().getLogin());
                statement.executeUpdate();
            }
        }
    }

    private static void insertFilesChanged(PagedIterable<GHPullRequestFileDetail> files, int prNo, int issueId, Connection conn) throws SQLException {
        String sql = "INSERT INTO pr_files_changed (PR_NO, ISSUE_ID, FILE_NAME, STATUS) VALUES (?, ?, ?, ?)";
        try ( PreparedStatement statement = conn.prepareStatement(sql)) {
            for (GHPullRequestFileDetail file : files) {
                statement.setInt(1, prNo);
                statement.setInt(2, issueId);
                statement.setString(3, file.getFilename());
                statement.setString(4, file.getStatus());
                statement.executeUpdate();
            }
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

    public static List<Comments> getListOfComments(int issueId) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<Comments> commentList = new ArrayList<Comments>();
        String query = "SELECT COMMENT,ISSUE_ID,PROCESSED_COMMENTS, ID FROM COMMENTS where ISSUE_ID = " +issueId;
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

    public static String getComments(int issueId) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
       StringBuilder  commentList = new StringBuilder();
        String query = "SELECT PROCESSED_COMMENTS  FROM COMMENTS where ISSUE_ID = " +issueId;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set

            while (rs.next()) {
                commentList.append(rs.getString("PROCESSED_COMMENTS")).append(" ");
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
        return commentList.toString();
    }

    public static List<Issue> getListOfIssues() {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<Issue> issueList = new ArrayList<Issue>();
        String query = "SELECT ISSUE_ID,TITLE,PROCESSED_TITLES, BODY, OPEN_DATE, CLOSE_DATE FROM ISSUE";
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
                issue.setProcessedTitle(rs.getString(("PROCESSED_TITLES")));
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

    public static List<CodeChanges> getCodeChange(int issueId) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<CodeChanges> codeChanges = new ArrayList<CodeChanges>();
        String query = "SELECT ISSUE_ID,CHANGES FROM CODE where ISSUE_ID ="+ issueId;
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

    public static void insertAuthor(Map<Integer, HashMap<String,String>> authorMap) throws SQLException  {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO AUTHORS (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : authorMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name = authorMap.get(id).values().toString();
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


    public static void insertPackageDetails(Map<Integer, HashMap<String,String>>  packageMap) throws SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO PACKAGES (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : packageMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name = packageMap.get(id).values().toString().replaceAll("/", ".").replace("[", "").replace("]","");
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

    public static void insertClassDetails(Map<Integer, HashMap<String,String>> classMap) throws  SQLException {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());

        String sql = "INSERT INTO CLASSES (ISSUE_ID, NAME ) VALUES(?,?)";
        PreparedStatement pstmt = null;

        try {
            for (Map.Entry mapElement : classMap.entrySet()) {
                Integer id = (Integer)mapElement.getKey();
                String name =  classMap.get(id).values().toString().replace("[", "").replace("]","");
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

    public static List<String> getAuthorList(int issueId) {
        {
            Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
            List<String> authorList = new ArrayList<String>();
            String query = "SELECT REPORTER, CLOSED_BY FROM ISSUE where ISSUE_ID = ?";
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = con.prepareStatement(query);
                pstmt.setInt(1, issueId);
                rs = pstmt.executeQuery();

                // loop through the result set

                while (rs.next()) {
                    String reporter = rs.getString("REPORTER");
                    String closed_by = rs.getString("CLOSED_BY");

                    if(reporter!=null){
                        authorList.add(reporter);
                    }
                    if(closed_by!=null && reporter!=null && !reporter.equals(closed_by)){
                        authorList.add(closed_by);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    rs.close();
                    pstmt.close();
                    com.phd.db.Connect.closeConnection(con);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            return authorList;
        }
    }

    public static List<CSVModel> getCSVModel(int count) {
        List<CSVModel> modelList = new ArrayList<CSVModel>();
        List<Issue> issueList = DBManager.getListOfIssuesForExport();
        for(Issue issue : issueList ){
            System.out.println("IssueId :"+ issue.getId());
            List<String> authors = getAuthorList(issue.getId());
            String comments = DBManager.getComments(issue.getId());
            List<String> codeChanges = getListOfClasses(issue.getId());
            int complexity = getIssueComlexity(issue.getId());
            if(complexity ==0){
                continue;
            }
            String issueType = getDefectType(issue.getId());
            CSVModel model = new CSVModel();
            model.setDesc(issue.getProcessedTitle());
            model.setResources(CSVHelper.toString(authors));
            model.setComments(CSVHelper.toStr(comments));
            model.setCodeChanges(CSVHelper.toString(codeChanges));
            model.setComplexity(complexity);
            model.setDefectType(issueType);
            if(issue.getProcessedBody()!=null) {
                model.setBody(CSVHelper.toStr(issue.getProcessedBody()));
            }
            else{
                model.setBody("");
            }
            if(ValidateFinalRecord.validate(model)){
                modelList.add(model);
            }
            else{
                continue;
            }
        }

        // In Progress work
        return modelList;

    }

    public static List<Issue> getListOfIssuesForExport() {
        {
            Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
            List<Issue> issueList = new ArrayList<Issue>();
            String query = "SELECT  ISSUE_ID,TITLE,PROCESSED_TITLES, PROCESSED_BODY, OPEN_DATE, CLOSE_DATE FROM ISSUE ";
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = con.createStatement();
                rs = stmt.executeQuery(query);

                // loop through the result set

                while (rs.next()) {
                    Issue issue = new Issue();
                    issue.setId(rs.getInt("ISSUE_ID"));
                    issue.setProcessedTitle(rs.getString(("PROCESSED_TITLES")));
                    issue.setTitle(rs.getString(("TITLE")));
                    issue.setProcessedBody(rs.getString("PROCESSED_BODY"));
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
    }


    private static List<String> getListOfClasses(Integer id) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<String> classList = new ArrayList<>();
        String query = "SELECT NAME FROM CLASSES where ISSUE_ID = " +id;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set
            while (rs.next()) {
                classList.add(rs.getString("NAME"));
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
        return classList;
    }

    private static int getIssueComlexity(Integer id) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        String query = "SELECT COMPLEXITY FROM CODE_DETAILS where ISSUE_ID = "+id;
        Statement stmt = null;
        ResultSet rs = null;
        int complexity = 0;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set
            while (rs.next()) {
                complexity =rs.getInt("COMPLEXITY");
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
        return complexity;
    }

    private static String getDefectType(Integer id) {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        String query = "select name from label where name like \"%>%\" AND  ISSUE_ID = "+id;
        Statement stmt = null;
        ResultSet rs = null;
        String defectType = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            // loop through the result set
            while (rs.next()) {
                defectType =rs.getString("name");
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
        return defectType;
    }


    public static List<Issue> getNotProcessedIssues() {
        Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
        List<Issue> issueList = new ArrayList<Issue>();
        String query = "SELECT ISSUE_ID,TITLE, BODY, OPEN_DATE, CLOSE_DATE FROM ISSUE where PROCESSED_TITLES is NULL";
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

    public static List<Issue> getIssue(int i) {
        {
            Connection con = com.phd.db.Connect.getConnection(Configuration.getConfig().getDbLocation());
            List<Issue> issueList = new ArrayList<Issue>();
            String query = "SELECT ISSUE_ID,TITLE,PROCESSED_TITLES, BODY, OPEN_DATE, CLOSE_DATE FROM ISSUE where ISSUE_ID = "+i;
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
                    issue.setProcessedTitle(rs.getString(("PROCESSED_TITLES")));
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
    }
}

