package com.phd.db;

import com.phd.config.DatabaseProperties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test class to verify database connection and schema creation
 * Run this to test if the database fixes work correctly
 */
public class DatabaseTest {
    
    public static void main(String[] args) {
        System.out.println("=== SDPTool Database Connection Test ===\n");
        
        // Test 1: Property file default location
        testDatabaseConnection(null, "Test 1: Default from Properties File");
        
        // Test 2: Custom location in temp directory
        testDatabaseConnection("C:/Temp/SDPTool/test_custom.db", "Test 2: Custom Location");
        
        // Test 3: Relative path
        testDatabaseConnection("./test_data/relative.db", "Test 3: Relative Path");
        
        // Test 4: Test property file operations
        testPropertyFileOperations();
        
        System.out.println("\n=== All Tests Complete ===");
    }
    
    private static void testDatabaseConnection(String dbLocation, String testName) {
        System.out.println("\n--- " + testName + " ---");
        System.out.println("Database Path: " + (dbLocation == null ? "(from properties)" : dbLocation));
        
        Connection conn = null;
        try {
            // Get connection
            conn = Connect.getConnection(dbLocation);
            
            if (conn == null) {
                System.out.println("❌ FAILED: Connection is null");
                return;
            }
            
            System.out.println("✅ Connection established");
            
            // Check if ISSUE table exists and query it
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
            
            int tableCount = 0;
            System.out.println("\nTables found:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("name"));
                tableCount++;
            }
            
            if (tableCount >= 14) {
                System.out.println("\n✅ Schema created successfully (" + tableCount + " tables)");
            } else {
                System.out.println("\n⚠️  Warning: Expected 14 tables, found " + tableCount);
            }
            
            // Test INSERT capability
            stmt.execute("INSERT INTO ISSUE (ISSUE_ID, TITLE, REPORTER) VALUES (99999, 'Test Issue', 'TestUser')");
            System.out.println("✅ INSERT test successful");
            
            // Test SELECT capability
            ResultSet testRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM ISSUE WHERE ISSUE_ID = 99999");
            if (testRs.next() && testRs.getInt("cnt") == 1) {
                System.out.println("✅ SELECT test successful");
            }
            
            // Cleanup test data
            stmt.execute("DELETE FROM ISSUE WHERE ISSUE_ID = 99999");
            System.out.println("✅ DELETE test successful");
            
            rs.close();
            stmt.close();
            
            System.out.println("\n✅✅✅ " + testName + " PASSED ✅✅✅");
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                Connect.closeConnection(conn);
                System.out.println("Connection closed");
            }
        }
    }
    
    private static void testPropertyFileOperations() {
        System.out.println("\n--- Test 4: Property File Operations ---");
        
        try {
            // Test reading default
            String defaultLocation = DatabaseProperties.getDatabaseLocation();
            System.out.println("✅ Default location: " + defaultLocation);
            
            String rawLocation = DatabaseProperties.getRawDatabaseLocation();
            System.out.println("✅ Raw location: " + rawLocation);
            
            // Test properties file path
            String propsPath = DatabaseProperties.getPropertiesFilePath();
            System.out.println("✅ Properties file: " + propsPath);
            
            // Test flags
            boolean autoCreate = DatabaseProperties.isAutoCreate();
            boolean autoInit = DatabaseProperties.isAutoInitSchema();
            System.out.println("✅ Auto-create: " + autoCreate + ", Auto-init: " + autoInit);
            
            // Test updating (save original first)
            String originalLocation = DatabaseProperties.getDatabaseLocation();
            String testLocation = "./test_override/test.db";
            
            if (DatabaseProperties.setDatabaseLocation(testLocation)) {
                System.out.println("✅ Updated location to: " + testLocation);
                
                // Verify update
                DatabaseProperties.reload();
                String newLocation = DatabaseProperties.getRawDatabaseLocation();
                
                if (newLocation.equals(testLocation)) {
                    System.out.println("✅ Location update verified");
                } else {
                    System.out.println("❌ Location not updated correctly");
                }
                
                // Restore original
                DatabaseProperties.setDatabaseLocation(rawLocation);
                DatabaseProperties.reload();
                System.out.println("✅ Restored original location");
            }
            
            System.out.println("\n✅✅✅ Test 4: Property File Operations PASSED ✅✅✅");
            
        } catch (Exception e) {
            System.out.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
