package com.phd.ui;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple UI to toggle visualization settings for multi-label and multi-class configs.
 */
public class VisualizationPanel extends JPanel {

    private static final String MULTILABEL_CONFIG_PATH = "multilable-prediction/configs/quick_test.json";
    private static final String MULTICLASS_CONFIG_PATH = "software-change-type-prediction-main/configs/quick_test_multiclass.json";

    private JSONObject multiLabelConfig;
    private JSONObject multiClassConfig;

    private final Map<String, JCheckBox> mlChecks = new LinkedHashMap<>();
    private final Map<String, JCheckBox> mcChecks = new LinkedHashMap<>();

    public VisualizationPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        loadConfigs();
        add(buildContent(), BorderLayout.CENTER);
    }

    private void loadConfigs() {
        multiLabelConfig = loadConfig(MULTILABEL_CONFIG_PATH);
        multiClassConfig = loadConfig(MULTICLASS_CONFIG_PATH);
    }

    private JSONObject loadConfig(String path) {
        try (FileReader reader = new FileReader(path)) {
            return new JSONObject(new JSONTokener(reader));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JComponent buildContent() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 12));
        panel.add(buildConfigPanel("Multi-label", multiLabelConfig, mlChecks, this::handleSaveMultiLabel));
        panel.add(buildConfigPanel("Multi-class", multiClassConfig, mcChecks, this::handleSaveMultiClass));
        return panel;
    }

    private JPanel buildConfigPanel(String title, JSONObject config, Map<String, JCheckBox> checkMap, Runnable onSave) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new TitledBorder(title));

        JPanel grid = new JPanel(new GridLayout(0, 1, 6, 6));
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));

        addCheck(grid, checkMap, "enabled", "Enable visualizations");
        addCheck(grid, checkMap, "description_length", "Description length distribution");
        addCheck(grid, checkMap, "class_distribution", "Class distribution");
        addCheck(grid, checkMap, "correlation_matrix", "Correlation matrix");
        addCheck(grid, checkMap, "label_frequency", "Label frequency / trends");
        addCheck(grid, checkMap, "word_clouds", "Word clouds");
        addCheck(grid, checkMap, "top_features", "Top features (Chi2)");
        addCheck(grid, checkMap, "f1_scores", "F1 score boxplot");
        addCheck(grid, checkMap, "all_metrics_boxplot", "All-metrics boxplot");
        addCheck(grid, checkMap, "nb_metrics", "NB metrics chart");

        applyValues(config, checkMap);

        JButton saveButton = new JButton("Save " + title + " visuals");
        saveButton.addActionListener(e -> onSave.run());

        wrapper.add(grid, BorderLayout.CENTER);
        wrapper.add(saveButton, BorderLayout.SOUTH);
        return wrapper;
    }

    private void addCheck(JPanel panel, Map<String, JCheckBox> map, String key, String label) {
        JCheckBox box = new JCheckBox(label);
        map.put(key, box);
        panel.add(box);
    }

    private void applyValues(JSONObject config, Map<String, JCheckBox> checks) {
        JSONObject vis = ensureObject(config, "visualizations");
        checks.forEach((key, box) -> box.setSelected(vis.optBoolean(key, true)));
    }

    private void persistValues(JSONObject config, Map<String, JCheckBox> checks) {
        JSONObject vis = ensureObject(config, "visualizations");
        checks.forEach((key, box) -> vis.put(key, box.isSelected()));
    }

    private void handleSaveMultiLabel() {
        persistValues(multiLabelConfig, mlChecks);
        saveConfig(MULTILABEL_CONFIG_PATH, multiLabelConfig, "Multi-label");
    }

    private void handleSaveMultiClass() {
        persistValues(multiClassConfig, mcChecks);
        saveConfig(MULTICLASS_CONFIG_PATH, multiClassConfig, "Multi-class");
    }

    private void saveConfig(String path, JSONObject config, String title) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(config.toString(2));
            JOptionPane.showMessageDialog(this, title + " visualization settings saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to save " + title + " settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JSONObject ensureObject(JSONObject parent, String key) {
        JSONObject child = parent.optJSONObject(key);
        if (child == null) {
            child = new JSONObject();
            parent.put(key, child);
        }
        return child;
    }
}
