// -*- coding: utf-8 -*-
package com.phd.ui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PredictionPanel extends JPanel {
    private static final Pattern RUN_TIMESTAMP_PATTERN = Pattern.compile("(\\d{8}_\\d{6})$");
    private static final DateTimeFormatter RUN_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter HUMAN_READABLE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
    private static PredictionPanel instance;
    private static final List<String> REPORT_DIRS = Arrays.asList(
            "multilable-prediction/output/reports",
            "software-change-type-prediction-main/output/reports"
    );
    private static final String MODELS_DIR = "multilable-prediction/models";
    private JPanel runsListPanel;
    private JPanel modelsListPanel;
    private JTabbedPane contentTabs;

    public PredictionPanel() {
        instance = this;
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Training Runs & Saved Models");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());
        buttonPanel.add(refreshButton);
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Create tabbed pane for Reports vs Models
        contentTabs = new JTabbedPane();
        
        // Training Runs Tab
        runsListPanel = new JPanel();
        runsListPanel.setLayout(new BoxLayout(runsListPanel, BoxLayout.Y_AXIS));
        runsListPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        JScrollPane runsScrollPane = new JScrollPane(runsListPanel);
        runsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        runsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentTabs.addTab("Training Runs", runsScrollPane);
        
        // Saved Models Tab
        modelsListPanel = new JPanel();
        modelsListPanel.setLayout(new BoxLayout(modelsListPanel, BoxLayout.Y_AXIS));
        modelsListPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        JScrollPane modelsScrollPane = new JScrollPane(modelsListPanel);
        modelsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        modelsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentTabs.addTab("Saved Models", modelsScrollPane);
        
        add(contentTabs, BorderLayout.CENTER);
        refreshAll();
    }

    public static void refreshPanel() {
        if (instance != null) {
            instance.refreshAll();
        }
    }
    
    private void refreshAll() {
        refreshRunsList();
        refreshModelsList();
    }

    private void refreshRunsList() {
        runsListPanel.removeAll();
        List<File> runDirs = new ArrayList<>();
        for (String dirPath : REPORT_DIRS) {
            File reportsDir = new File(dirPath);
            if (!reportsDir.exists() || !reportsDir.isDirectory()) {
                continue;
            }
            File[] dirs = reportsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                runDirs.addAll(Arrays.asList(dirs));
            }
        }
        if (runDirs.isEmpty()) {
            runsListPanel.add(new JLabel("No prediction runs were found."));
        } else {
            runDirs.sort(Comparator.comparingLong(File::lastModified).reversed());
            for (File runDir : runDirs) {
                RunMetadata meta = buildMetadata(runDir);
                runsListPanel.add(createRunEntry(meta));
                runsListPanel.add(Box.createVerticalStrut(12));
            }
        }
        runsListPanel.revalidate();
        runsListPanel.repaint();
    }
    
    private void refreshModelsList() {
        modelsListPanel.removeAll();
        File modelsDir = new File(MODELS_DIR);
        
        if (!modelsDir.exists() || !modelsDir.isDirectory()) {
            modelsListPanel.add(new JLabel("No saved models found. Train a model first to enable persistence."));
            modelsListPanel.revalidate();
            modelsListPanel.repaint();
            return;
        }
        
        File[] modelDirs = modelsDir.listFiles(File::isDirectory);
        if (modelDirs == null || modelDirs.length == 0) {
            modelsListPanel.add(new JLabel("No saved models found."));
        } else {
            List<File> sortedModelDirs = Arrays.asList(modelDirs);
            sortedModelDirs.sort(Comparator.comparingLong(File::lastModified).reversed());
            
            // Build all metadata
            List<ModelMetadata> allMetadata = new ArrayList<>();
            for (File modelDir : sortedModelDirs) {
                allMetadata.add(buildModelMetadata(modelDir));
            }
            
            // Determine best model based on selection metric
            String selectionMetric = getSelectionMetricFromConfig();
            ModelMetadata bestModel = determineBestModel(allMetadata, selectionMetric);
            
            // Display models
            for (ModelMetadata meta : allMetadata) {
                boolean isBest = (bestModel != null && meta.runId.equals(bestModel.runId));
                modelsListPanel.add(createModelEntry(meta, isBest, selectionMetric));
                modelsListPanel.add(Box.createVerticalStrut(12));
            }
        }
        modelsListPanel.revalidate();
        modelsListPanel.repaint();
    }

    private JPanel createRunEntry(RunMetadata metadata) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 223, 230)),
                new EmptyBorder(12, 14, 12, 14)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Left section: Run name and status
        JPanel leftSection = new JPanel();
        leftSection.setOpaque(false);
        leftSection.setLayout(new BoxLayout(leftSection, BoxLayout.Y_AXIS));
        
        JLabel nameLabel = new JLabel(metadata.displayName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 15f));
        
        JLabel statusLabel = new JLabel(metadata.status);
        statusLabel.setForeground(statusToColor(metadata.status));
        
        leftSection.add(nameLabel);
        leftSection.add(Box.createVerticalStrut(4));
        leftSection.add(statusLabel);
        
        // Center section: Details
        JPanel centerSection = new JPanel();
        centerSection.setOpaque(false);
        centerSection.setLayout(new BoxLayout(centerSection, BoxLayout.Y_AXIS));
        
        JLabel summaryLabel = new JLabel(buildSummaryLine(metadata));
        summaryLabel.setForeground(new Color(90, 98, 110));
        centerSection.add(summaryLabel);
        
        if (metadata.hyperSummary != null && !metadata.hyperSummary.isBlank()) {
            String hyperText = metadata.hyperSummary.trim();
            String shortText = hyperText.length() > 180 ? hyperText.substring(0, 177) + "..." : hyperText;
            JLabel hyperLabel = new JLabel("Key Params: " + shortText);
            if (hyperText.length() > shortText.length()) {
                hyperLabel.setToolTipText(hyperText);
            }
            hyperLabel.setForeground(new Color(110, 118, 130));
            hyperLabel.setFont(hyperLabel.getFont().deriveFont(Font.ITALIC, 11f));
            centerSection.add(Box.createVerticalStrut(6));
            centerSection.add(hyperLabel);
        }

        // Right section: Action buttons
        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        
        JButton openButton = new JButton("Open Report");
        openButton.setEnabled(metadata.hasReport);
        openButton.setPreferredSize(new Dimension(120, 28));
        openButton.setMaximumSize(new Dimension(120, 28));
        openButton.addActionListener(e -> openReport(metadata.runDir));

        JButton deleteButton = new JButton("Delete");
        deleteButton.setPreferredSize(new Dimension(120, 28));
        deleteButton.setMaximumSize(new Dimension(120, 28));
        deleteButton.addActionListener(e -> {
            deleteRun(metadata.runDir);
            refreshRunsList();
        });

        actionsPanel.add(openButton);
        actionsPanel.add(Box.createVerticalStrut(6));
        actionsPanel.add(deleteButton);
        actionsPanel.add(Box.createVerticalGlue());

        panel.add(leftSection, BorderLayout.WEST);
        panel.add(centerSection, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.EAST);
        return panel;
    }

    private String buildSummaryLine(RunMetadata metadata) {
        String timePart = metadata.formattedTimestamp != null ? metadata.formattedTimestamp : "Unknown time";
        String dataPart = "Data: " + String.join(", ", metadata.dataTypes);
        String modelPart = metadata.models.isEmpty()
                ? "Models: Not recorded"
                : "Models: " + String.join(", ", metadata.models);
        return timePart + "   |   " + dataPart + "   |   " + modelPart;
    }

    private RunMetadata buildMetadata(File runDir) {
        RunMetadata metadata = new RunMetadata();
        metadata.runDir = runDir;
        metadata.folderName = runDir.getName();
        metadata.displayName = metadata.folderName;
        metadata.hasReport = new File(runDir, "report.html").exists();
        boolean hasCompleteFlag = new File(runDir, "COMPLETE.flag").exists();
        metadata.status = hasCompleteFlag ? "Completed" : "Processing...";

        File metaFile = new File(runDir, "metadata.json");
        if (metaFile.exists()) {
            try (FileReader reader = new FileReader(metaFile)) {
                JSONObject json = new JSONObject(new JSONTokener(reader));
                metadata.displayName = json.optString("run_name", metadata.displayName);
                metadata.timestampRaw = json.optString("timestamp", null);
                metadata.formattedTimestamp = formatTimestamp(metadata.timestampRaw);
                metadata.dataTypes = readList(json.optJSONArray("data_types"));
                metadata.models = readList(json.optJSONArray("models"));
                JSONObject hyper = json.optJSONObject("hyperparameters");
                metadata.hyperSummary = buildHyperSummary(hyper);
                metadata.status = resolveStatus(json.optString("status", null), hasCompleteFlag);
            } catch (Exception ex) {
                metadata.formattedTimestamp = formatTimestamp(extractTimestamp(metadata.folderName));
            }
        } else {
            metadata.formattedTimestamp = formatTimestamp(extractTimestamp(metadata.folderName));
        }

        if (metadata.dataTypes.isEmpty()) {
            metadata.dataTypes = Collections.singletonList("Unknown");
        }
        if (metadata.formattedTimestamp == null) {
            metadata.formattedTimestamp = "Unknown time";
        }

        return metadata;
    }

    private List<String> readList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            values.add(array.optString(i));
        }
        return values;
    }

    private String buildHyperSummary(JSONObject hyperJson) {
        if (hyperJson == null) {
            return null;
        }
        List<String> sections = new ArrayList<>();
        Iterator<String> keys = hyperJson.keys();
        while (keys.hasNext()) {
            String dataType = keys.next();
            JSONArray entries = hyperJson.optJSONArray(dataType);
            if (entries == null || entries.length() == 0) {
                continue;
            }
            List<String> snippets = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                String model = entry.optString("model", "?");
                JSONObject params = entry.optJSONObject("parameters");
                String snippet = summarizeParams(params);
                snippets.add(snippet.isEmpty() ? model : model + "(" + snippet + ")");
                if (snippets.size() >= 3) {
                    break;
                }
            }
            if (!snippets.isEmpty()) {
                sections.add(dataType + ": " + String.join(", ", snippets));
            }
        }
        if (sections.isEmpty()) {
            return null;
        }
        return String.join(" | ", sections);
    }

    private String summarizeParams(JSONObject params) {
        if (params == null || params.length() == 0) {
            return "";
        }
        List<String> pieces = new ArrayList<>();
        Iterator<String> keys = params.keys();
        while (keys.hasNext() && pieces.size() < 3) {
            String key = keys.next();
            Object value = params.opt(key);
            pieces.add(key + "=" + value);
        }
        if (params.length() > pieces.size()) {
            pieces.add("...");
        }
        return String.join(", ", pieces);
    }

    private String extractTimestamp(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = RUN_TIMESTAMP_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String formatTimestamp(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(raw, RUN_NAME_FORMATTER);
            return dateTime.format(HUMAN_READABLE_FORMAT);
        } catch (DateTimeParseException ex) {
            return raw;
        }
    }

    private void handleOpenLatestReport() {
        File latestReport = findLatestReportDir();
        if (latestReport == null) {
            JOptionPane.showMessageDialog(this, "No reports found.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openReport(latestReport);
    }

    private void handleDeleteLatestReport() {
        File latestReport = findLatestReportDir();
        if (latestReport == null) {
            JOptionPane.showMessageDialog(this, "No reports found to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete report: " + latestReport.getName() + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            deleteRun(latestReport);
            JOptionPane.showMessageDialog(this, "Report deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshRunsList();
        }
    }

    private File findLatestReportDir() {
        File latestDir = null;
        long latestTime = 0;

        for (String dirPath : REPORT_DIRS) {
            File reportsDir = new File(dirPath);
            if (!reportsDir.exists() || !reportsDir.isDirectory()) {
                continue;
            }
            File[] dirs = reportsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    long modified = dir.lastModified();
                    if (modified > latestTime) {
                        latestTime = modified;
                        latestDir = dir;
                    }
                }
            }
        }
        return latestDir;
    }

    private void openReport(File runDir) {
        File htmlFile = new File(runDir, "report.html");
        if (htmlFile.exists()) {
            try {
                Desktop.getDesktop().browse(htmlFile.toURI());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to open report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Fallback to log.txt if report.html doesn't exist
            File logFile = new File(runDir, "log.txt");
            if (logFile.exists()) {
                try {
                    Desktop.getDesktop().open(logFile);  // Use open() for text files
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to open log: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Neither report.html nor log.txt found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteRun(File runDir) {
        try {
            Files.walk(runDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete run: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File findReportForModel(String runId) {
        // Try to find a training report matching this model's run ID
        for (String dirPath : REPORT_DIRS) {
            File reportsBaseDir = new File(dirPath);
            if (!reportsBaseDir.exists()) continue;
            
            // Look for exact match first
            File exactMatch = new File(reportsBaseDir, runId);
            if (exactMatch.exists() && (new File(exactMatch, "report.html").exists() || new File(exactMatch, "log.txt").exists())) {
                return exactMatch;
            }
            
            // Look for partial match (runId might be part of directory name)
            File[] dirs = reportsBaseDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.getName().contains(runId) && (new File(dir, "report.html").exists() || new File(dir, "log.txt").exists())) {
                        return dir;
                    }
                }
            }
        }
        return null;
    }

    private String resolveStatus(String statusValue, boolean hasCompleteFlag) {
        if (hasCompleteFlag) {
            return "Completed";
        }
        String normalized = statusValue == null ? "" : statusValue.trim().toLowerCase();
        switch (normalized) {
            case "complete":
            case "completed":
            case "done":
                return "Completed";
            case "failed":
            case "error":
            case "stopped":
                return "Failed";
            case "running":
            case "processing":
            case "in_progress":
            case "in-progress":
                return "Processing...";
            default:
                return "Processing...";
        }
    }

    private Color statusToColor(String status) {
        if (status == null) {
            return new Color(90, 98, 110);
        }
        String normalized = status.toLowerCase();
        if (normalized.contains("complete")) {
            return new Color(34, 139, 34);
        }
        if (normalized.contains("fail") || normalized.contains("error")) {
            return new Color(178, 34, 34);
        }
        if (normalized.contains("process") || normalized.contains("run")) {
            return new Color(204, 102, 0);
        }
        return new Color(90, 98, 110);
    }
    
    private ModelMetadata buildModelMetadata(File modelDir) {
        ModelMetadata metadata = new ModelMetadata();
        metadata.modelDir = modelDir;
        metadata.runId = modelDir.getName();
        metadata.displayName = metadata.runId;
        
        File metaFile = new File(modelDir, "metadata.json");
        if (metaFile.exists()) {
            try (FileReader reader = new FileReader(metaFile)) {
                JSONObject json = new JSONObject(new JSONTokener(reader));
                metadata.modelName = json.optString("model_name", "Unknown");
                metadata.modelType = json.optString("model_type", "Unknown");
                metadata.savedAt = json.optString("saved_at", null);
                
                JSONObject metricsJson = json.optJSONObject("metrics");
                if (metricsJson != null) {
                    metadata.macroF1 = metricsJson.optDouble("macro_f1", 0.0);
                    metadata.macroRecall = metricsJson.optDouble("macro_recall", 0.0);
                    metadata.hammingLoss = metricsJson.optDouble("hamming_loss", 0.0);
                }
            } catch (Exception ex) {
                // Ignore, use defaults
            }
        }
        
        return metadata;
    }
    
    private String getSelectionMetricFromConfig() {
        // Fixed criterion: Always use CV Mean F1
        return "cv_mean_f1";
    }
    
    private ModelMetadata determineBestModel(List<ModelMetadata> models, String selectionMetric) {
        if (models.isEmpty()) {
            return null;
        }
        
        ModelMetadata best = models.get(0);
        
        // Fixed criterion: Always use Macro F1 (higher is better)
        for (ModelMetadata model : models) {
            if (model.macroF1 > best.macroF1) {
                best = model;
            }
        }
        
        return best;
    }
    
    private JPanel createModelEntry(ModelMetadata metadata, boolean isBest, String selectionMetric) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240)),
                new EmptyBorder(12, 14, 12, 14)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Left section: Model name and type
        JPanel leftSection = new JPanel();
        leftSection.setOpaque(false);
        leftSection.setLayout(new BoxLayout(leftSection, BoxLayout.Y_AXIS));
        
        JLabel nameLabel = new JLabel(metadata.modelName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 15f));
        nameLabel.setForeground(new Color(25, 118, 210));
        
        // Add "BEST MODEL" badge if this is the best model
        if (isBest) {
            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            nameRow.setOpaque(false);
            nameRow.add(nameLabel);
            
            JLabel bestBadge = new JLabel("BEST");
            bestBadge.setFont(bestBadge.getFont().deriveFont(Font.BOLD, 11f));
            bestBadge.setForeground(Color.WHITE);
            bestBadge.setBackground(new Color(76, 175, 80)); // Green
            bestBadge.setOpaque(true);
            bestBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 142, 60), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            nameRow.add(bestBadge);
            
            leftSection.add(nameRow);
        } else {
            leftSection.add(nameLabel);
        }
        
        JLabel typeLabel = new JLabel(metadata.modelType);
        typeLabel.setForeground(new Color(90, 98, 110));
        
        JLabel runIdLabel = new JLabel("Run ID: " + metadata.runId);
        runIdLabel.setFont(runIdLabel.getFont().deriveFont(Font.ITALIC, 11f));
        runIdLabel.setForeground(new Color(130, 138, 150));
        
        leftSection.add(nameLabel);
        leftSection.add(Box.createVerticalStrut(4));
        leftSection.add(typeLabel);
        leftSection.add(Box.createVerticalStrut(4));
        leftSection.add(runIdLabel);
        
        // Center section: Metrics
        JPanel centerSection = new JPanel();
        centerSection.setOpaque(false);
        centerSection.setLayout(new BoxLayout(centerSection, BoxLayout.Y_AXIS));
        
        JLabel metricsTitle = new JLabel("Performance Metrics:");
        metricsTitle.setFont(metricsTitle.getFont().deriveFont(Font.BOLD, 12f));
        centerSection.add(metricsTitle);
        centerSection.add(Box.createVerticalStrut(4));
        
        centerSection.add(new JLabel(String.format("Macro F1: %.4f", metadata.macroF1)));
        centerSection.add(new JLabel(String.format("Macro Recall: %.4f", metadata.macroRecall)));
        centerSection.add(new JLabel(String.format("Hamming Loss: %.4f", metadata.hammingLoss)));
        
        if (isBest) {
            centerSection.add(Box.createVerticalStrut(4));
            JLabel bestByLabel = new JLabel("(Best by: CV Mean F1)");
            bestByLabel.setFont(bestByLabel.getFont().deriveFont(Font.ITALIC, 11f));
            bestByLabel.setForeground(new Color(76, 175, 80));
            centerSection.add(bestByLabel);
        }
        
        // Right section: Action buttons
        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        
        JButton predictButton = new JButton("Run Prediction");
        predictButton.setPreferredSize(new Dimension(140, 28));
        predictButton.setMaximumSize(new Dimension(140, 28));
        predictButton.addActionListener(e -> showPredictionDialog(metadata));

        JButton deleteButton = new JButton("Delete Model");
        deleteButton.setPreferredSize(new Dimension(140, 28));
        deleteButton.setMaximumSize(new Dimension(140, 28));
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete model '" + metadata.runId + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteModel(metadata.modelDir);
                refreshModelsList();
            }
        });

        actionsPanel.add(predictButton);
        actionsPanel.add(Box.createVerticalStrut(6));
        actionsPanel.add(deleteButton);
        actionsPanel.add(Box.createVerticalGlue());

        panel.add(leftSection, BorderLayout.WEST);
        panel.add(centerSection, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.EAST);
        return panel;
    }
    
    private void showPredictionDialog(ModelMetadata metadata) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Run Prediction", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        int row = 0;
        
        // Model info
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel modelLabel = new JLabel("Model: " + metadata.modelName + " (" + metadata.runId + ")");
        modelLabel.setFont(modelLabel.getFont().deriveFont(Font.BOLD));
        contentPanel.add(modelLabel, gbc);
        
        gbc.gridwidth = 1;
        
        // Prediction mode - Only "rows" option visible for now
        gbc.gridx = 0; gbc.gridy = row;
        contentPanel.add(new JLabel("Prediction Mode:"), gbc);
        gbc.gridx = 1;
        // Note: Interactive and CSV modes are disabled in UI (but still available in Python script)
        JComboBox<String> modeCombo = new JComboBox<>(new String[]{"rows"});
        modeCombo.setEnabled(false); // Only one option, so disable dropdown
        contentPanel.add(modeCombo, gbc);
        row++;
        
        // Row numbers (for row mode)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel rowNumbersLabel = new JLabel("Row Numbers (comma-separated):");
        contentPanel.add(rowNumbersLabel, gbc);
        gbc.gridx = 1;
        JTextField rowNumbersField = new JTextField("0,5,10");
        contentPanel.add(rowNumbersField, gbc);
        row++;
        
        // Hidden/disabled fields for future use
        JTextField inputFileField = new JTextField();
        inputFileField.setVisible(false);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton runButton = new JButton("Run");
        JButton cancelButton = new JButton("Cancel");
        
        runButton.addActionListener(e -> {
            String mode = (String) modeCombo.getSelectedItem();
            String inputFile = inputFileField.getText().trim();
            String rowNumbers = rowNumbersField.getText().trim();
            
            dialog.dispose();
            executePrediction(metadata.runId, mode, inputFile, rowNumbers);
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(runButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void executePrediction(String runId, String mode, String inputFile, String rowNumbers) {
        // Use venv Python interpreter if available
        Path venvPython = Paths.get("venv/Scripts/python.exe").toAbsolutePath();
        String pythonExe = Files.exists(venvPython) ? venvPython.toString() : "python";
        Path pythonScript = Paths.get("multilable-prediction/src/predict_with_model.py").toAbsolutePath();
        
        // Validate script exists
        if (!Files.exists(pythonScript)) {
            JOptionPane.showMessageDialog(this,
                "Python script not found: " + pythonScript,
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create log directory
        Path logDir = Paths.get("multilable-prediction/output/prediction_logs");
        try {
            Files.createDirectories(logDir);
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
        
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        Path logFile = logDir.resolve("prediction_" + runId + "_" + timestamp + ".log");
        
        // Build command as List to avoid quote escaping issues
        List<String> command = new ArrayList<>();
        command.add(pythonExe);
        command.add(pythonScript.toString());
        command.add(runId);
        command.add("--mode");
        command.add(mode);
        command.add("--json"); // Request JSON output
        
        if ("csv".equals(mode) && !inputFile.isEmpty()) {
            command.add("--input");
            command.add(inputFile);
        } else if ("rows".equals(mode) && !rowNumbers.isEmpty()) {
            // Add dataset argument first
            Path datasetPath = Paths.get("multilable-prediction/data/dataset.csv").toAbsolutePath();
            command.add("--dataset");
            command.add(datasetPath.toString());
            // Add rows argument with comma-separated values
            command.add("--rows");
            command.add(rowNumbers.trim());
        }
        
        // Execute and capture output
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);
        
        // Show progress dialog
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Running Prediction", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString("Running prediction...");
        progressBar.setStringPainted(true);
        JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
        progressPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        progressPanel.add(new JLabel("Please wait while the model makes predictions..."), BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressDialog.add(progressPanel);
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        // Run prediction in background thread
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            private StringBuilder fullLog = new StringBuilder();
            
            @Override
            protected String doInBackground() throws Exception {
                // Log command execution
                fullLog.append("=".repeat(80)).append("\n");
                fullLog.append("PREDICTION EXECUTION LOG\n");
                fullLog.append("=".repeat(80)).append("\n");
                fullLog.append("Timestamp: ").append(timestamp).append("\n");
                fullLog.append("Run ID: ").append(runId).append("\n");
                fullLog.append("Mode: ").append(mode).append("\n");
                if ("rows".equals(mode)) {
                    fullLog.append("Rows: ").append(rowNumbers).append("\n");
                } else if ("csv".equals(mode)) {
                    fullLog.append("Input File: ").append(inputFile).append("\n");
                }
                fullLog.append("Python Executable: ").append(pythonExe).append("\n");
                fullLog.append("Command: ").append(String.join(" ", command)).append("\n");
                fullLog.append("=".repeat(80)).append("\n\n");
                
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder output = new StringBuilder();
                String line;
                boolean inJson = false;
                
                fullLog.append("PYTHON OUTPUT:\n");
                fullLog.append("-".repeat(80)).append("\n");
                
                while ((line = reader.readLine()) != null) {
                    fullLog.append(line).append("\n"); // Log everything
                    
                    if (line.contains("JSON_OUTPUT_START")) {
                        inJson = true;
                        continue;
                    } else if (line.contains("JSON_OUTPUT_END")) {
                        break;
                    }
                    
                    if (inJson) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                fullLog.append("-".repeat(80)).append("\n");
                fullLog.append("Process exit code: ").append(exitCode).append("\n");
                fullLog.append("=".repeat(80)).append("\n");
                
                // Write log to file
                try {
                    Files.write(logFile, fullLog.toString().getBytes());
                    System.out.println("Prediction log saved to: " + logFile);
                } catch (Exception e) {
                    System.err.println("Failed to write log file: " + e.getMessage());
                }
                
                return output.toString();
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    String jsonOutput = get();
                    
                    if (jsonOutput.isEmpty()) {
                        String errorMsg = "No prediction results received from Python script.\n\n" +
                                        "Log file: " + logFile.toString();
                        JOptionPane.showMessageDialog(PredictionPanel.this,
                            errorMsg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Parse JSON and show dialog
                    JSONObject jsonResults = new JSONObject(jsonOutput);
                    Map<String, Object> resultsMap = jsonToMap(jsonResults);
                    
                    PredictionResultDialog dialog = new PredictionResultDialog(
                        (Frame) SwingUtilities.getWindowAncestor(PredictionPanel.this),
                        resultsMap
                    );
                    dialog.setVisible(true);
                    
                    // Show log location in console
                    System.out.println("Prediction completed successfully. Log: " + logFile);
                    
                } catch (Exception ex) {
                    String errorMsg = "Failed to parse prediction results: " + ex.getMessage() + "\n\n" +
                                    "Log file: " + logFile.toString();
                    JOptionPane.showMessageDialog(PredictionPanel.this,
                        errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true); // Blocks until worker completes
    }
    
    private void deleteModel(File modelDir) {
        try {
            Files.walk(modelDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
            JOptionPane.showMessageDialog(this, "Model deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete model: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Convert JSONObject to Map recursively
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            
            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else {
                map.put(key, value);
            }
        }
        
        return map;
    }
    
    /**
     * Convert JSONArray to List recursively
     */
    @SuppressWarnings("unchecked")
    private List<Object> jsonArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            
            if (value instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonArrayToList((JSONArray) value));
            } else {
                list.add(value);
            }
        }
        
        return list;
    }

    private static class RunMetadata {
        private File runDir;
        private String folderName;
        private String displayName;
        private String timestampRaw;
        private String formattedTimestamp;
        private List<String> dataTypes = new ArrayList<>();
        private List<String> models = new ArrayList<>();
        private boolean hasReport;
        private String status;
        private String hyperSummary;
    }
    
    private static class ModelMetadata {
        private File modelDir;
        private String runId;
        private String displayName;
        private String modelName;
        private String modelType;
        private String savedAt;
        private double macroF1;
        private double macroRecall;
        private double hammingLoss;
    }
}
