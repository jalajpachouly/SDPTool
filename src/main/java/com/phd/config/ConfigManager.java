package com.phd.config;

import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Centralized configuration manager for multi-label and multi-class experiments.
 * All panels read/write to these shared config objects to avoid synchronization issues.
 */
public class ConfigManager {
    private static JSONObject multiLabelConfig;
    private static JSONObject multiClassConfig;
    
    public static final String MULTILABEL_CONFIG_PATH = "multilable-prediction/configs/ui_config.json";
    public static final String MULTICLASS_CONFIG_PATH = "software-change-type-prediction-main/configs/ui_config_multiclass.json";
    
    /**
     * Initialize configs by loading from disk at application startup
     */
    public static void initialize() {
        multiLabelConfig = loadConfig(MULTILABEL_CONFIG_PATH, "multi_label");
        multiClassConfig = loadConfig(MULTICLASS_CONFIG_PATH, "multi_class");
        System.out.println("✓ ConfigManager initialized");
        System.out.println("  Multi-label config path: " + new java.io.File(MULTILABEL_CONFIG_PATH).getAbsolutePath());
        System.out.println("  Multi-class config path: " + new java.io.File(MULTICLASS_CONFIG_PATH).getAbsolutePath());
    }
    
    /**
     * Load config from file, create default if not exists
     */
    private static JSONObject loadConfig(String path, String problemType) {
        JSONObject config;
        try (FileReader reader = new FileReader(path)) {
            config = new JSONObject(new JSONTokener(reader));
            System.out.println("  Loaded config from: " + path);
        } catch (Exception e) {
            System.err.println("  Creating new config for: " + path + " (" + e.getMessage() + ")");
            config = new JSONObject();
        }
        
        // Ensure problem_type is set
        if (!config.has("problem_type")) {
            config.put("problem_type", problemType);
        }
        
        // Ensure required sections exist
        ensureObject(config, "models");
        ensureObject(config, "data");
        ensureObject(config, "feature_engineering");
        ensureObject(config, "visualizations");
        
        return config;
    }
    
    /**
     * Save multi-label config to disk
     */
    public static synchronized void saveMultiLabel() {
        saveConfig(multiLabelConfig, MULTILABEL_CONFIG_PATH);
    }
    
    /**
     * Save multi-class config to disk
     */
    public static synchronized void saveMultiClass() {
        saveConfig(multiClassConfig, MULTICLASS_CONFIG_PATH);
    }
    
    /**
     * Save config to file
     */
    private static void saveConfig(JSONObject config, String path) {
        try (FileWriter writer = new FileWriter(path, StandardCharsets.UTF_8)) {
            writer.write(config.toString(2));
            System.out.println("✓ Config saved to: " + new java.io.File(path).getAbsolutePath());
        } catch (Exception e) {
            System.err.println("✗ Failed to save config to " + path + ": " + e.getMessage());
            throw new RuntimeException("Failed to save configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the shared multi-label config object
     */
    public static JSONObject getMultiLabelConfig() {
        if (multiLabelConfig == null) {
            throw new IllegalStateException("ConfigManager not initialized. Call initialize() first.");
        }
        return multiLabelConfig;
    }
    
    /**
     * Get the shared multi-class config object
     */
    public static JSONObject getMultiClassConfig() {
        if (multiClassConfig == null) {
            throw new IllegalStateException("ConfigManager not initialized. Call initialize() first.");
        }
        return multiClassConfig;
    }
    
    /**
     * Get config paths
     */
    public static String getMultiLabelConfigPath() {
        return MULTILABEL_CONFIG_PATH;
    }
    
    public static String getMultiClassConfigPath() {
        return MULTICLASS_CONFIG_PATH;
    }
    
    /**
     * Ensure a JSON object exists in parent
     */
    private static JSONObject ensureObject(JSONObject parent, String key) {
        JSONObject existing = parent.optJSONObject(key);
        if (existing == null) {
            existing = new JSONObject();
            parent.put(key, existing);
        }
        return existing;
    }
}
