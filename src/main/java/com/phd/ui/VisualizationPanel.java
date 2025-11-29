package com.phd.ui;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple UI to toggle visualization settings for multi-label and multi-class configs.
 */
public class VisualizationPanel extends JPanel {

    private static final String DEFAULT_MULTILABEL_CONFIG_PATH = "multilable-prediction/configs/quick_test.json";
    private static final String DEFAULT_MULTICLASS_CONFIG_PATH = "software-change-type-prediction-main/configs/quick_test_multiclass.json";

    private final String multiLabelConfigPath;
    private final String multiClassConfigPath;

    private JSONObject multiLabelConfig;
    private JSONObject multiClassConfig;

    private final Map<String, JCheckBox> mlChecks = new LinkedHashMap<>();
    private final Map<String, JCheckBox> mcChecks = new LinkedHashMap<>();

    public VisualizationPanel() {
        this(DEFAULT_MULTILABEL_CONFIG_PATH, DEFAULT_MULTICLASS_CONFIG_PATH);
    }

    public VisualizationPanel(String multiLabelConfigPath, String multiClassConfigPath) {
        this.multiLabelConfigPath = multiLabelConfigPath;
        this.multiClassConfigPath = multiClassConfigPath;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        loadConfigs();
        add(buildContent(), BorderLayout.CENTER);
    }

    private void loadConfigs() {
        multiLabelConfig = loadConfig(multiLabelConfigPath);
        multiClassConfig = loadConfig(multiClassConfigPath);
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
        panel.add(buildConfigPanel("Multi-label", multiLabelConfig, mlChecks, this::handleSaveMultiLabel, multiLabelConfigPath));
        panel.add(buildConfigPanel("Multi-class", multiClassConfig, mcChecks, this::handleSaveMultiClass, multiClassConfigPath));
        return panel;
    }

    private JPanel buildConfigPanel(String title, JSONObject config, Map<String, JCheckBox> checkMap, Runnable onSave, String path) {
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
        attachAutoSave(checkMap, () -> {
            persistValues(config, checkMap);
            saveConfig(path, config, title, false);
        });

        wrapper.add(grid, BorderLayout.CENTER);
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

    private void attachAutoSave(Map<String, JCheckBox> checks, Runnable saveAction) {
        // Save immediately whenever a checkbox is toggled so the JSON stays in sync with UI
        checks.values().forEach(box ->
            box.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                    saveAction.run();
                }
            })
        );
    }

    private void persistValues(JSONObject config, Map<String, JCheckBox> checks) {
        JSONObject vis = ensureObject(config, "visualizations");
        checks.forEach((key, box) -> vis.put(key, box.isSelected()));
    }

    private void handleSaveMultiLabel() {
        persistValues(multiLabelConfig, mlChecks);
        saveConfig(multiLabelConfigPath, multiLabelConfig, "Multi-label", true);
    }

    private void handleSaveMultiClass() {
        persistValues(multiClassConfig, mcChecks);
        saveConfig(multiClassConfigPath, multiClassConfig, "Multi-class", true);
    }

    private void saveConfig(String path, JSONObject config, String title, boolean showMessage) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(config.toString(2));
            if (showMessage && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, title + " visualization settings saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Unable to save " + title + " settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
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

    // Test hook: persist current checkboxes and save without dialogs
    void saveSilently() {
        persistValues(multiLabelConfig, mlChecks);
        saveConfig(multiLabelConfigPath, multiLabelConfig, "Multi-label", false);
        persistValues(multiClassConfig, mcChecks);
        saveConfig(multiClassConfigPath, multiClassConfig, "Multi-class", false);
    }
}
