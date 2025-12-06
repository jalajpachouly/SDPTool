package com.phd.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dialog for displaying prediction results in a readable format
 */
public class PredictionResultDialog extends JDialog {
    
    private JPanel resultsPanel;
    private JScrollPane mainScrollPane;
    
    public PredictionResultDialog(Frame owner, Map<String, Object> results) {
        super(owner, "Prediction Results", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(owner);
        
        initComponents(results);
    }
    
    private void initComponents(Map<String, Object> results) {
        setLayout(new BorderLayout(5, 5));
        
        // Header Panel with Summary - Fixed height, no scrollbar
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 5, 10),
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Summary",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
            )
        ));
        headerPanel.setBackground(new Color(245, 250, 255));
        
        // Add summary labels in rows
        addSummaryRow(headerPanel, "Model:", results.getOrDefault("model_name", "Unknown").toString());
        addSummaryRow(headerPanel, "Model Type:", results.getOrDefault("model_type", "Unknown").toString());
        addSummaryRow(headerPanel, "Run ID:", results.getOrDefault("run_id", "Unknown").toString());
        addSummaryRow(headerPanel, "Samples:", results.getOrDefault("num_samples", 0).toString());
        
        if (results.containsKey("source")) {
            addSummaryRow(headerPanel, "Source:", results.get("source").toString());
        }
        
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        headerPanel.setPreferredSize(new Dimension(980, 120));
        
        headerPanel.setPreferredSize(new Dimension(980, 90));
        
        // Main Results Panel - Cards for each prediction
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        populateResults(resultsPanel, results);
        
        // Wrap in scroll pane
        mainScrollPane = new JScrollPane(resultsPanel);
        mainScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 10, 5, 10),
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Prediction Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
            )
        ));
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Button Panel - Fixed at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        buttonPanel.setPreferredSize(new Dimension(980, 50));
        
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.setPreferredSize(new Dimension(100, 32));
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(closeButton);
        
        // Add components
        add(headerPanel, BorderLayout.NORTH);
        add(mainScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void addSummaryRow(JPanel panel, String label, String value) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        rowPanel.setBackground(new Color(245, 250, 255));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 11));
        labelComp.setPreferredSize(new Dimension(80, 20));
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        rowPanel.add(labelComp);
        rowPanel.add(valueComp);
        panel.add(rowPanel);
    }
    
    @SuppressWarnings("unchecked")
    private void populateResults(JPanel panel, Map<String, Object> results) {
        List<Map<String, Object>> predictions = (List<Map<String, Object>>) results.get("predictions");
        
        if (predictions == null || predictions.isEmpty()) {
            JLabel noData = new JLabel("No predictions available");
            noData.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            noData.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(noData);
            return;
        }
        
        for (int i = 0; i < predictions.size(); i++) {
            Map<String, Object> pred = predictions.get(i);
            JPanel card = createPredictionCard(pred, i + 1);
            panel.add(card);
            
            if (i < predictions.size() - 1) {
                panel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private JPanel createPredictionCard(Map<String, Object> pred, int sampleNum) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(950, 300));
        
        // Header with sample info
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerPanel.setBackground(new Color(240, 245, 250));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel sampleLabel = new JLabel("Sample #" + sampleNum);
        sampleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sampleLabel.setForeground(new Color(0, 51, 102));
        headerPanel.add(sampleLabel);
        
        if (pred.containsKey("dataset_row")) {
            JLabel rowLabel = new JLabel("│ Dataset Row: " + pred.get("dataset_row"));
            rowLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            headerPanel.add(rowLabel);
        }
        
        // Get exact match status for header
        if (pred.containsKey("exact_match")) {
            boolean exactMatch = (Boolean) pred.get("exact_match");
            JLabel matchLabel = new JLabel(exactMatch ? "| Exact Match" : "| No Match");
            matchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            matchLabel.setForeground(exactMatch ? new Color(0, 128, 0) : new Color(200, 0, 0));
            headerPanel.add(matchLabel);
        }
        
        // Text area - with wrapping
        String text = pred.getOrDefault("text", "N/A").toString();
        JTextArea textArea = new JTextArea(text);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Text"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        textArea.setRows(3);
        
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScroll.setPreferredSize(new Dimension(920, 80));
        
        // Info panel with predictions
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 15, 8));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Predicted labels
        List<String> predictedLabels = (List<String>) pred.get("predicted_labels");
        String predictedStr = predictedLabels != null && !predictedLabels.isEmpty() 
                ? String.join(", ", predictedLabels) 
                : "None";
        addInfoRow(infoPanel, "Predicted:", predictedStr, false);
        
        // Ground truth
        if (pred.containsKey("ground_truth")) {
            List<String> groundTruth = (List<String>) pred.get("ground_truth");
            String groundTruthStr = groundTruth != null && !groundTruth.isEmpty() 
                    ? String.join(", ", groundTruth) 
                    : "None";
            addInfoRow(infoPanel, "Ground Truth:", groundTruthStr, false);
        }
        
        // Confidence scores
        Map<String, Object> confidenceScores = (Map<String, Object>) pred.get("confidence_scores");
        if (confidenceScores != null && !confidenceScores.isEmpty()) {
            StringBuilder confStr = new StringBuilder();
            confidenceScores.forEach((label, score) -> {
                double scoreVal = ((Number) score).doubleValue();
                confStr.append(String.format("%s: %.1f%%, ", label, scoreVal * 100));
            });
            if (confStr.length() > 2) {
                confStr.setLength(confStr.length() - 2); // Remove trailing comma
            }
            addInfoRow(infoPanel, "Confidence:", confStr.toString(), false);
        }
        
        // Sample accuracy
        if (pred.containsKey("sample_accuracy")) {
            double accuracy = ((Number) pred.get("sample_accuracy")).doubleValue();
            addInfoRow(infoPanel, "Accuracy:", String.format("%.2f%%", accuracy * 100), true);
        }
        
        // Assemble card
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(textScroll, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private void addInfoRow(JPanel panel, String label, String value, boolean highlight) {
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 11));
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        if (highlight) {
            valueComp.setForeground(new Color(0, 102, 204));
            valueComp.setFont(valueComp.getFont().deriveFont(Font.BOLD));
        }
        
        panel.add(labelComp);
        panel.add(valueComp);
    }
}
