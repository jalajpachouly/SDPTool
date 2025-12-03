package com.phd.ui;

import com.phd.config.LabelFilterConfig;
import com.phd.db.DBManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * UI Panel for configuring label filtering to mitigate inconsistent labeling practices.
 * 
 * Implements mitigation strategies:
 * - Color-based filtering for high-confidence defect types
 * - Exclusion of incomplete/untriaged labels
 * - Single-label vs multi-label dataset generation
 * - Module importance filtering for multiclass classification
 */
public class LabelFilterPanel extends JPanel {
    
    private JCheckBox colorFilteringCheckBox;
    private JCheckBox incompleteFilteringCheckBox;
    private JCheckBox moduleFilteringCheckBox;
    private JCheckBox singleLabelCheckBox;
    
    private JSpinner minLabelSpinner;
    private JSpinner maxSingleLabelSpinner;
    
    private JButton applyButton;
    private JButton viewStatsButton;
    private JButton testQueryButton;
    
    private JTextArea statsArea;
    
    public LabelFilterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);
        
        // Main content with config and stats
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(createConfigPanel());
        splitPane.setBottomComponent(createStatsPanel());
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.6);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Load current configuration
        loadConfiguration();
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 28, 38));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("Label Filtering Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        
        JLabel subtitle = new JLabel("Mitigate inconsistent labeling practices across repositories");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(200, 210, 220));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);
        
        panel.add(textPanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Filter Configuration"));
        
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Color filtering
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        colorFilteringCheckBox = new JCheckBox("Enable Color-Based Filtering");
        colorFilteringCheckBox.setToolTipText("Filter labels to only include 10 predefined colors representing high-confidence defect types");
        content.add(colorFilteringCheckBox, gbc);
        
        gbc.gridy = row++;
        JLabel colorInfo = new JLabel("<html><i>Only labels with predefined colors (10 specific colors) will be included</i></html>");
        colorInfo.setForeground(Color.GRAY);
        content.add(colorInfo, gbc);
        
        // Incomplete filtering
        gbc.gridy = row++;
        incompleteFilteringCheckBox = new JCheckBox("Exclude Incomplete/Untriaged Labels");
        incompleteFilteringCheckBox.setToolTipText("Exclude labels like 'waiting', 'pending', 'triage', 'needs', 'question', etc.");
        content.add(incompleteFilteringCheckBox, gbc);
        
        gbc.gridy = row++;
        JLabel incompleteInfo = new JLabel("<html><i>Filters out labels indicating incomplete or untriaged issues</i></html>");
        incompleteInfo.setForeground(Color.GRAY);
        content.add(incompleteInfo, gbc);
        
        // Single label enforcement
        gbc.gridy = row++;
        singleLabelCheckBox = new JCheckBox("Enforce Single-Label Constraint");
        singleLabelCheckBox.setToolTipText("Only include issues with exactly one valid label (for single-class classification)");
        content.add(singleLabelCheckBox, gbc);
        
        // Label count constraints
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        content.add(new JLabel("Minimum Labels:"), gbc);
        
        gbc.gridx = 1;
        minLabelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        minLabelSpinner.setToolTipText("Minimum number of valid labels required per issue");
        content.add(minLabelSpinner, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = row++;
        content.add(new JLabel("Max Labels (Single-Class):"), gbc);
        
        gbc.gridx = 1;
        maxSingleLabelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        maxSingleLabelSpinner.setToolTipText("Maximum labels allowed when single-label constraint is enforced");
        content.add(maxSingleLabelSpinner, gbc);
        
        // Module filtering
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        moduleFilteringCheckBox = new JCheckBox("Enable High-Importance Module Filtering");
        moduleFilteringCheckBox.setToolTipText("Filter by modules with high importance in developer communities (e.g., Spring Core, JDBC, TCP/UDP)");
        content.add(moduleFilteringCheckBox, gbc);
        
        gbc.gridy = row++;
        JLabel moduleInfo = new JLabel("<html><i>For multiclass datasets: Spring Core, JDBC, TCP/UDP, etc.</i></html>");
        moduleInfo.setForeground(Color.GRAY);
        content.add(moduleInfo, gbc);
        
        // Buttons
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        applyButton = new JButton("Apply Configuration");
        applyButton.setBackground(new Color(0, 120, 215));
        applyButton.setForeground(Color.WHITE);
        applyButton.addActionListener(new ApplyConfigListener());
        buttonPanel.add(applyButton);
        
        viewStatsButton = new JButton("View Statistics");
        viewStatsButton.addActionListener(e -> viewStatistics());
        buttonPanel.add(viewStatsButton);
        
        testQueryButton = new JButton("Test Filtered Query");
        testQueryButton.addActionListener(e -> testFilteredQuery());
        buttonPanel.add(testQueryButton);
        
        content.add(buttonPanel, gbc);
        
        panel.add(content, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Label Statistics & Validation"));
        
        statsArea = new JTextArea(15, 60);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        statsArea.setText("Click 'View Statistics' to analyze label distribution and filtering impact.");
        
        JScrollPane scrollPane = new JScrollPane(statsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadConfiguration() {
        colorFilteringCheckBox.setSelected(LabelFilterConfig.isColorFilteringEnabled());
        incompleteFilteringCheckBox.setSelected(LabelFilterConfig.isIncompleteFilteringEnabled());
        moduleFilteringCheckBox.setSelected(LabelFilterConfig.isModuleFilteringEnabled());
        singleLabelCheckBox.setSelected(LabelFilterConfig.isSingleLabelEnforced());
        
        minLabelSpinner.setValue(LabelFilterConfig.getMinLabelCount());
        maxSingleLabelSpinner.setValue(LabelFilterConfig.getMaxLabelCountForSingleClass());
    }
    
    private class ApplyConfigListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Apply configuration
            LabelFilterConfig.setEnableColorFiltering(colorFilteringCheckBox.isSelected());
            LabelFilterConfig.setEnableIncompleteFiltering(incompleteFilteringCheckBox.isSelected());
            LabelFilterConfig.setEnableModuleFiltering(moduleFilteringCheckBox.isSelected());
            LabelFilterConfig.setEnforceSingleLabel(singleLabelCheckBox.isSelected());
            
            LabelFilterConfig.setMinLabelCount((Integer) minLabelSpinner.getValue());
            LabelFilterConfig.setMaxLabelCountForSingleClass((Integer) maxSingleLabelSpinner.getValue());
            
            // Print to console
            LabelFilterConfig.printConfiguration();
            
            // Show confirmation
            JOptionPane.showMessageDialog(
                LabelFilterPanel.this,
                "Label filtering configuration applied successfully!\n\n" +
                "This configuration will be used during:\n" +
                "- Data collection from GitHub\n" +
                "- Dataset export operations\n" +
                "- Query execution\n\n" +
                "Check console for detailed configuration.",
                "Configuration Applied",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            // Update stats area with current config
            statsArea.setText("Configuration Applied:\n\n" +
                "Color Filtering: " + (colorFilteringCheckBox.isSelected() ? "ENABLED" : "DISABLED") + "\n" +
                "Incomplete Filtering: " + (incompleteFilteringCheckBox.isSelected() ? "ENABLED" : "DISABLED") + "\n" +
                "Module Filtering: " + (moduleFilteringCheckBox.isSelected() ? "ENABLED" : "DISABLED") + "\n" +
                "Single-Label Enforcement: " + (singleLabelCheckBox.isSelected() ? "ENABLED" : "DISABLED") + "\n" +
                "Min Labels: " + minLabelSpinner.getValue() + "\n" +
                "Max Labels (Single-Class): " + maxSingleLabelSpinner.getValue() + "\n\n" +
                "Click 'View Statistics' to analyze filtering impact on your dataset.");
        }
    }
    
    private void viewStatistics() {
        statsArea.setText("Loading statistics...\n");
        
        // Run in background thread
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                Map<String, Object> stats = DBManager.getLabelStatistics();
                
                StringBuilder sb = new StringBuilder();
                sb.append("=== LABEL STATISTICS ===\n\n");
                
                sb.append("Total Labels: ").append(stats.get("total_labels")).append("\n");
                sb.append("Valid Labels (by color): ").append(stats.get("valid_labels_by_color")).append("\n");
                sb.append("Issues with Valid Labels: ").append(stats.get("issues_with_valid_labels")).append("\n\n");
                
                sb.append("COLOR DISTRIBUTION:\n");
                @SuppressWarnings("unchecked")
                Map<String, Integer> colorDist = (Map<String, Integer>) stats.get("color_distribution");
                colorDist.forEach((color, count) -> {
                    boolean isValid = LabelFilterConfig.isValidLabelColor(color);
                    sb.append("  ").append(color).append(": ").append(count)
                      .append(isValid ? " [VALID]" : " [FILTERED]").append("\n");
                });
                
                sb.append("\nLABEL COUNT DISTRIBUTION:\n");
                @SuppressWarnings("unchecked")
                Map<Integer, Integer> labelCountDist = (Map<Integer, Integer>) stats.get("label_count_distribution");
                labelCountDist.forEach((labelCount, issueCount) -> 
                    sb.append("  ").append(labelCount).append(" label(s): ")
                      .append(issueCount).append(" issue(s)\n"));
                
                sb.append("\n=== FILTERING IMPACT ===\n\n");
                int totalLabels = (Integer) stats.get("total_labels");
                int validLabels = (Integer) stats.get("valid_labels_by_color");
                int filteredLabels = totalLabels - validLabels;
                double filterPercentage = (filteredLabels * 100.0) / totalLabels;
                
                sb.append("Filtered Labels: ").append(filteredLabels).append(" (")
                  .append(String.format("%.1f%%", filterPercentage)).append(")\n");
                sb.append("Retained Labels: ").append(validLabels).append(" (")
                  .append(String.format("%.1f%%", 100 - filterPercentage)).append(")\n\n");
                
                sb.append("This filtering ensures only high-confidence,\n");
                sb.append("well-established label types are included in the dataset.\n");
                
                return sb.toString();
            }
            
            @Override
            protected void done() {
                try {
                    statsArea.setText(get());
                } catch (Exception e) {
                    statsArea.setText("Error loading statistics:\n" + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    private void testFilteredQuery() {
        statsArea.setText("Executing filtered query...\n");
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                long startTime = System.currentTimeMillis();
                var issues = DBManager.getFilteredIssues();
                long endTime = System.currentTimeMillis();
                
                StringBuilder sb = new StringBuilder();
                sb.append("=== FILTERED QUERY RESULTS ===\n\n");
                sb.append("Execution Time: ").append(endTime - startTime).append(" ms\n");
                sb.append("Filtered Issues Found: ").append(issues.size()).append("\n\n");
                
                if (issues.size() > 0) {
                    sb.append("Sample Issues (first 10):\n");
                    for (int i = 0; i < Math.min(10, issues.size()); i++) {
                        var issue = issues.get(i);
                        sb.append("  #").append(issue.getId()).append(": ")
                          .append(issue.getTitle() != null ? 
                                  (issue.getTitle().length() > 60 ? 
                                   issue.getTitle().substring(0, 60) + "..." : 
                                   issue.getTitle()) : "No title")
                          .append("\n");
                    }
                } else {
                    sb.append("No issues match the current filter criteria.\n");
                    sb.append("Try adjusting the configuration or check if data exists in the database.\n");
                }
                
                return sb.toString();
            }
            
            @Override
            protected void done() {
                try {
                    statsArea.setText(get());
                } catch (Exception e) {
                    statsArea.setText("Error executing query:\n" + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
}
