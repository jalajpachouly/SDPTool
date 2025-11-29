package com.phd.ui;

import com.phd.config.ConfigManager;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple UI to toggle visualization settings for multi-label and multi-class configs.
 */
public class VisualizationPanel extends JPanel {

    private final Map<String, JCheckBox> mlChecks = new LinkedHashMap<>();
    private final Map<String, JCheckBox> mcChecks = new LinkedHashMap<>();

    public VisualizationPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(buildContent(), BorderLayout.CENTER);
    }

    private JComponent buildContent() {
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        
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
        attachAutoSave(checkMap, () -> {
            persistValues(config, checkMap);
            onSave.run();
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
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        persistValues(multiLabelConfig, mlChecks);
        try {
            ConfigManager.saveMultiLabel();
        } catch (Exception ex) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Unable to save Multi-label settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSaveMultiClass() {
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        persistValues(multiClassConfig, mcChecks);
        try {
            ConfigManager.saveMultiClass();
        } catch (Exception ex) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Unable to save Multi-class settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        
        persistValues(multiLabelConfig, mlChecks);
        persistValues(multiClassConfig, mcChecks);
        
        try {
            ConfigManager.saveMultiLabel();
            ConfigManager.saveMultiClass();
        } catch (Exception ex) {
            System.err.println("Failed to save visualization settings: " + ex.getMessage());
        }
    }
}
