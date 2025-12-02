package com.phd.db;

import com.phd.config.DatabaseProperties;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class Connect {
    /**
     * Connect to a sample database
     * @param dbLocation
     */
    public static Connection getConnection(String dbLocation) {
        Connection conn = null;
        try {
            // Use provided dbLocation or fallback to properties file default
            if (dbLocation == null || dbLocation.trim().isEmpty()) {
                dbLocation = DatabaseProperties.getDatabaseLocation();
                System.out.println("No database location provided, using property file default: " + dbLocation);
            }
            
            // Ensure the database directory exists
            File dbFile = new File(dbLocation);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    System.out.println("Created database directory: " + parentDir.getAbsolutePath());
                } else {
                    throw new SQLException("Failed to create database directory: " + parentDir.getAbsolutePath());
                }
            }
            
            String url = "jdbc:sqlite:" + dbLocation;
            System.out.println("Connecting to database: " + url);
            
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            
            // Initialize database schema if tables don't exist
            if (DatabaseProperties.isAutoInitSchema()) {
                initializeDatabaseSchema(conn);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database connection error: " + e.getMessage());
        }
        return conn;
    }

    public static void closeConnection(Connection conn) {

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    /**
     * Initialize database schema by creating tables if they don't exist
     */
    private static void initializeDatabaseSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Check if ISSUE table exists
            ResultSet rs = conn.getMetaData().getTables(null, null, "ISSUE", null);
            if (!rs.next()) {
                System.out.println("Database schema not found. Creating tables...");
                
                // Create all required tables
                String[] createTableStatements = {
                    "CREATE TABLE IF NOT EXISTS \"AUTHORS\" (" +
                    "\"ID\" INTEGER, " +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"NAME\" TEXT, " +
                    "PRIMARY KEY(\"ID\" AUTOINCREMENT)" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"CLASSES\" (" +
                    "\"ID\" INTEGER, " +
                    "\"NAME\" TEXT, " +
                    "\"ISSUE_ID\" INTEGER, " +
                    "PRIMARY KEY(\"ID\" AUTOINCREMENT)" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"CODE\" (" +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"CHANGES\" TEXT, " +
                    "FOREIGN KEY(\"ISSUE_ID\") REFERENCES \"ISSUE\"(\"ISSUE_ID\")" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"CODE_DETAILS\" (" +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"NO_OF_CLASSES\" INTEGER, " +
                    "\"NO_OF_LINES\" INTEGER, " +
                    "\"NO_OF_AUTHORS\" INTEGER, " +
                    "\"COMPLEXITY\" INTEGER" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"COMMENTS\" (" +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"COMMENT\" TEXT, " +
                    "\"PROCESSED_COMMENTS\" TEXT, " +
                    "\"ID\" INTEGER, " +
                    "FOREIGN KEY(\"ISSUE_ID\") REFERENCES \"ISSUE\"(\"ISSUE_ID\"), " +
                    "PRIMARY KEY(\"ID\")" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"CONFIGURATION\" (" +
                    "\"DBLOC\" TEXT, " +
                    "\"USE_SSL\" TEXT, " +
                    "\"IGNORE_CERT\" TEXT, " +
                    "\"ACCESS_TOKEN\" TEXT, " +
                    "\"REPONAME\" TEXT, " +
                    "\"RECORD_FROM\" INTEGER, " +
                    "\"RECORD_TO\" INTEGER" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"ISSUE\" (" +
                    "\"TITLE\" TEXT, " +
                    "\"REPORTER\" TEXT, " +
                    "\"OPEN_DATE\" TEXT, " +
                    "\"CLOSE_DATE\" TEXT, " +
                    "\"BODY\" TEXT, " +
                    "\"ISSUE_ID\" INTEGER NOT NULL, " +
                    "\"CLOSED_BY\" TEXT, " +
                    "\"TIME_TAKEN\" NUMERIC, " +
                    "\"PROCESSED_TITLES\" TEXT COLLATE BINARY, " +
                    "\"PROCESSED_BODY\" TEXT, " +
                    "PRIMARY KEY(\"ISSUE_ID\")" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"LABEL\" (" +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"NAME\" TEXT, " +
                    "\"COLOR\" TEXT, " +
                    "FOREIGN KEY(\"ISSUE_ID\") REFERENCES \"ISSUE\"(\"ISSUE_ID\")" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS \"PACKAGES\" (" +
                    "\"ISSUE_ID\" INTEGER, " +
                    "\"ID\" INTEGER, " +
                    "\"NAME\" TEXT, " +
                    "PRIMARY KEY(\"ID\" AUTOINCREMENT)" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS PULL_RQ (" +
                    "PR_ID INT PRIMARY KEY, " +
                    "ISSUE_ID INT, " +
                    "REPO_ID INT, " +
                    "USER_ID INT, " +
                    "TITLE VARCHAR(255), " +
                    "BODY TEXT, " +
                    "STATE VARCHAR(50), " +
                    "CREATED_AT DATETIME, " +
                    "UPDATED_AT DATETIME, " +
                    "CLOSED_AT DATETIME, " +
                    "MERGED_AT DATETIME, " +
                    "MERGE_COMMIT_SHA VARCHAR(40), " +
                    "HEAD_REF VARCHAR(255), " +
                    "BASE_REF VARCHAR(255), " +
                    "MERGEABLE BOOLEAN, " +
                    "MERGED BOOLEAN, " +
                    "COMMENTS_COUNT INT, " +
                    "REVIEW_COMMENTS_COUNT INT, " +
                    "COMMITS_COUNT INT, " +
                    "ADDITIONS INT, " +
                    "DELETIONS INT, " +
                    "CHANGED_FILES_COUNT INT" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS pr_review_comments (" +
                    "COMMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "PR_NO INT, " +
                    "ISSUE_ID INT, " +
                    "USER_ID INT, " +
                    "COMMENT TEXT, " +
                    "CREATED_AT DATETIME" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS pr_reviewers (" +
                    "REVIEWER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "PR_NO INT, " +
                    "ISSUE_ID INT, " +
                    "USER_ID VARCHAR(255)" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS pr_files_changed (" +
                    "FILE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "PR_NO INT, " +
                    "ISSUE_ID INT, " +
                    "FILE_NAME VARCHAR(255), " +
                    "STATUS VARCHAR(100)" +
                    ")",
                    
                    "CREATE TABLE IF NOT EXISTS TEAMS (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "TEAM_NAME VARCHAR(255), " +
                    "PR_NO INT, " +
                    "ISSUE_ID INT" +
                    ")"
                };
                
                for (String sql : createTableStatements) {
                    stmt.execute(sql);
                }
                
                System.out.println("Database schema created successfully!");
            } else {
                System.out.println("Database schema already exists.");
            }
            rs.close();
            
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        getConnection("test");
    }

}
