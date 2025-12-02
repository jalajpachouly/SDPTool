package com.phd.config;

import java.io.*;
import java.util.Properties;

/**
 * Manages database configuration properties
 * Reads from database.properties and allows runtime updates
 */
public class DatabaseProperties {
    
    private static final String PROPERTIES_FILE = "database.properties";
    private static final String DEFAULT_DB_LOCATION = "./data/sdp.db"; // Relative path
    private static Properties properties;
    private static String propertiesFilePath;
    
    static {
        loadProperties();
    }
    
    /**
     * Load properties from classpath or working directory
     */
    private static void loadProperties() {
        properties = new Properties();
        
        // Try to load from working directory first (for user overrides)
        File workingDirProps = new File(PROPERTIES_FILE);
        if (workingDirProps.exists()) {
            try (FileInputStream fis = new FileInputStream(workingDirProps)) {
                properties.load(fis);
                propertiesFilePath = workingDirProps.getAbsolutePath();
                System.out.println("Loaded database properties from: " + propertiesFilePath);
                return;
            } catch (IOException e) {
                System.err.println("Failed to load properties from working directory: " + e.getMessage());
            }
        }
        
        // Fall back to classpath (default from resources)
        try (InputStream input = DatabaseProperties.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
                // Copy to working directory for future edits
                propertiesFilePath = copyToWorkingDirectory();
                System.out.println("Loaded default database properties from classpath");
            } else {
                System.out.println("Properties file not found, using defaults");
                setDefaults();
                propertiesFilePath = new File(PROPERTIES_FILE).getAbsolutePath();
                saveProperties();
            }
        } catch (IOException e) {
            System.err.println("Error loading database properties: " + e.getMessage());
            setDefaults();
        }
    }
    
    /**
     * Copy properties from classpath to working directory
     */
    private static String copyToWorkingDirectory() {
        File targetFile = new File(PROPERTIES_FILE);
        try (InputStream input = DatabaseProperties.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE);
             FileOutputStream output = new FileOutputStream(targetFile)) {
            
            if (input != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                System.out.println("Created properties file in working directory: " + 
                                   targetFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to copy properties file: " + e.getMessage());
        }
        return targetFile.getAbsolutePath();
    }
    
    /**
     * Set default property values
     */
    private static void setDefaults() {
        properties.setProperty("database.location", DEFAULT_DB_LOCATION);
        properties.setProperty("database.auto.create", "true");
        properties.setProperty("database.auto.init.schema", "true");
    }
    
    /**
     * Get database location from properties
     * @return database file path (relative or absolute)
     */
    public static String getDatabaseLocation() {
        String location = properties.getProperty("database.location", DEFAULT_DB_LOCATION);
        
        // Convert relative path to absolute
        File dbFile = new File(location);
        if (!dbFile.isAbsolute()) {
            dbFile = new File(System.getProperty("user.dir"), location);
            location = dbFile.getAbsolutePath();
        }
        
        return location;
    }
    
    /**
     * Get the raw database location as stored in properties (may be relative)
     * @return raw database location string
     */
    public static String getRawDatabaseLocation() {
        return properties.getProperty("database.location", DEFAULT_DB_LOCATION);
    }
    
    /**
     * Update database location in properties and save to file
     * @param location new database location
     * @return true if successfully saved
     */
    public static boolean setDatabaseLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            System.err.println("Cannot set empty database location");
            return false;
        }
        
        properties.setProperty("database.location", location);
        return saveProperties();
    }
    
    /**
     * Save properties to file in working directory
     * @return true if successfully saved
     */
    private static boolean saveProperties() {
        File propsFile = new File(PROPERTIES_FILE);
        try (FileOutputStream output = new FileOutputStream(propsFile)) {
            properties.store(output, "Database Configuration - Updated: " + 
                           new java.util.Date());
            propertiesFilePath = propsFile.getAbsolutePath();
            System.out.println("Saved database properties to: " + propertiesFilePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save database properties: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get auto-create database setting
     */
    public static boolean isAutoCreate() {
        return Boolean.parseBoolean(properties.getProperty("database.auto.create", "true"));
    }
    
    /**
     * Get auto-init schema setting
     */
    public static boolean isAutoInitSchema() {
        return Boolean.parseBoolean(properties.getProperty("database.auto.init.schema", "true"));
    }
    
    /**
     * Get properties file path
     */
    public static String getPropertiesFilePath() {
        return propertiesFilePath;
    }
    
    /**
     * Reload properties from file
     */
    public static void reload() {
        loadProperties();
    }
    
    /**
     * Get default database location
     */
    public static String getDefaultDatabaseLocation() {
        return DEFAULT_DB_LOCATION;
    }
}
