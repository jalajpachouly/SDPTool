package com.phd.config;

import java.util.*;

/**
 * Configuration for color-based label filtering to ensure consistency in dataset.
 * Implements mitigation strategy for inconsistent labeling practices across repositories.
 * 
 * Based on research requirements:
 * - Only labels with predefined colors represent high-confidence defect types
 * - Ensures consistent, well-established label types are included
 * - Maintains referential integrity through database constraints
 */
public class LabelFilterConfig {
    
    /**
     * Predefined label colors that represent high-confidence defect types.
     * These colors were selected based on analysis of labeling conventions
     * across multiple repositories to ensure consistency.
     */
    private static final Set<String> VALID_LABEL_COLORS = new HashSet<>(Arrays.asList(
        "d93f0b",  // Orange-red (bug severity)
        "b60205",  // Red (critical bugs)
        "db755e",  // Rust (enhancement bugs)
        "4a4ea8",  // Blue (feature defects)
        "006b75",  // Teal (technical debt)
        "d4c5f9",  // Light purple (UI/UX issues)
        "cfcfcf",  // Light gray (minor issues)
        "dddddd",  // Very light gray (documentation)
        "5319e7",  // Purple (performance)
        "e11d21"   // Bright red (security)
    ));
    
    /**
     * Keywords that indicate incomplete or untriaged labels.
     * Issues with these labels should be excluded from dataset.
     */
    private static final Set<String> EXCLUDED_LABEL_KEYWORDS = new HashSet<>(Arrays.asList(
        "waiting",
        "pending",
        "triage",
        "needs",
        "question",
        "invalid",
        "wontfix",
        "duplicate",
        "help wanted",
        "good first issue"
    ));
    
    /**
     * High-importance code modules for multiclass classification.
     * Chosen based on developer community activity and significance.
     */
    private static final Set<String> HIGH_IMPORTANCE_MODULES = new HashSet<>(Arrays.asList(
        "spring-core",
        "spring-boot",
        "spring-web",
        "spring-data",
        "jdbc",
        "jpa",
        "hibernate",
        "tcp",
        "udp",
        "http",
        "rest",
        "security",
        "authentication",
        "authorization"
    ));
    
    /**
     * Minimum number of labels required for an issue to be considered valid.
     * Set to 1 by default. Can be adjusted for multi-label requirements.
     */
    private static int minLabelCount = 1;
    
    /**
     * Maximum number of labels allowed for single-label classification.
     * Issues with more labels will be filtered out.
     */
    private static int maxLabelCountForSingleClass = 1;
    
    /**
     * Whether to enable color-based filtering.
     */
    private static boolean enableColorFiltering = true;
    
    /**
     * Whether to filter out incomplete/untriaged labels.
     */
    private static boolean enableIncompleteFiltering = true;
    
    /**
     * Whether to filter by high-importance modules.
     */
    private static boolean enableModuleFiltering = false;
    
    /**
     * Whether to enforce single-label constraint.
     */
    private static boolean enforceSingleLabel = false;
    
    // Getters for valid colors
    public static Set<String> getValidLabelColors() {
        return Collections.unmodifiableSet(VALID_LABEL_COLORS);
    }
    
    public static boolean isValidLabelColor(String color) {
        if (color == null || !enableColorFiltering) {
            return true;
        }
        return VALID_LABEL_COLORS.contains(color.toLowerCase().replace("#", ""));
    }
    
    public static Set<String> getExcludedLabelKeywords() {
        return Collections.unmodifiableSet(EXCLUDED_LABEL_KEYWORDS);
    }
    
    public static boolean isExcludedLabel(String labelName) {
        if (labelName == null || !enableIncompleteFiltering) {
            return false;
        }
        String lowerName = labelName.toLowerCase();
        return EXCLUDED_LABEL_KEYWORDS.stream()
                .anyMatch(keyword -> lowerName.contains(keyword));
    }
    
    public static Set<String> getHighImportanceModules() {
        return Collections.unmodifiableSet(HIGH_IMPORTANCE_MODULES);
    }
    
    public static boolean isHighImportanceModule(String moduleName) {
        if (moduleName == null || !enableModuleFiltering) {
            return true;
        }
        String lowerModule = moduleName.toLowerCase();
        return HIGH_IMPORTANCE_MODULES.stream()
                .anyMatch(module -> lowerModule.contains(module));
    }
    
    // Configuration setters
    public static void setEnableColorFiltering(boolean enable) {
        enableColorFiltering = enable;
    }
    
    public static boolean isColorFilteringEnabled() {
        return enableColorFiltering;
    }
    
    public static void setEnableIncompleteFiltering(boolean enable) {
        enableIncompleteFiltering = enable;
    }
    
    public static boolean isIncompleteFilteringEnabled() {
        return enableIncompleteFiltering;
    }
    
    public static void setEnableModuleFiltering(boolean enable) {
        enableModuleFiltering = enable;
    }
    
    public static boolean isModuleFilteringEnabled() {
        return enableModuleFiltering;
    }
    
    public static void setEnforceSingleLabel(boolean enforce) {
        enforceSingleLabel = enforce;
    }
    
    public static boolean isSingleLabelEnforced() {
        return enforceSingleLabel;
    }
    
    public static void setMinLabelCount(int count) {
        minLabelCount = Math.max(1, count);
    }
    
    public static int getMinLabelCount() {
        return minLabelCount;
    }
    
    public static void setMaxLabelCountForSingleClass(int count) {
        maxLabelCountForSingleClass = Math.max(1, count);
    }
    
    public static int getMaxLabelCountForSingleClass() {
        return maxLabelCountForSingleClass;
    }
    
    /**
     * Generate SQL WHERE clause for color filtering.
     * @return SQL condition string
     */
    public static String getColorFilterSQL() {
        if (!enableColorFiltering) {
            return "1=1";
        }
        StringBuilder sql = new StringBuilder("COLOR IN (");
        List<String> colors = new ArrayList<>(VALID_LABEL_COLORS);
        for (int i = 0; i < colors.size(); i++) {
            sql.append("'").append(colors.get(i)).append("'");
            if (i < colors.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        return sql.toString();
    }
    
    /**
     * Generate SQL WHERE clause for excluded label filtering.
     * @return SQL condition string
     */
    public static String getExcludedLabelFilterSQL() {
        if (!enableIncompleteFiltering) {
            return "1=1";
        }
        StringBuilder sql = new StringBuilder("(");
        List<String> keywords = new ArrayList<>(EXCLUDED_LABEL_KEYWORDS);
        for (int i = 0; i < keywords.size(); i++) {
            sql.append("LOWER(NAME) NOT LIKE '%").append(keywords.get(i)).append("%'");
            if (i < keywords.size() - 1) {
                sql.append(" AND ");
            }
        }
        sql.append(")");
        return sql.toString();
    }
    
    /**
     * Print current filter configuration to console.
     */
    public static void printConfiguration() {
        System.out.println("=== Label Filter Configuration ===");
        System.out.println("Color Filtering: " + (enableColorFiltering ? "ENABLED" : "DISABLED"));
        if (enableColorFiltering) {
            System.out.println("  Valid Colors: " + VALID_LABEL_COLORS.size() + " colors");
        }
        System.out.println("Incomplete Label Filtering: " + (enableIncompleteFiltering ? "ENABLED" : "DISABLED"));
        if (enableIncompleteFiltering) {
            System.out.println("  Excluded Keywords: " + EXCLUDED_LABEL_KEYWORDS.size() + " keywords");
        }
        System.out.println("Module Filtering: " + (enableModuleFiltering ? "ENABLED" : "DISABLED"));
        if (enableModuleFiltering) {
            System.out.println("  High-Importance Modules: " + HIGH_IMPORTANCE_MODULES.size() + " modules");
        }
        System.out.println("Single Label Enforcement: " + (enforceSingleLabel ? "ENABLED" : "DISABLED"));
        System.out.println("Min Label Count: " + minLabelCount);
        System.out.println("Max Label Count (Single Class): " + maxLabelCountForSingleClass);
        System.out.println("==================================");
    }
}
