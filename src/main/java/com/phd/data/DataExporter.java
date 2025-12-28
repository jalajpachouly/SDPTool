package com.phd.data;

import com.phd.config.Configuration;
import com.phd.db.Connect;
import com.phd.issue.Issue;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles exporting processed data to CSV format for machine learning
 */
public class DataExporter {
    
    // Valid label colors for filtering (10 predefined colors)
    private static final Set<String> VALID_COLORS = new HashSet<>(Arrays.asList(
        "0052cc", "006b75", "0e8a16", "1d76db", "5319e7",
        "c5def5", "d93f0b", "e99695", "f9d0c4", "fbca04"
    ));
    
    /**
     * Export data in multilabel format (binary columns for each label)
     * Format: report,type_blocker,type_regression,type_bug,type_documentation,type_enhancement,type_task,type_dependency_upgrade
     */
    public static void exportMultilabel(JFrame parent) {
        File outputFile = showSaveDialog(parent, "multilabel_dataset_" + getTimestamp() + ".csv");
        if (outputFile == null) return;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            // Get all unique label names first
            Set<String> allLabels = getAllLabelNames();
            List<String> sortedLabels = new ArrayList<>(allLabels);
            Collections.sort(sortedLabels);
            
            // Write header
            writer.write("report");
            for (String label : sortedLabels) {
                writer.write(",type_" + sanitizeLabelName(label));
            }
            writer.newLine();
            
            // Get all issues with valid labels
            List<ExportDataRow> dataRows = fetchExportData(true);
            
            // Write data rows
            for (ExportDataRow row : dataRows) {
                writer.write("\"" + escapeQuotes(row.report) + "\"");
                
                // Create binary columns for each label
                Set<String> issueLabels = new HashSet<>(row.labels);
                for (String label : sortedLabels) {
                    writer.write(",");
                    writer.write(issueLabels.contains(label) ? "1" : "0");
                }
                writer.newLine();
            }
            
            JOptionPane.showMessageDialog(parent, 
                "Multilabel dataset exported successfully!\n" +
                "Records: " + dataRows.size() + "\n" +
                "Labels: " + sortedLabels.size() + "\n" +
                "File: " + outputFile.getAbsolutePath(),
                "Export Complete", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, 
                "Error exporting data: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Export data in multiclass format (single target column with module/component)
     * Format: report,target
     */
    public static void exportMulticlass(JFrame parent) {
        File outputFile = showSaveDialog(parent, "multiclass_dataset_" + getTimestamp() + ".csv");
        if (outputFile == null) return;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            // Write header
            writer.write("report,target");
            writer.newLine();
            
            // Get all issues with valid labels
            List<ExportDataRow> dataRows = fetchExportData(true);
            
            // Write data rows
            for (ExportDataRow row : dataRows) {
                String target = extractModuleFromLabels(row.labels);
                if (target != null && !target.isEmpty()) {
                    writer.write("\"" + escapeQuotes(row.report) + "\"");
                    writer.write(",");
                    writer.write(target);
                    writer.newLine();
                }
            }
            
            JOptionPane.showMessageDialog(parent, 
                "Multiclass dataset exported successfully!\n" +
                "Records: " + dataRows.size() + "\n" +
                "File: " + outputFile.getAbsolutePath(),
                "Export Complete", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, 
                "Error exporting data: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Fetch all issues with processed data from database
     */
    private static List<ExportDataRow> fetchExportData(boolean filterByColor) {
        List<ExportDataRow> rows = new ArrayList<>();
        Connection conn = Connect.getConnection(Configuration.getConfig().getDbLocation());
        
        String query = "SELECT i.ISSUE_ID, i.PROCESSED_TITLES, i.PROCESSED_BODY " +
                      "FROM ISSUE i " +
                      "WHERE i.PROCESSED_TITLES IS NOT NULL " +
                      "AND i.PROCESSED_BODY IS NOT NULL " +
                      "ORDER BY i.ISSUE_ID";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int issueId = rs.getInt("ISSUE_ID");
                String title = rs.getString("PROCESSED_TITLES");
                String body = rs.getString("PROCESSED_BODY");
                
                // Get comments for this issue
                String comments = getProcessedComments(conn, issueId);
                
                // Get labels for this issue
                List<String> labels = getLabelsForIssue(conn, issueId, filterByColor);
                
                // Skip issues without valid labels
                if (labels.isEmpty()) {
                    continue;
                }
                
                // Build report paragraph
                String report = buildReportParagraph(title, body, comments);
                
                ExportDataRow row = new ExportDataRow();
                row.issueId = issueId;
                row.report = report;
                row.labels = labels;
                
                rows.add(row);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Connect.closeConnection(conn);
        }
        
        return rows;
    }
    
    /**
     * Get processed comments for an issue
     */
    private static String getProcessedComments(Connection conn, int issueId) {
        StringBuilder comments = new StringBuilder();
        String query = "SELECT PROCESSED_COMMENTS FROM COMMENTS WHERE ISSUE_ID = ? AND PROCESSED_COMMENTS IS NOT NULL";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, issueId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String comment = rs.getString("PROCESSED_COMMENTS");
                if (comment != null && !comment.trim().isEmpty()) {
                    if (comments.length() > 0) {
                        comments.append(" ");
                    }
                    comments.append(comment.trim());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return comments.toString();
    }
    
    /**
     * Get labels for an issue with optional color filtering
     */
    private static List<String> getLabelsForIssue(Connection conn, int issueId, boolean filterByColor) {
        List<String> labels = new ArrayList<>();
        String query = "SELECT NAME, COLOR FROM LABEL WHERE ISSUE_ID = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, issueId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String name = rs.getString("NAME");
                String color = rs.getString("COLOR");
                
                // Apply color filtering if enabled
                if (filterByColor) {
                    if (color != null && VALID_COLORS.contains(color.toLowerCase())) {
                        labels.add(name);
                    }
                } else {
                    labels.add(name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return labels;
    }
    
    /**
     * Get all unique label names from database
     */
    private static Set<String> getAllLabelNames() {
        Set<String> labels = new HashSet<>();
        Connection conn = Connect.getConnection(Configuration.getConfig().getDbLocation());
        
        String query = "SELECT DISTINCT NAME FROM LABEL WHERE COLOR IN (" +
                      String.join(",", Collections.nCopies(VALID_COLORS.size(), "?")) + ")";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            int idx = 1;
            for (String color : VALID_COLORS) {
                pstmt.setString(idx++, color);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                labels.add(rs.getString("NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            Connect.closeConnection(conn);
        }
        
        return labels;
    }
    
    /**
     * Build report paragraph from title, body, and comments
     * Removes code blocks, keeps spaces, creates flowing paragraph
     */
    private static String buildReportParagraph(String title, String body, String comments) {
        StringBuilder report = new StringBuilder();
        
        // Add title
        if (title != null && !title.trim().isEmpty()) {
            report.append(cleanText(title)).append(" ");
        }
        
        // Add body (remove code blocks)
        if (body != null && !body.trim().isEmpty()) {
            String cleanBody = removeCodeBlocks(body);
            report.append(cleanText(cleanBody)).append(" ");
        }
        
        // Add comments
        if (comments != null && !comments.trim().isEmpty()) {
            report.append(cleanText(comments));
        }
        
        // Clean up extra spaces
        String result = report.toString().trim();
        result = result.replaceAll("\\s+", " "); // Normalize spaces
        
        return result;
    }
    
    /**
     * Remove code blocks from text (markdown and inline code)
     */
    private static String removeCodeBlocks(String text) {
        if (text == null) return "";
        
        // Remove markdown code blocks (```...```)
        text = text.replaceAll("```[\\s\\S]*?```", " ");
        
        // Remove inline code (`...`)
        text = text.replaceAll("`[^`]+`", " ");
        
        // Remove HTML code blocks
        text = text.replaceAll("<code>[\\s\\S]*?</code>", " ");
        text = text.replaceAll("<pre>[\\s\\S]*?</pre>", " ");
        
        return text;
    }
    
    /**
     * Clean text by removing extra whitespace and normalizing
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        
        // Remove newlines and tabs, replace with space
        text = text.replaceAll("[\\r\\n\\t]+", " ");
        
        // Remove multiple spaces
        text = text.replaceAll("\\s+", " ");
        
        return text.trim();
    }
    
    /**
     * Extract module/component name from labels (e.g., "changetype_jms")
     * Looks for labels starting with "changetype_", "module:", "component:", etc.
     */
    private static String extractModuleFromLabels(List<String> labels) {
        for (String label : labels) {
            // Check for changetype_ pattern
            if (label.toLowerCase().startsWith("changetype_")) {
                return label.toLowerCase();
            }
            
            // Check for module: pattern
            if (label.toLowerCase().startsWith("module:")) {
                return "changetype_" + label.substring(7).toLowerCase().replaceAll("[^a-z0-9]+", "_");
            }
            
            // Check for component: pattern
            if (label.toLowerCase().startsWith("component:")) {
                return "changetype_" + label.substring(10).toLowerCase().replaceAll("[^a-z0-9]+", "_");
            }
        }
        
        // If no module found, use first label as fallback
        if (!labels.isEmpty()) {
            return "changetype_" + sanitizeLabelName(labels.get(0));
        }
        
        return "changetype_unknown";
    }
    
    /**
     * Sanitize label name for column headers
     */
    private static String sanitizeLabelName(String label) {
        if (label == null) return "unknown";
        
        // Convert to lowercase and replace special chars with underscore
        return label.toLowerCase()
                   .replaceAll("[^a-z0-9]+", "_")
                   .replaceAll("^_+|_+$", ""); // Remove leading/trailing underscores
    }
    
    /**
     * Escape quotes in CSV values
     */
    private static String escapeQuotes(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
    
    /**
     * Show file save dialog
     */
    private static File showSaveDialog(JFrame parent, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Dataset");
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // Ensure .csv extension
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            return file;
        }
        
        return null;
    }
    
    /**
     * Get timestamp for filename
     */
    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return sdf.format(new Date());
    }
    
    /**
     * Inner class to hold export data
     */
    private static class ExportDataRow {
        int issueId;
        String report;
        List<String> labels;
    }
}
