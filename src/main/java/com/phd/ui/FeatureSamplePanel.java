package com.phd.ui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FeatureSamplePanel extends JPanel {
    private static final String CONFIG_PATH = "multilable-prediction/configs/quick_test.json";

    private JSONObject config;
    private JSONObject dataConfig;
    private JSONObject featureConfig;

    private JTextField datasetPathField;
    private JSpinner sampleSizeSpinner;
    private JSpinner testSizeSpinner;
    private JSpinner sampleRandomSpinner;
    private JCheckBox runBalancedBox;
    private JCheckBox runUnbalancedBox;
    private JSpinner targetCountSpinner;

    private JCheckBox useFeatureSelectionBox;
    private JSpinner topKSpinner;
    private JSpinner topKPlotSpinner;
    private JCheckBox wordCloudVocabularyBox;
    private JSpinner maxWordsSpinner;
    private JSpinner ngramMinSpinner;
    private JSpinner ngramMaxSpinner;
    private JSpinner minDfSpinner;
    private JCheckBox useIdfBox;

    public FeatureSamplePanel() {
        loadConfig();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
        applyModelToUi();
    }

    private void loadConfig() {
        try (FileReader reader = new FileReader(CONFIG_PATH)) {
            config = new JSONObject(new JSONTokener(reader));
        } catch (Exception e) {
            config = new JSONObject();
        }
        dataConfig = ensureObject(config, "data");
        featureConfig = ensureObject(config, "feature_engineering");
        ensureObject(featureConfig, "tfidf");
    }

    private JComponent createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 28, 38));
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("Feature & Sample Selection");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Control how data is sampled and which textual signals are used for learning.");
        subtitle.setForeground(new Color(200, 210, 220));
        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createBody() {
        JPanel columns = new JPanel(new GridBagLayout());
        columns.setBorder(new EmptyBorder(15, 0, 10, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.weightx = 0.5;
        columns.add(wrapWithBorder("Dataset Controls", createDatasetPanel()), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 0, 0);
        columns.add(wrapWithBorder("Feature Engineering", createFeaturePanel()), gbc);

        JPanel body = new JPanel(new BorderLayout(0, 15));
        body.add(createInfoStrip(), BorderLayout.NORTH);
        body.add(columns, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        return scrollPane;
    }

    private JComponent createInfoStrip() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setBackground(new Color(245, 247, 250));
        strip.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel info = new JLabel("Left: sampling strategy     |     Right: feature engineering knobs");
        info.setForeground(new Color(90, 98, 110));
        strip.add(info, BorderLayout.WEST);
        return strip;
    }

    private JPanel createDatasetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        datasetPathField = new JTextField(30);
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(this::handleBrowse);
        addRow(panel, row++, "Dataset Path", buildFieldWithButton(datasetPathField, browseButton));

        sampleSizeSpinner = createIntSpinner(100, 10, 100000, 10);
        addRow(panel, row++, "Sample Size", sampleSizeSpinner);

        sampleRandomSpinner = createIntSpinner(42, 0, 100000, 1);
        addRow(panel, row++, "Sample Random State", sampleRandomSpinner);

        testSizeSpinner = createDoubleSpinner(0.2, 0.05, 0.9, 0.05, "0.00");
        addRow(panel, row++, "Test Split", testSizeSpinner);

        runBalancedBox = new JCheckBox("Generate balanced dataset");
        runUnbalancedBox = new JCheckBox("Keep original imbalance");
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        togglePanel.add(runBalancedBox);
        togglePanel.add(runUnbalancedBox);
        addRow(panel, row++, "Runs", togglePanel);

        targetCountSpinner = createIntSpinner(600, 100, 10000, 50);
        addRow(panel, row, "Target count for balancing", targetCountSpinner);
        return panel;
    }

    private JPanel createFeaturePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        useFeatureSelectionBox = new JCheckBox("Enable feature selection");
        addFullRow(panel, row++, useFeatureSelectionBox);

        topKSpinner = createIntSpinner(50, 5, 2000, 5);
        addRow(panel, row++, "Top K features (training)", topKSpinner);

        topKPlotSpinner = createIntSpinner(20, 5, 200, 5);
        addRow(panel, row++, "Top K for plots", topKPlotSpinner);

        wordCloudVocabularyBox = new JCheckBox("Generate label-specific vocabulary");
        addFullRow(panel, row++, wordCloudVocabularyBox);

        maxWordsSpinner = createIntSpinner(50, 5, 500, 5);
        addRow(panel, row++, "Max words per label", maxWordsSpinner);

        JPanel ngramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ngramMinSpinner = createIntSpinner(1, 1, 5, 1);
        ngramMaxSpinner = createIntSpinner(2, 1, 5, 1);
        ngramPanel.add(new JLabel("Min"));
        ngramPanel.add(ngramMinSpinner);
        ngramPanel.add(new JLabel("Max"));
        ngramPanel.add(ngramMaxSpinner);
        addRow(panel, row++, "TF-IDF N-gram range", ngramPanel);

        minDfSpinner = createIntSpinner(1, 1, 20, 1);
        addRow(panel, row++, "Minimum document frequency", minDfSpinner);

        useIdfBox = new JCheckBox("Use inverse document frequency");
        addFullRow(panel, row, useIdfBox);
        return panel;
    }

    private JPanel buildFieldWithButton(JTextField field, JButton button) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 0));
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(button, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel createActionBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetButton = new JButton("Reset");
        JButton previewButton = new JButton("Preview JSON");
        JButton saveButton = new JButton("Save");
        resetButton.addActionListener(this::handleReset);
        previewButton.addActionListener(this::handlePreview);
        saveButton.addActionListener(this::handleSave);
        panel.add(resetButton);
        panel.add(previewButton);
        panel.add(saveButton);
        return panel;
    }

    private void handleBrowse(ActionEvent actionEvent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select dataset file");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            datasetPathField.setText(selected.getPath());
        }
    }

    private void handleReset(ActionEvent event) {
        loadConfig();
        applyModelToUi();
    }

    private void handlePreview(ActionEvent event) {
        persistUiToModel();
        JTextArea textArea = new JTextArea(config.toString(2));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(650, 450));
        JOptionPane.showMessageDialog(this, scrollPane, "Configuration Preview", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleSave(ActionEvent event) {
        persistUiToModel();
        try (FileWriter writer = new FileWriter(CONFIG_PATH)) {
            writer.write(config.toString(2));
            JOptionPane.showMessageDialog(this, "Configuration saved successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to save configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyModelToUi() {
        datasetPathField.setText(dataConfig.optString("dataset_path", ""));
        sampleSizeSpinner.setValue(dataConfig.optInt("sample_size", 100));
        sampleRandomSpinner.setValue(dataConfig.optInt("sample_random_state", 42));
        testSizeSpinner.setValue(dataConfig.optDouble("test_size", 0.2));
        runBalancedBox.setSelected(dataConfig.optBoolean("run_balanced", true));
        runUnbalancedBox.setSelected(dataConfig.optBoolean("run_unbalanced", true));
        targetCountSpinner.setValue(dataConfig.optInt("balanced_target_count", 600));

        useFeatureSelectionBox.setSelected(featureConfig.optBoolean("use_feature_selection", true));
        topKSpinner.setValue(featureConfig.optInt("top_k", 50));
        topKPlotSpinner.setValue(featureConfig.optInt("top_k_plot", 20));
        wordCloudVocabularyBox.setSelected(featureConfig.optBoolean("use_wordcloud_vocabulary", true));
        maxWordsSpinner.setValue(featureConfig.optInt("max_words_per_label", 50));

        JSONObject tfidf = ensureObject(featureConfig, "tfidf");
        JSONArray ngramRange = tfidf.optJSONArray("ngram_range");
        int minN = ngramRange != null && ngramRange.length() > 0 ? ngramRange.getInt(0) : 1;
        int maxN = ngramRange != null && ngramRange.length() > 1 ? ngramRange.getInt(1) : 2;
        ngramMinSpinner.setValue(minN);
        ngramMaxSpinner.setValue(Math.max(minN, maxN));
        minDfSpinner.setValue(tfidf.optInt("min_df", 1));
        useIdfBox.setSelected(tfidf.optBoolean("use_idf", true));
    }

    private void persistUiToModel() {
        dataConfig.put("dataset_path", datasetPathField.getText().trim());
        dataConfig.put("sample_size", getInt(sampleSizeSpinner));
        dataConfig.put("sample_random_state", getInt(sampleRandomSpinner));
        dataConfig.put("test_size", getDouble(testSizeSpinner));
        dataConfig.put("run_balanced", runBalancedBox.isSelected());
        dataConfig.put("run_unbalanced", runUnbalancedBox.isSelected());
        dataConfig.put("balanced_target_count", getInt(targetCountSpinner));

        featureConfig.put("use_feature_selection", useFeatureSelectionBox.isSelected());
        featureConfig.put("top_k", getInt(topKSpinner));
        featureConfig.put("top_k_plot", getInt(topKPlotSpinner));
        featureConfig.put("use_wordcloud_vocabulary", wordCloudVocabularyBox.isSelected());
        featureConfig.put("max_words_per_label", getInt(maxWordsSpinner));

        JSONObject tfidf = ensureObject(featureConfig, "tfidf");
        int minRange = getInt(ngramMinSpinner);
        int maxRange = Math.max(minRange, getInt(ngramMaxSpinner));
        JSONArray range = new JSONArray();
        range.put(minRange);
        range.put(maxRange);
        tfidf.put("ngram_range", range);
        tfidf.put("min_df", getInt(minDfSpinner));
        tfidf.put("use_idf", useIdfBox.isSelected());
    }

    private JSONObject ensureObject(JSONObject parent, String key) {
        JSONObject existing = parent.optJSONObject(key);
        if (existing == null) {
            existing = new JSONObject();
            parent.put(key, existing);
        }
        return existing;
    }

    private int getInt(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private double getDouble(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private JPanel wrapWithBorder(String title, JComponent child) {
        JPanel wrapper = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        wrapper.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(10, 10, 10, 10)));
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    private void addRow(JPanel panel, int row, String label, JComponent component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_START;
        left.insets = new Insets(6, 0, 6, 12);
        panel.add(new JLabel(label + ":"), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(6, 0, 6, 0);
        panel.add(component, right);
    }

    private void addFullRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(6, 0, 6, 0);
        panel.add(component, gbc);
    }

    private JSpinner createIntSpinner(int value, int min, int max, int step) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
        return spinner;
    }

    private JSpinner createDoubleSpinner(double value, double min, double max, double step, String pattern) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.NumberEditor(spinner, pattern));
        return spinner;
    }
}
