
package com.phd.ui;

import com.phd.config.ConfigManager;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AITechniquePanel extends JPanel {
    public static final String DEFAULT_MULTILABEL_SCRIPT_PATH = "multilable-prediction/src/main.py";
    public static final String DEFAULT_MULTICLASS_SCRIPT_PATH = "software-change-type-prediction-main/src/configurable_main.py";
    // Note: Actual paths will be determined at runtime based on experiment_name from ui_config.json
    public static final String MULTILABEL_OUTPUT_BASE = "multilable-prediction/output/reports";
    public static final String MULTILABEL_LOG_FILENAME = "log.txt";
    public static final String MULTILABEL_REPORT_FILENAME = "report.html";
    private static final String PROBLEM_MULTI_LABEL = "multi_label";
    private static final String PROBLEM_MULTI_CLASS = "multi_class";

    private final String multiLabelScriptPath;
    private final String multiClassScriptPath;

    private JSONObject multiLabelModels;
    private JSONObject multiClassModels;
    
    private VisualizationPanel visualizationPanel;
    private FeatureSamplePanel featureSamplePanel;

    private JTabbedPane problemTabs;
    private JRadioButton mlRadio;
    private JRadioButton mcRadio;
    private JProgressBar runProgressBar;
    private JProgressBar taskProgressBar;
    private JLabel runStatusLabel;
    private JLabel taskStatusLabel;

    // Multi-label controls
    private JCheckBox mlRandomForestEnabledBox;
    private JCheckBox mlRandomForestChainBox;
    private JSpinner mlRandomForestEstimatorsSpinner;
    private JSpinner mlRandomForestRandomStateSpinner;
    private JSpinner mlCvSplitsSpinner;
    
    // Global cross-validation control
    private JCheckBox mlGlobalCvBox;
    
    // Global analysis controls
    private JCheckBox mlErrorAnalysisBox;
    private JCheckBox mlStatSigBox;

    private JCheckBox mlLogisticEnabledBox;
    private JCheckBox mlLogisticChainBox;
    private JSpinner mlLogisticMaxIterSpinner;

    private JCheckBox mlMultinomialEnabledBox;
    private JCheckBox mlMultinomialChainBox;
    private JTextField mlExperimentNameField;
    private JCheckBox mlRunUnbalancedBox;
    private JCheckBox mlRunBalancedBox;
    private JSpinner mlBalancedTargetSpinner;
    
    // Model Persistence controls
    private JCheckBox mlPersistenceEnabledBox;
    private JCheckBox mlSaveBestModelBox;
    private JTextField mlCustomModelNameField;
    private JComboBox<String> mlSelectionMetricCombo;
    
    private JSpinner mlTopKSpinner;
    private JSpinner mlTopKPlotSpinner;
    private JSpinner mlMaxWordsPerLabelSpinner;
    private JCheckBox mlUseWordcloudVocabBox;
    private JSpinner mlTfidfMinDfSpinner;
    private JCheckBox mlTfidfUseIdfBox;
    private JSpinner mlTfidfNgramMinSpinner;
    private JSpinner mlTfidfNgramMaxSpinner;

    private JCheckBox mlDeepLearningEnabledBox;

    private JCheckBox mlMlpEnabledBox;
    private JSpinner mlMlpCvEpochsSpinner;
    private JSpinner mlMlpCvBatchSpinner;
    private JSpinner mlMlpEpochsSpinner;
    private JSpinner mlMlpBatchSpinner;
    private JSpinner mlMlpValidationSpinner;
    private JSpinner mlMlpEarlyStoppingSpinner;
    private JSpinner mlMlpLayer1UnitsSpinner;
    private JSpinner mlMlpLayer1DropoutSpinner;
    private JSpinner mlMlpLayer2UnitsSpinner;
    private JSpinner mlMlpLayer2DropoutSpinner;

    private JCheckBox mlCnnEnabledBox;
    private JSpinner mlCnnCvEpochsSpinner;
    private JSpinner mlCnnCvBatchSpinner;
    private JSpinner mlCnnEpochsSpinner;
    private JSpinner mlCnnBatchSpinner;
    private JSpinner mlCnnValidationSpinner;
    private JSpinner mlCnnEarlyStoppingSpinner;
    private JSpinner mlCnnMaxWordsSpinner;
    private JSpinner mlCnnMaxLenSpinner;
    private JSpinner mlCnnEmbeddingSpinner;
    private JSpinner mlCnnConvFiltersSpinner;
    private JSpinner mlCnnKernelSpinner;
    private JSpinner mlCnnDenseUnitsSpinner;
    private JSpinner mlCnnDropoutSpinner;

    // Multi-class controls
    private JCheckBox mcEnabledBox;
    private JCheckBox mcRunUnbalancedBox;
    private JCheckBox mcRunBalancedBox;
    private JSpinner mcSmoteNeighborsSpinner;
    private JSpinner mcTfidfMaxFeaturesSpinner;
    private JComboBox<String> mcVectorizerCombo;
    private JCheckBox mcLogisticEnabledBox;
    private JSpinner mcMaxIterSpinner;
    private JCheckBox mcRandomForestEnabledBox;
    private JSpinner mcEstimatorsSpinner;
    private JCheckBox mcLinearSvmEnabledBox;
    private JCheckBox mcMultinomialEnabledBox;
    private JCheckBox mcBertEnabledBox;
    private JCheckBox mcBertBalanceBox;
    private JSpinner mcBertEpochsSpinner;
    private JSpinner mcBertBatchSpinner;
    private JSpinner mcBertMaxLenSpinner;
    private JSpinner mcBertLrSpinner;
    private JComboBox<String> mcBertOptimizerCombo;

    private boolean updatingUiValues;
    public AITechniquePanel() {
        this(DEFAULT_MULTILABEL_SCRIPT_PATH, DEFAULT_MULTICLASS_SCRIPT_PATH, null, null);
    }

    public AITechniquePanel(String multiLabelScriptPath, String multiClassScriptPath, VisualizationPanel visualizationPanel, FeatureSamplePanel featureSamplePanel) {
        this.multiLabelScriptPath = multiLabelScriptPath;
        this.multiClassScriptPath = multiClassScriptPath;
        this.visualizationPanel = visualizationPanel;
        this.featureSamplePanel = featureSamplePanel;
        loadConfigs();
        // Initialize feature engineering fields (not displayed as tab, but needed for config read/write)
        createFeatureEngineeringPanel();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createActionBar(), BorderLayout.SOUTH);
        applyMultiLabelValues();
        applyMultiClassValues();
        updateProblemTypeSelection();
    }

    private void loadConfigs() {
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        
        multiLabelModels = ensureObject(multiLabelConfig, "models");
        multiClassModels = ensureObject(multiClassConfig, "models");
        ensureObject(multiClassConfig, "feature_engineering");
        ensureObject(multiLabelConfig, "data");
        ensureObject(multiLabelModels, "traditional_ml");
        ensureObject(multiLabelModels, "deep_learning");
        ensureObject(multiClassModels, "traditional");
        ensureObject(multiClassModels, "bert");
        ensureObject(multiLabelConfig, "feature_engineering");
        
        if (!multiLabelConfig.has("problem_type")) {
            multiLabelConfig.put("problem_type", PROBLEM_MULTI_LABEL);
        }
        if (!multiClassConfig.has("problem_type")) {
            multiClassConfig.put("problem_type", PROBLEM_MULTI_CLASS);
        }
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.setBackground(new Color(18, 28, 38));
        JLabel title = new JLabel("Artificial Intelligence Techniques");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Choose between multi-label or multi-class flows and fine tune each model family.");
        subtitle.setForeground(new Color(200, 210, 220));
        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        mlRadio = new JRadioButton("Multi-label");
        mcRadio = new JRadioButton("Multi-class");
        mlRadio.setOpaque(false);
        mcRadio.setOpaque(false);
        mlRadio.setForeground(Color.WHITE);
        mcRadio.setForeground(Color.WHITE);
        ButtonGroup group = new ButtonGroup();
        group.add(mlRadio);
        group.add(mcRadio);
        switcher.setOpaque(false);
        switcher.add(new JLabel("Problem type:"));
        switcher.add(mlRadio);
        switcher.add(mcRadio);

        mlRadio.addActionListener(e -> {
            if (mlRadio.isSelected()) {
                problemTabs.setSelectedIndex(0);
                updateProblemTabEnabling();
            }
        });
        mcRadio.addActionListener(e -> {
            if (mcRadio.isSelected()) {
                problemTabs.setSelectedIndex(1);
                updateProblemTabEnabling();
            }
        });

        header.add(title, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(subtitle, BorderLayout.WEST);
        bottom.add(switcher, BorderLayout.EAST);
        header.add(bottom, BorderLayout.SOUTH);
        return header;
    }

    private JComponent createBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(new EmptyBorder(10, 0, 0, 0));
        problemTabs = new JTabbedPane();
        problemTabs.addTab("Multi-label", createMultiLabelTabs());
        problemTabs.addTab("Multi-class", createMultiClassTabs());
        problemTabs.addChangeListener(e -> {
            if (problemTabs.getSelectedIndex() == 1) {
                ConfigManager.getMultiClassConfig().put("problem_type", PROBLEM_MULTI_CLASS);
                mcRadio.setSelected(true);
            } else {
                ConfigManager.getMultiLabelConfig().put("problem_type", PROBLEM_MULTI_LABEL);
                mlRadio.setSelected(true);
            }
            updateProblemTabEnabling();
        });
        body.add(problemTabs, BorderLayout.CENTER);
        return body;
    }

    private JComponent createMultiLabelTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", wrapTab(createMultiLabelGeneralPanel()));
        tabs.addTab("Data", wrapTab(createMultiLabelDataPanel()));
        tabs.addTab("Random Forest", wrapTab(createRandomForestPanel()));
        tabs.addTab("Logistic Regression", wrapTab(createLogisticPanel()));
        tabs.addTab("Multinomial NB", wrapTab(createMultinomialPanel()));
        tabs.addTab("Deep Learning", wrapTab(createDeepLearningGeneralPanel()));
        tabs.addTab("MLP", wrapTab(createMlpPanel()));
        tabs.addTab("CNN", wrapTab(createCnnPanel()));
        tabs.addTab("Prediction & Models", new PredictionPanel());
        return tabs;
    }

    private JComponent createMultiLabelGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlExperimentNameField = new JTextField(20);
        addRow(panel, row++, "Experiment Name", mlExperimentNameField);
        
        mlGlobalCvBox = new JCheckBox("Enable Cross-Validation (Global)");
        addFullRow(panel, row++, mlGlobalCvBox);
        
        mlCvSplitsSpinner = createIntSpinner(10, 2, 20, 1);
        addRow(panel, row++, "CV Splits", mlCvSplitsSpinner);
        
        mlErrorAnalysisBox = new JCheckBox("Enable Error Analysis (Misclassification)");
        addFullRow(panel, row++, mlErrorAnalysisBox);
        
        mlStatSigBox = new JCheckBox("Enable Statistical Significance Testing");
        addFullRow(panel, row++, mlStatSigBox);
        
        // Add separator
        addFullRow(panel, row++, new JSeparator());
        
        // Model Persistence Section
        JLabel persistenceLabel = new JLabel("Model Persistence:");
        persistenceLabel.setFont(persistenceLabel.getFont().deriveFont(Font.BOLD));
        addFullRow(panel, row++, persistenceLabel);
        
        mlPersistenceEnabledBox = new JCheckBox("Enable Model Persistence");
        addFullRow(panel, row++, mlPersistenceEnabledBox);
        
        mlSaveBestModelBox = new JCheckBox("Save Best Model After Training");
        addFullRow(panel, row++, mlSaveBestModelBox);
        
        mlCustomModelNameField = new JTextField(20);
        addRow(panel, row++, "Custom Model Name (optional)", mlCustomModelNameField);
        
        mlSelectionMetricCombo = new JComboBox<>(new String[]{"macro_f1", "micro_f1", "macro_recall", "micro_recall", "hamming_loss"});
        addRow(panel, row++, "Best Model Selection Metric", mlSelectionMetricCombo);
        
        return panel;
    }

    private JComponent createMultiLabelDataPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlRunUnbalancedBox = new JCheckBox("Run on Unbalanced data");
        mlRunBalancedBox = new JCheckBox("Run on Balanced data");
        mlBalancedTargetSpinner = createIntSpinner(600, 100, 10000, 50);
        addFullRow(panel, row++, mlRunUnbalancedBox);
        addFullRow(panel, row++, mlRunBalancedBox);
        addRow(panel, row++, "Balanced target count", mlBalancedTargetSpinner);
        return panel;
    }

    private JComponent createFeatureEngineeringPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlTopKSpinner = createIntSpinner(50, 10, 1000, 10);
        addRow(panel, row++, "Top K features", mlTopKSpinner);
        mlTopKPlotSpinner = createIntSpinner(20, 5, 100, 5);
        addRow(panel, row++, "Top K features to plot", mlTopKPlotSpinner);
        mlMaxWordsPerLabelSpinner = createIntSpinner(50, 10, 200, 10);
        addRow(panel, row++, "Max words per label", mlMaxWordsPerLabelSpinner);
        mlUseWordcloudVocabBox = new JCheckBox("Use wordcloud vocabulary");
        addFullRow(panel, row++, mlUseWordcloudVocabBox);
        mlTfidfMinDfSpinner = createIntSpinner(1, 1, 10, 1);
        addRow(panel, row++, "TF-IDF min_df", mlTfidfMinDfSpinner);
        mlTfidfUseIdfBox = new JCheckBox("TF-IDF use IDF");
        addFullRow(panel, row++, mlTfidfUseIdfBox);
        mlTfidfNgramMinSpinner = createIntSpinner(1, 1, 3, 1);
        addRow(panel, row++, "TF-IDF ngram min", mlTfidfNgramMinSpinner);
        mlTfidfNgramMaxSpinner = createIntSpinner(2, 1, 5, 1);
        addRow(panel, row++, "TF-IDF ngram max", mlTfidfNgramMaxSpinner);
        return panel;
    }

    private JComponent createRandomForestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlRandomForestEnabledBox = new JCheckBox("Enable Random Forest");
        addFullRow(panel, row++, mlRandomForestEnabledBox);
        mlRandomForestChainBox = new JCheckBox("Use classifier chain");
        addFullRow(panel, row++, mlRandomForestChainBox);
        mlRandomForestEstimatorsSpinner = createIntSpinner(100, 10, 1000, 10);
        addRow(panel, row++, "Trees", mlRandomForestEstimatorsSpinner);
        mlRandomForestRandomStateSpinner = createIntSpinner(42, 0, 10000, 1);
        addRow(panel, row++, "Random state", mlRandomForestRandomStateSpinner);
        return panel;
    }

    private JComponent createLogisticPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlLogisticEnabledBox = new JCheckBox("Enable Logistic Regression");
        addFullRow(panel, row++, mlLogisticEnabledBox);
        mlLogisticChainBox = new JCheckBox("Use classifier chain");
        addFullRow(panel, row++, mlLogisticChainBox);
        mlLogisticMaxIterSpinner = createIntSpinner(10000, 100, 20000, 100);
        addRow(panel, row++, "Max iterations", mlLogisticMaxIterSpinner);
        return panel;
    }

    private JComponent createMultinomialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlMultinomialEnabledBox = new JCheckBox("Enable Multinomial NB");
        addFullRow(panel, row++, mlMultinomialEnabledBox);
        mlMultinomialChainBox = new JCheckBox("Use classifier chain");
        addFullRow(panel, row++, mlMultinomialChainBox);
        return panel;
    }

    private JComponent createDeepLearningGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlDeepLearningEnabledBox = new JCheckBox("Enable deep learning experiments");
        addFullRow(panel, row++, mlDeepLearningEnabledBox);
        JLabel hint = new JLabel("Toggle individual MLP/CNN tabs below to include them in a run.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        addFullRow(panel, row++, hint);
        return panel;
    }

    private JComponent createMlpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlMlpEnabledBox = new JCheckBox("Enable MLP");
        addFullRow(panel, row++, mlMlpEnabledBox);
        mlMlpCvEpochsSpinner = createIntSpinner(100, 1, 1000, 5);
        addRow(panel, row++, "CV epochs", mlMlpCvEpochsSpinner);
        mlMlpCvBatchSpinner = createIntSpinner(16, 1, 256, 1);
        addRow(panel, row++, "CV batch size", mlMlpCvBatchSpinner);
        mlMlpEpochsSpinner = createIntSpinner(100, 1, 1000, 5);
        addRow(panel, row++, "Epochs", mlMlpEpochsSpinner);
        mlMlpBatchSpinner = createIntSpinner(16, 1, 256, 1);
        addRow(panel, row++, "Batch size", mlMlpBatchSpinner);
        mlMlpValidationSpinner = createDoubleSpinner(0.2, 0.05, 0.5, 0.05, "0.00");
        addRow(panel, row++, "Validation split", mlMlpValidationSpinner);
        mlMlpEarlyStoppingSpinner = createIntSpinner(5, 1, 50, 1);
        addRow(panel, row++, "Early stopping patience", mlMlpEarlyStoppingSpinner);
        mlMlpLayer1UnitsSpinner = createIntSpinner(256, 8, 1024, 8);
        addRow(panel, row++, "Layer 1 units", mlMlpLayer1UnitsSpinner);
        mlMlpLayer1DropoutSpinner = createDoubleSpinner(0.5, 0.0, 0.9, 0.05, "0.00");
        addRow(panel, row++, "Layer 1 dropout", mlMlpLayer1DropoutSpinner);
        mlMlpLayer2UnitsSpinner = createIntSpinner(128, 8, 1024, 8);
        addRow(panel, row++, "Layer 2 units", mlMlpLayer2UnitsSpinner);
        mlMlpLayer2DropoutSpinner = createDoubleSpinner(0.5, 0.0, 0.9, 0.05, "0.00");
        addRow(panel, row++, "Layer 2 dropout", mlMlpLayer2DropoutSpinner);
        return panel;
    }

    private JComponent createCnnPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mlCnnEnabledBox = new JCheckBox("Enable CNN");
        addFullRow(panel, row++, mlCnnEnabledBox);
        mlCnnCvEpochsSpinner = createIntSpinner(10, 1, 500, 1);
        addRow(panel, row++, "CV epochs", mlCnnCvEpochsSpinner);
        mlCnnCvBatchSpinner = createIntSpinner(32, 1, 512, 1);
        addRow(panel, row++, "CV batch size", mlCnnCvBatchSpinner);
        mlCnnEpochsSpinner = createIntSpinner(20, 1, 500, 1);
        addRow(panel, row++, "Epochs", mlCnnEpochsSpinner);
        mlCnnBatchSpinner = createIntSpinner(32, 1, 512, 1);
        addRow(panel, row++, "Batch size", mlCnnBatchSpinner);
        mlCnnValidationSpinner = createDoubleSpinner(0.2, 0.05, 0.5, 0.05, "0.00");
        addRow(panel, row++, "Validation split", mlCnnValidationSpinner);
        mlCnnEarlyStoppingSpinner = createIntSpinner(5, 1, 50, 1);
        addRow(panel, row++, "Early stopping patience", mlCnnEarlyStoppingSpinner);
        mlCnnMaxWordsSpinner = createIntSpinner(5000, 1000, 20000, 250);
        addRow(panel, row++, "Max words", mlCnnMaxWordsSpinner);
        mlCnnMaxLenSpinner = createIntSpinner(100, 10, 1000, 10);
        addRow(panel, row++, "Sequence length", mlCnnMaxLenSpinner);
        mlCnnEmbeddingSpinner = createIntSpinner(100, 16, 512, 8);
        addRow(panel, row++, "Embedding size", mlCnnEmbeddingSpinner);
        mlCnnConvFiltersSpinner = createIntSpinner(128, 32, 1024, 16);
        addRow(panel, row++, "Conv filters", mlCnnConvFiltersSpinner);
        mlCnnKernelSpinner = createIntSpinner(5, 1, 15, 1);
        addRow(panel, row++, "Kernel size", mlCnnKernelSpinner);
        mlCnnDenseUnitsSpinner = createIntSpinner(128, 8, 1024, 8);
        addRow(panel, row++, "Dense units", mlCnnDenseUnitsSpinner);
        mlCnnDropoutSpinner = createDoubleSpinner(0.5, 0.0, 0.9, 0.05, "0.00");
        addRow(panel, row++, "Dropout", mlCnnDropoutSpinner);
        return panel;
    }

    private JComponent createMultiClassTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", wrapTab(createMultiClassOverviewPanel()));
        tabs.addTab("Logistic Regression", wrapTab(createMcLogisticPanel()));
        tabs.addTab("Random Forest", wrapTab(createMcRandomForestPanel()));
        tabs.addTab("Linear SVM", wrapTab(createMcSvmPanel()));
        tabs.addTab("Multinomial NB", wrapTab(createMcNbPanel()));
        tabs.addTab("BERT", wrapTab(createMcBertPanel()));
        return tabs;
    }

    private JComponent createMultiClassOverviewPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcEnabledBox = new JCheckBox("Enable multi-class ML workflow");
        addFullRow(panel, row++, mcEnabledBox);
        mcRunUnbalancedBox = new JCheckBox("Run on unbalanced data split");
        addFullRow(panel, row++, mcRunUnbalancedBox);
        mcRunBalancedBox = new JCheckBox("Run on SMOTE-balanced data split");
        addFullRow(panel, row++, mcRunBalancedBox);
        mcSmoteNeighborsSpinner = createIntSpinner(5, 1, 10, 1);
        addRow(panel, row++, "SMOTE k-neighbors", mcSmoteNeighborsSpinner);
        mcTfidfMaxFeaturesSpinner = createIntSpinner(5000, 500, 20000, 500);
        addRow(panel, row++, "TF-IDF max features", mcTfidfMaxFeaturesSpinner);
        mcVectorizerCombo = new JComboBox<>(new String[]{"TF-IDF", "BERT tokenizer (BERT only)"});
        addRow(panel, row++, "Text representation", mcVectorizerCombo);
        JLabel guidance = new JLabel("Enable the models you want to compare; multiple choices are allowed.");
        guidance.setFont(guidance.getFont().deriveFont(Font.ITALIC, 11f));
        addFullRow(panel, row++, guidance);
        mcEnabledBox.addActionListener(e -> {
            if (updatingUiValues || mcEnabledBox.isSelected()) {
                return;
            }
            if (mcLogisticEnabledBox != null) {
                mcLogisticEnabledBox.setSelected(false);
            }
            if (mcRandomForestEnabledBox != null) {
                mcRandomForestEnabledBox.setSelected(false);
            }
            if (mcLinearSvmEnabledBox != null) {
                mcLinearSvmEnabledBox.setSelected(false);
            }
            if (mcMultinomialEnabledBox != null) {
                mcMultinomialEnabledBox.setSelected(false);
            }
            if (mcBertEnabledBox != null) {
                mcBertEnabledBox.setSelected(false);
            }
        });
        return panel;
    }

    private JComponent createMcLogisticPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcLogisticEnabledBox = new JCheckBox("Run Logistic Regression");
        addFullRow(panel, row++, mcLogisticEnabledBox);
        mcMaxIterSpinner = createIntSpinner(1000, 50, 20000, 50);
        addRow(panel, row++, "Max iterations", mcMaxIterSpinner);
        mcLogisticEnabledBox.addActionListener(e -> syncMultiClassEnabled());
        return panel;
    }

    private JComponent createMcRandomForestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcRandomForestEnabledBox = new JCheckBox("Run Random Forest");
        addFullRow(panel, row++, mcRandomForestEnabledBox);
        mcEstimatorsSpinner = createIntSpinner(200, 10, 1000, 10);
        addRow(panel, row++, "n_estimators", mcEstimatorsSpinner);
        mcRandomForestEnabledBox.addActionListener(e -> syncMultiClassEnabled());
        return panel;
    }

    private JComponent createMcSvmPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcLinearSvmEnabledBox = new JCheckBox("Run Linear SVM");
        addFullRow(panel, row++, mcLinearSvmEnabledBox);
        mcLinearSvmEnabledBox.addActionListener(e -> syncMultiClassEnabled());
        return panel;
    }

    private JComponent createMcNbPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcMultinomialEnabledBox = new JCheckBox("Run Multinomial NB");
        addFullRow(panel, row++, mcMultinomialEnabledBox);
        mcMultinomialEnabledBox.addActionListener(e -> syncMultiClassEnabled());
        return panel;
    }

    private JComponent createMcBertPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        mcBertEnabledBox = new JCheckBox("Enable BERT fine-tuning");
        addFullRow(panel, row++, mcBertEnabledBox);
        mcBertBalanceBox = new JCheckBox("Oversample classes for BERT training");
        addFullRow(panel, row++, mcBertBalanceBox);
        mcBertEpochsSpinner = createIntSpinner(4, 1, 20, 1);
        addRow(panel, row++, "Epochs", mcBertEpochsSpinner);
        mcBertBatchSpinner = createIntSpinner(16, 1, 128, 1);
        addRow(panel, row++, "Batch size", mcBertBatchSpinner);
        mcBertMaxLenSpinner = createIntSpinner(128, 32, 512, 8);
        addRow(panel, row++, "Max sequence length", mcBertMaxLenSpinner);
        mcBertLrSpinner = createDoubleSpinner(0.00002, 0.000001, 0.001, 0.000001, "0.######");
        addRow(panel, row++, "Learning rate", mcBertLrSpinner);
        mcBertOptimizerCombo = new JComboBox<>(new String[]{"AdamW", "Adam", "PSO (fallback to AdamW)"});
        addRow(panel, row++, "Optimizer", mcBertOptimizerCombo);
        mcBertEnabledBox.addActionListener(e -> syncMultiClassEnabled());
        return panel;
    }

    private JComponent wrapTab(JComponent component) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(12, 12, 12, 12));
        wrapper.add(component, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createActionBar() {
        JPanel container = new JPanel(new BorderLayout());

        JPanel statusPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Overall progress
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        runStatusLabel = new JLabel("Status:");
        statusPanel.add(runStatusLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        runProgressBar = new JProgressBar();
        runProgressBar.setPreferredSize(new Dimension(180, 16));
        runProgressBar.setStringPainted(true);
        runProgressBar.setString("Idle");
        runProgressBar.setValue(0);
        statusPanel.add(runProgressBar, gbc);
        
        // Task-specific progress
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        taskStatusLabel = new JLabel("");
        taskStatusLabel.setFont(taskStatusLabel.getFont().deriveFont(Font.PLAIN, 10f));
        taskStatusLabel.setForeground(Color.DARK_GRAY);
        statusPanel.add(taskStatusLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        taskProgressBar = new JProgressBar();
        taskProgressBar.setPreferredSize(new Dimension(180, 12));
        taskProgressBar.setStringPainted(true);
        taskProgressBar.setString("");
        taskProgressBar.setValue(0);
        taskProgressBar.setVisible(false);
        statusPanel.add(taskProgressBar, gbc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetButton = new JButton("Reset");
        JButton previewButton = new JButton("Preview JSON");
        JButton saveConfigButton = new JButton("Save Config");
        JButton runButton = new JButton("Run");
        runButton.setBackground(new Color(144, 238, 144));
        runButton.setOpaque(true);
        runButton.setFocusPainted(false);

        resetButton.addActionListener(this::handleReset);
        previewButton.addActionListener(this::handlePreview);
        saveConfigButton.addActionListener(this::handleSave);
        runButton.addActionListener(this::handleRun);

        actions.add(resetButton);
        actions.add(previewButton);
        actions.add(saveConfigButton);
        actions.add(runButton);
        container.add(statusPanel, BorderLayout.WEST);
        container.add(actions, BorderLayout.EAST);
        return container;
    }

    private void handleReset(ActionEvent event) {
        loadConfigs();
        applyMultiLabelValues();
        applyMultiClassValues();
        updateProblemTypeSelection();
        setProgressState("Idle", false, 0, Color.DARK_GRAY);
    }

    private void handlePreview(ActionEvent event) {
        // Persist AI panel values to shared config
        persistInputsToModel();
        
        // Persist feature/sample panel values to shared config
        if (featureSamplePanel != null) {
            featureSamplePanel.persistUiToModel();
        }
        
        // Get active config from ConfigManager (already has all changes in memory)
        JSONObject activeConfig = getActiveConfig();
        
        JTextArea textArea = new JTextArea(activeConfig.toString(2));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Configuration Preview", JOptionPane.INFORMATION_MESSAGE);
        setProgressState("Previewed JSON", false, 10, Color.DARK_GRAY);
    }

    private void handleSave(ActionEvent event) {
        saveConfigToFile(true);
        setProgressState("Saved configuration", false, 20, Color.BLUE);
    }

    private void handleRun(ActionEvent event) {
        // Validate config before running
        String validationError = validateConfig();
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError, "Configuration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!saveConfigToFile(false)) {
            return;
        }
        setProgressState("Saved configuration", false, 30, Color.BLUE);

        JButton sourceButton = event.getSource() instanceof JButton ? (JButton) event.getSource() : null;
        final String defaultText = sourceButton != null ? sourceButton.getText() : null;
        if (sourceButton != null) {
            sourceButton.setEnabled(false);
            sourceButton.setText("Running...");
        }
        setProgressState("Running...", true, 0, Color.BLUE);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private String outputText = "";
            private String errorMessage;
            private int exitCode = -1;

            @Override
            protected Void doInBackground() {
                try {
                    outputText = executePythonScript();
                } catch (IOException | InterruptedException ex) {
                    errorMessage = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String line : chunks) {
                    parseAndUpdateProgress(line);
                }
            }

            private String executePythonScript() throws IOException, InterruptedException {
                Path scriptPath = Paths.get(getActiveScriptPath()).toAbsolutePath();
                Path scriptDir = scriptPath.getParent();
                Path runDirectory = scriptDir;

                Path configPathObj = null;
                if (isMultiClassSelected()) {
                    configPathObj = Paths.get(ConfigManager.MULTICLASS_CONFIG_PATH).toAbsolutePath();
                }

                if (!isMultiClassSelected() && scriptDir != null && scriptDir.getParent() != null) {
                    runDirectory = scriptDir.getParent(); // Run from project root so main.py can find configs/*
                }

                if (!Files.exists(scriptPath)) {
                    throw new IOException("Unable to locate Python script at " + scriptPath);
                }
                if (configPathObj != null && !Files.exists(configPathObj)) {
                    throw new IOException("Configuration file not found at " + configPathObj);
                }

                List<String> command = new ArrayList<>();
                command.add("python");
                command.add(scriptPath.toString());
                if (configPathObj != null) {
                    command.add(configPathObj.toString());
                }

                ProcessBuilder builder = new ProcessBuilder(command);
                if (runDirectory != null) {
                    builder.directory(runDirectory.toFile());
                }
                builder.redirectErrorStream(true);

                Process process = builder.start();
                StringBuilder outputBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append(System.lineSeparator());
                        publish(line); // Send line to UI for real-time processing
                    }
                }
                exitCode = process.waitFor();

                return outputBuilder.toString();
            }

            @Override
            protected void done() {
                if (sourceButton != null) {
                    sourceButton.setEnabled(true);
                    sourceButton.setText(defaultText);
                }
                if (exitCode == 0 && errorMessage == null) {
                    setProgressState("Completed", false, 100, new Color(0, 128, 0));
                } else if (errorMessage != null) {
                    setProgressState("Failed: " + errorMessage, false, 100, Color.RED);
                } else {
                    setProgressState("Finished with exit code " + exitCode, false, 100, Color.ORANGE);
                }

                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(
                            AITechniquePanel.this,
                            "Unable to run script (" + getActiveScriptPath() + "): " + errorMessage,
                            "Run Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                // Refresh PredictionPanel after run completes
                PredictionPanel.refreshPanel();

                int messageType = exitCode == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
                String title = exitCode == 0 ? "Run Completed" : "Run finished with exit code " + exitCode;
                showProcessOutput(title, outputText, messageType);
            }
        };

        worker.execute();
    }

    private boolean saveConfigToFile(boolean showSuccessMessage) {
        persistInputsToModel();
        
        // Save using ConfigManager
        boolean success = false;
        try {
            if (isMultiClassSelected()) {
                ConfigManager.saveMultiClass();
            } else {
                ConfigManager.saveMultiLabel();
            }
            success = true;
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Unable to save configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
        
        // Save feature/sample settings
        if (featureSamplePanel != null) {
            featureSamplePanel.saveSilently();
        }
        
        // Also save visualization settings
        if (visualizationPanel != null) {
            visualizationPanel.saveSilently();
        }
        
        if (showSuccessMessage && !GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this, "Configuration saved successfully (all tabs).");
        }
        return true;
    }

    private void showProcessOutput(String title, String output, int messageType) {
        String content = (output == null || output.isBlank()) ? "[No output produced]" : output;
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        JOptionPane.showMessageDialog(this, scrollPane, title, messageType);
    }

    private void parseAndUpdateProgress(String line) {
        // Parse structured log markers: [PROGRESS:overall:percent:message] or [TASK:model:percent:message]
        if (line.contains("[PROGRESS:")) {
            int start = line.indexOf("[PROGRESS:") + 10;
            int end = line.indexOf("]", start);
            if (end > start) {
                String[] parts = line.substring(start, end).split(":", 4);
                if (parts.length >= 3) {
                    try {
                        int percent = Integer.parseInt(parts[1]);
                        String message = parts.length > 2 ? parts[2] : "";
                        SwingUtilities.invokeLater(() -> {
                            runProgressBar.setIndeterminate(false);
                            runProgressBar.setValue(percent);
                            runProgressBar.setString(message);
                            runStatusLabel.setText("Status:");
                            runStatusLabel.setForeground(Color.BLUE);
                        });
                    } catch (NumberFormatException ignored) {}
                }
            }
        } else if (line.contains("[TASK:")) {
            int start = line.indexOf("[TASK:") + 6;
            int end = line.indexOf("]", start);
            if (end > start) {
                String[] parts = line.substring(start, end).split(":", 4);
                if (parts.length >= 3) {
                    try {
                        int percent = Integer.parseInt(parts[1]);
                        String message = parts.length > 2 ? parts[2] : "";
                        SwingUtilities.invokeLater(() -> {
                            taskProgressBar.setVisible(true);
                            taskProgressBar.setIndeterminate(false);
                            taskProgressBar.setValue(percent);
                            taskProgressBar.setString( message);
                            taskStatusLabel.setText("Model: " + parts[0]);
                        });
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private void setProgressState(String text, boolean indeterminate, int value, Color color) {
        if (runProgressBar != null) {
            runProgressBar.setIndeterminate(indeterminate);
            if (!indeterminate) {
                runProgressBar.setValue(value);
            }
            runProgressBar.setForeground(color);
            runProgressBar.setString(text);
        }
        if (runStatusLabel != null) {
            runStatusLabel.setText("Status:");
            runStatusLabel.setForeground(color);
        }
        if (taskProgressBar != null) {
            taskProgressBar.setVisible(false);
            taskProgressBar.setValue(0);
        }
        if (taskStatusLabel != null) {
            taskStatusLabel.setText("");
        }
    }

    private void updateProblemTypeSelection() {
        if (problemTabs == null) {
            return;
        }
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        String labelType = multiLabelConfig != null ? multiLabelConfig.optString("problem_type", PROBLEM_MULTI_LABEL) : PROBLEM_MULTI_LABEL;
        String classType = multiClassConfig != null ? multiClassConfig.optString("problem_type", PROBLEM_MULTI_LABEL) : PROBLEM_MULTI_LABEL;
        String selectedType = labelType;
        if (PROBLEM_MULTI_LABEL.equals(labelType) && PROBLEM_MULTI_CLASS.equals(classType)) {
            selectedType = PROBLEM_MULTI_CLASS;
        }
        if (PROBLEM_MULTI_CLASS.equals(selectedType)) {
            problemTabs.setSelectedIndex(1);
            if (mcRadio != null) mcRadio.setSelected(true);
        } else {
            problemTabs.setSelectedIndex(0);
            if (mlRadio != null) mlRadio.setSelected(true);
        }
        updateProblemTabEnabling();
    }

    private void updateProblemTabEnabling() {
        if (problemTabs == null) {
            return;
        }
        boolean isMultiLabel = problemTabs.getSelectedIndex() == 0;
        problemTabs.setEnabledAt(0, true);
        problemTabs.setEnabledAt(1, true);
        problemTabs.setEnabledAt(isMultiLabel ? 1 : 0, false);
    }

    private void applyMultiLabelValues() {
        if (multiLabelModels == null || mlRandomForestEnabledBox == null) {
            return;
        }
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject traditional = ensureObject(multiLabelModels, "traditional_ml");
        JSONObject data = ensureObject(multiLabelConfig, "data");
        JSONObject featureEng = ensureObject(multiLabelConfig, "feature_engineering");
        JSONObject tfidf = ensureObject(featureEng, "tfidf");

        // Load global cross-validation setting
        mlGlobalCvBox.setSelected(multiLabelConfig.optBoolean("run_cross_validation", true));
        mlCvSplitsSpinner.setValue(traditional.optInt("cv_n_splits", 10));
        
        // Load global analysis settings (default OFF)
        JSONObject analysis = ensureObject(multiLabelConfig, "analysis");
        mlErrorAnalysisBox.setSelected(analysis.optBoolean("enable_error_analysis", false));
        mlStatSigBox.setSelected(analysis.optBoolean("enable_statistical_significance", false));

        mlExperimentNameField.setText(multiLabelConfig.optString("experiment_name", "Multi-Label Experiment"));
        mlRunUnbalancedBox.setSelected(data.optBoolean("run_unbalanced", true));
        mlRunBalancedBox.setSelected(data.optBoolean("run_balanced", true));
        mlBalancedTargetSpinner.setValue(data.optInt("balanced_target_count", 600));
        
        mlTopKSpinner.setValue(featureEng.optInt("top_k", 50));
        mlTopKPlotSpinner.setValue(featureEng.optInt("top_k_plot", 20));
        mlMaxWordsPerLabelSpinner.setValue(featureEng.optInt("max_words_per_label", 50));
        mlUseWordcloudVocabBox.setSelected(featureEng.optBoolean("use_wordcloud_vocabulary", true));
        mlTfidfMinDfSpinner.setValue(tfidf.optInt("min_df", 1));
        mlTfidfUseIdfBox.setSelected(tfidf.optBoolean("use_idf", true));
        org.json.JSONArray ngramRange = tfidf.optJSONArray("ngram_range");
        if (ngramRange != null && ngramRange.length() == 2) {
            mlTfidfNgramMinSpinner.setValue(ngramRange.optInt(0, 1));
            mlTfidfNgramMaxSpinner.setValue(ngramRange.optInt(1, 2));
        }

        JSONObject randomForest = ensureObject(traditional, "random_forest");
        mlRandomForestEnabledBox.setSelected(randomForest.optBoolean("enabled", true));
        mlRandomForestChainBox.setSelected(randomForest.optBoolean("use_classifier_chain", true));
        mlRandomForestEstimatorsSpinner.setValue(randomForest.optInt("n_estimators", 100));
        mlRandomForestRandomStateSpinner.setValue(randomForest.optInt("random_state", 42));

        JSONObject logistic = ensureObject(traditional, "logistic_regression");
        mlLogisticEnabledBox.setSelected(logistic.optBoolean("enabled", true));
        mlLogisticChainBox.setSelected(logistic.optBoolean("use_classifier_chain", true));
        mlLogisticMaxIterSpinner.setValue(logistic.optInt("max_iter", 1000));

        JSONObject multinomial = ensureObject(traditional, "multinomial_nb");
        mlMultinomialEnabledBox.setSelected(multinomial.optBoolean("enabled", true));
        mlMultinomialChainBox.setSelected(multinomial.optBoolean("use_classifier_chain", true));

        JSONObject deepLearning = ensureObject(multiLabelModels, "deep_learning");
        mlDeepLearningEnabledBox.setSelected(deepLearning.optBoolean("enabled", true));

        JSONObject mlp = ensureObject(deepLearning, "mlp");
        mlMlpEnabledBox.setSelected(mlp.optBoolean("enabled", true));
        mlMlpCvEpochsSpinner.setValue(mlp.optInt("cv_epochs", 50));
        mlMlpCvBatchSpinner.setValue(mlp.optInt("cv_batch_size", 16));
        mlMlpEpochsSpinner.setValue(mlp.optInt("epochs", 50));
        mlMlpBatchSpinner.setValue(mlp.optInt("batch_size", 16));
        mlMlpValidationSpinner.setValue(mlp.optDouble("validation_split", 0.2));
        mlMlpEarlyStoppingSpinner.setValue(mlp.optInt("early_stopping_patience", 5));
        JSONObject mlpArchitecture = ensureObject(mlp, "architecture");
        mlMlpLayer1UnitsSpinner.setValue(mlpArchitecture.optInt("layer1_units", 256));
        mlMlpLayer1DropoutSpinner.setValue(mlpArchitecture.optDouble("layer1_dropout", 0.5));
        mlMlpLayer2UnitsSpinner.setValue(mlpArchitecture.optInt("layer2_units", 128));
        mlMlpLayer2DropoutSpinner.setValue(mlpArchitecture.optDouble("layer2_dropout", 0.5));

        JSONObject cnn = ensureObject(deepLearning, "cnn");
        mlCnnEnabledBox.setSelected(cnn.optBoolean("enabled", true));
        mlCnnCvEpochsSpinner.setValue(cnn.optInt("cv_epochs", 10));
        mlCnnCvBatchSpinner.setValue(cnn.optInt("cv_batch_size", 32));
        mlCnnEpochsSpinner.setValue(cnn.optInt("epochs", 20));
        mlCnnBatchSpinner.setValue(cnn.optInt("batch_size", 32));
        mlCnnValidationSpinner.setValue(cnn.optDouble("validation_split", 0.2));
        mlCnnEarlyStoppingSpinner.setValue(cnn.optInt("early_stopping_patience", 5));
        mlCnnMaxWordsSpinner.setValue(cnn.optInt("max_words", 5000));
        mlCnnMaxLenSpinner.setValue(cnn.optInt("max_len", 100));
        mlCnnEmbeddingSpinner.setValue(cnn.optInt("embedding_dim", 100));
        mlCnnConvFiltersSpinner.setValue(cnn.optInt("conv_filters", 128));
        mlCnnKernelSpinner.setValue(cnn.optInt("conv_kernel_size", 5));
        mlCnnDenseUnitsSpinner.setValue(cnn.optInt("dense_units", 128));
        mlCnnDropoutSpinner.setValue(cnn.optDouble("dropout", 0.5));
        
        // Load model persistence settings
        JSONObject persistence = ensureObject(multiLabelConfig, "model_persistence");
        mlPersistenceEnabledBox.setSelected(persistence.optBoolean("enabled", true));
        mlSaveBestModelBox.setSelected(persistence.optBoolean("save_best_model", true));
        mlCustomModelNameField.setText(persistence.optString("custom_model_name", ""));
        mlSelectionMetricCombo.setSelectedItem(persistence.optString("selection_metric", "macro_f1"));
    }

    private void applyMultiClassValues() {
        if (multiClassModels == null || mcEnabledBox == null) {
            return;
        }
        updatingUiValues = true;
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        JSONObject data = ensureObject(multiClassConfig, "data");
        JSONObject preprocessing = ensureObject(multiClassConfig, "preprocessing");
        JSONObject featureEngineering = ensureObject(multiClassConfig, "feature_engineering");
        JSONObject traditional = ensureObject(multiClassModels, "traditional");
        JSONObject bert = ensureObject(multiClassModels, "bert");

        boolean logisticEnabled = ensureObject(traditional, "logistic_regression").optBoolean("enabled", true);
        boolean rfEnabled = ensureObject(traditional, "random_forest").optBoolean("enabled", true);
        boolean svmEnabled = ensureObject(traditional, "linear_svm").optBoolean("enabled", true);
        boolean nbEnabled = ensureObject(traditional, "multinomial_nb").optBoolean("enabled", true);
        mcEnabledBox.setSelected(logisticEnabled || rfEnabled || svmEnabled || nbEnabled || bert.optBoolean("enabled", false));

        mcRunUnbalancedBox.setSelected(data.optBoolean("run_unbalanced", true));
        mcRunBalancedBox.setSelected(data.optBoolean("run_balanced", true));
        JSONObject smote = ensureObject(data, "smote");
        mcSmoteNeighborsSpinner.setValue(smote.optInt("k_neighbors", 5));
        mcTfidfMaxFeaturesSpinner.setValue(preprocessing.optInt("max_features", 5000));
        String vectorizer = featureEngineering.optString("vectorizer", preprocessing.optString("vectorizer", "tfidf"));
        mcVectorizerCombo.setSelectedItem("bert_tokenizer".equalsIgnoreCase(vectorizer) ? "BERT tokenizer (BERT only)" : "TF-IDF");

        JSONObject logistic = ensureObject(traditional, "logistic_regression");
        mcLogisticEnabledBox.setSelected(logisticEnabled);
        mcMaxIterSpinner.setValue(logistic.optInt("max_iter", 1000));

        JSONObject randomForest = ensureObject(traditional, "random_forest");
        mcRandomForestEnabledBox.setSelected(rfEnabled);
        mcEstimatorsSpinner.setValue(randomForest.optInt("n_estimators", 200));

        JSONObject linearSvm = ensureObject(traditional, "linear_svm");
        mcLinearSvmEnabledBox.setSelected(svmEnabled);

        JSONObject multinomial = ensureObject(traditional, "multinomial_nb");
        mcMultinomialEnabledBox.setSelected(nbEnabled);

        mcBertEnabledBox.setSelected(bert.optBoolean("enabled", false));
        mcBertBalanceBox.setSelected(bert.optBoolean("balance_training", true));
        mcBertEpochsSpinner.setValue(bert.optInt("epochs", 4));
        mcBertBatchSpinner.setValue(bert.optInt("batch_size", 16));
        mcBertMaxLenSpinner.setValue(bert.optInt("max_length", 128));
        mcBertLrSpinner.setValue(bert.optDouble("learning_rate", 0.00002));
        String bertOpt = bert.optString("optimizer", "adamw").toLowerCase();
        if (bertOpt.startsWith("adam") && !bertOpt.contains("w")) {
            mcBertOptimizerCombo.setSelectedItem("Adam");
        } else if (bertOpt.startsWith("pso")) {
            mcBertOptimizerCombo.setSelectedItem("PSO (fallback to AdamW)");
        } else {
            mcBertOptimizerCombo.setSelectedItem("AdamW");
        }
        updatingUiValues = false;
    }

    private void persistInputsToModel() {
        if (isMultiClassSelected()) {
            ConfigManager.getMultiClassConfig().put("problem_type", PROBLEM_MULTI_CLASS);
            persistMultiClassValues();
        } else {
            ConfigManager.getMultiLabelConfig().put("problem_type", PROBLEM_MULTI_LABEL);
            persistMultiLabelValues();
        }
    }

    private void persistMultiLabelValues() {
        JSONObject multiLabelConfig = ConfigManager.getMultiLabelConfig();
        JSONObject traditional = ensureObject(multiLabelModels, "traditional_ml");

        boolean rfEnabled = mlRandomForestEnabledBox.isSelected();
        boolean logisticEnabled = mlLogisticEnabledBox.isSelected();
        boolean nbEnabled = mlMultinomialEnabledBox.isSelected();

        multiLabelConfig.put("experiment_name", mlExperimentNameField.getText().trim());
        
        JSONObject data = ensureObject(multiLabelConfig, "data");
        data.put("run_unbalanced", mlRunUnbalancedBox.isSelected());
        data.put("run_balanced", mlRunBalancedBox.isSelected());
        data.put("balanced_target_count", getInt(mlBalancedTargetSpinner));
        
        JSONObject featureEng = ensureObject(multiLabelConfig, "feature_engineering");
        featureEng.put("top_k", getInt(mlTopKSpinner));
        featureEng.put("top_k_plot", getInt(mlTopKPlotSpinner));
        featureEng.put("max_words_per_label", getInt(mlMaxWordsPerLabelSpinner));
        featureEng.put("use_wordcloud_vocabulary", mlUseWordcloudVocabBox.isSelected());
        JSONObject tfidf = ensureObject(featureEng, "tfidf");
        tfidf.put("min_df", getInt(mlTfidfMinDfSpinner));
        tfidf.put("use_idf", mlTfidfUseIdfBox.isSelected());
        org.json.JSONArray ngramRange = new org.json.JSONArray();
        ngramRange.put(getInt(mlTfidfNgramMinSpinner));
        ngramRange.put(getInt(mlTfidfNgramMaxSpinner));
        tfidf.put("ngram_range", ngramRange);

        JSONObject randomForest = ensureObject(traditional, "random_forest");
        randomForest.put("enabled", rfEnabled);
        randomForest.put("use_classifier_chain", mlRandomForestChainBox.isSelected());
        randomForest.put("n_estimators", getInt(mlRandomForestEstimatorsSpinner));
        randomForest.put("random_state", getInt(mlRandomForestRandomStateSpinner));

        JSONObject logistic = ensureObject(traditional, "logistic_regression");
        logistic.put("enabled", logisticEnabled);
        logistic.put("use_classifier_chain", mlLogisticChainBox.isSelected());
        logistic.put("max_iter", getInt(mlLogisticMaxIterSpinner));

        JSONObject multinomial = ensureObject(traditional, "multinomial_nb");
        multinomial.put("enabled", nbEnabled);
        multinomial.put("use_classifier_chain", mlMultinomialChainBox.isSelected());

        boolean anyTraditionalEnabled = rfEnabled || logisticEnabled || nbEnabled;
        traditional.put("enabled", anyTraditionalEnabled);
        
        // Use global cross-validation setting
        boolean globalCv = mlGlobalCvBox.isSelected();
        multiLabelConfig.put("run_cross_validation", globalCv);
        traditional.put("run_cross_validation", globalCv);
        traditional.put("cv_n_splits", getInt(mlCvSplitsSpinner));
        
        // Save global analysis settings
        JSONObject analysis = ensureObject(multiLabelConfig, "analysis");
        analysis.put("enable_error_analysis", mlErrorAnalysisBox.isSelected());
        analysis.put("enable_statistical_significance", mlStatSigBox.isSelected());

        JSONObject deepLearning = ensureObject(multiLabelModels, "deep_learning");
        deepLearning.put("enabled", mlDeepLearningEnabledBox.isSelected());

        JSONObject mlp = ensureObject(deepLearning, "mlp");
        mlp.put("enabled", mlMlpEnabledBox.isSelected());
        mlp.put("run_cross_validation", globalCv);
        mlp.put("cv_n_splits", getInt(mlCvSplitsSpinner));
        mlp.put("cv_epochs", getInt(mlMlpCvEpochsSpinner));
        mlp.put("cv_batch_size", getInt(mlMlpCvBatchSpinner));
        mlp.put("epochs", getInt(mlMlpEpochsSpinner));
        mlp.put("batch_size", getInt(mlMlpBatchSpinner));
        mlp.put("validation_split", getDouble(mlMlpValidationSpinner));
        mlp.put("early_stopping_patience", getInt(mlMlpEarlyStoppingSpinner));
        JSONObject architecture = ensureObject(mlp, "architecture");
        architecture.put("layer1_units", getInt(mlMlpLayer1UnitsSpinner));
        architecture.put("layer1_dropout", getDouble(mlMlpLayer1DropoutSpinner));
        architecture.put("layer2_units", getInt(mlMlpLayer2UnitsSpinner));
        architecture.put("layer2_dropout", getDouble(mlMlpLayer2DropoutSpinner));

        JSONObject cnn = ensureObject(deepLearning, "cnn");
        cnn.put("enabled", mlCnnEnabledBox.isSelected());
        cnn.put("run_cross_validation", globalCv);
        cnn.put("cv_n_splits", getInt(mlCvSplitsSpinner));
        cnn.put("cv_epochs", getInt(mlCnnCvEpochsSpinner));
        cnn.put("cv_batch_size", getInt(mlCnnCvBatchSpinner));
        cnn.put("epochs", getInt(mlCnnEpochsSpinner));
        cnn.put("batch_size", getInt(mlCnnBatchSpinner));
        cnn.put("validation_split", getDouble(mlCnnValidationSpinner));
        cnn.put("early_stopping_patience", getInt(mlCnnEarlyStoppingSpinner));
        cnn.put("max_words", getInt(mlCnnMaxWordsSpinner));
        cnn.put("max_len", getInt(mlCnnMaxLenSpinner));
        cnn.put("embedding_dim", getInt(mlCnnEmbeddingSpinner));
        cnn.put("conv_filters", getInt(mlCnnConvFiltersSpinner));
        cnn.put("conv_kernel_size", getInt(mlCnnKernelSpinner));
        cnn.put("dense_units", getInt(mlCnnDenseUnitsSpinner));
        cnn.put("dropout", getDouble(mlCnnDropoutSpinner));
        
        // Save model persistence settings
        JSONObject persistence = ensureObject(multiLabelConfig, "model_persistence");
        persistence.put("enabled", mlPersistenceEnabledBox.isSelected());
        persistence.put("save_best_model", mlSaveBestModelBox.isSelected());
        String customName = mlCustomModelNameField.getText().trim();
        if (customName.isEmpty()) {
            persistence.put("custom_model_name", JSONObject.NULL);
        } else {
            persistence.put("custom_model_name", customName);
        }
        persistence.put("selection_metric", mlSelectionMetricCombo.getSelectedItem());
    }

    private void persistMultiClassValues() {
        JSONObject multiClassConfig = ConfigManager.getMultiClassConfig();
        JSONObject data = ensureObject(multiClassConfig, "data");
        data.put("run_unbalanced", mcRunUnbalancedBox.isSelected());
        data.put("run_balanced", mcRunBalancedBox.isSelected());
        JSONObject smote = ensureObject(data, "smote");
        smote.put("k_neighbors", getInt(mcSmoteNeighborsSpinner));

        JSONObject preprocessing = ensureObject(multiClassConfig, "preprocessing");
        preprocessing.put("max_features", getInt(mcTfidfMaxFeaturesSpinner));
        JSONObject featureEngineering = ensureObject(multiClassConfig, "feature_engineering");
        String vectorizerChoice = mcVectorizerCombo != null && mcVectorizerCombo.getSelectedIndex() == 1 ? "bert_tokenizer" : "tfidf";
        featureEngineering.put("vectorizer", vectorizerChoice);

        JSONObject traditional = ensureObject(multiClassModels, "traditional");
        JSONObject logistic = ensureObject(traditional, "logistic_regression");
        logistic.put("enabled", mcLogisticEnabledBox.isSelected());
        logistic.put("max_iter", getInt(mcMaxIterSpinner));

        JSONObject randomForest = ensureObject(traditional, "random_forest");
        randomForest.put("enabled", mcRandomForestEnabledBox.isSelected());
        randomForest.put("n_estimators", getInt(mcEstimatorsSpinner));

        JSONObject linearSvm = ensureObject(traditional, "linear_svm");
        linearSvm.put("enabled", mcLinearSvmEnabledBox.isSelected());

        JSONObject multinomial = ensureObject(traditional, "multinomial_nb");
        multinomial.put("enabled", mcMultinomialEnabledBox.isSelected());

        boolean workflowEnabled = mcEnabledBox.isSelected() || logistic.optBoolean("enabled", false)
                || randomForest.optBoolean("enabled", false) || linearSvm.optBoolean("enabled", false)
                || multinomial.optBoolean("enabled", false);
        multiClassConfig.put("enabled", workflowEnabled);

        JSONObject bert = ensureObject(multiClassModels, "bert");
        bert.put("enabled", mcBertEnabledBox.isSelected());
        bert.put("balance_training", mcBertBalanceBox.isSelected());
        bert.put("epochs", getInt(mcBertEpochsSpinner));
        bert.put("batch_size", getInt(mcBertBatchSpinner));
        bert.put("max_length", getInt(mcBertMaxLenSpinner));
        bert.put("learning_rate", getDouble(mcBertLrSpinner));
        String bertOptSelection = (String) mcBertOptimizerCombo.getSelectedItem();
        if (bertOptSelection != null && bertOptSelection.toLowerCase().contains("adamw")) {
            bert.put("optimizer", "adamw");
        } else if (bertOptSelection != null && bertOptSelection.toLowerCase().contains("pso")) {
            bert.put("optimizer", "pso");
        } else {
            bert.put("optimizer", "adam");
        }
    }

    private boolean isMultiClassSelected() {
        return problemTabs != null && problemTabs.getSelectedIndex() == 1;
    }

    private JSONObject getActiveConfig() {
        return isMultiClassSelected() ? ConfigManager.getMultiClassConfig() : ConfigManager.getMultiLabelConfig();
    }

    private String getActiveScriptPath() {
        return isMultiClassSelected() ? multiClassScriptPath : multiLabelScriptPath;
    }

    // Test hooks
    void persistInputsToModelForTest() {
        persistInputsToModel();
    }

    JSONObject getMultiLabelConfigForTest() {
        return ConfigManager.getMultiLabelConfig();
    }

    JSONObject getMultiClassConfigForTest() {
        return ConfigManager.getMultiClassConfig();
    }

    private void syncMultiClassEnabled() {
        if (mcEnabledBox == null || updatingUiValues) {
            return;
        }
        boolean anyModelSelected =
                (mcLogisticEnabledBox != null && mcLogisticEnabledBox.isSelected()) ||
                (mcRandomForestEnabledBox != null && mcRandomForestEnabledBox.isSelected()) ||
                (mcLinearSvmEnabledBox != null && mcLinearSvmEnabledBox.isSelected()) ||
                (mcMultinomialEnabledBox != null && mcMultinomialEnabledBox.isSelected()) ||
                (mcBertEnabledBox != null && mcBertEnabledBox.isSelected());
        if (anyModelSelected) {
            mcEnabledBox.setSelected(true);
        } else if (mcEnabledBox.isSelected()) {
            mcEnabledBox.setSelected(false);
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

    private int getInt(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private double getDouble(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
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

    private String validateConfig() {
        persistInputsToModel();
        JSONObject activeConfig = getActiveConfig();
        
        // Check experiment name
        String expName = activeConfig.optString("experiment_name", "").trim();
        if (expName.isEmpty()) {
            return "Experiment name cannot be empty.";
        }
        
        // Check at least one data type is selected
        JSONObject data = activeConfig.optJSONObject("data");
        if (data != null) {
            boolean runUnbalanced = data.optBoolean("run_unbalanced", false);
            boolean runBalanced = data.optBoolean("run_balanced", false);
            if (!runUnbalanced && !runBalanced) {
                return "At least one data type (Balanced or Unbalanced) must be selected.";
            }
        }
        
        // Check at least one model is enabled
        JSONObject models = activeConfig.optJSONObject("models");
        if (models != null && isMultiClassSelected()) {
            // Multi-class validation
            JSONObject traditional = models.optJSONObject("traditional");
            JSONObject bert = models.optJSONObject("bert");
            boolean anyEnabled = false;
            if (traditional != null) {
                anyEnabled = traditional.optJSONObject("logistic_regression").optBoolean("enabled", false) ||
                            traditional.optJSONObject("random_forest").optBoolean("enabled", false) ||
                            traditional.optJSONObject("linear_svm").optBoolean("enabled", false) ||
                            traditional.optJSONObject("multinomial_nb").optBoolean("enabled", false);
            }
            if (bert != null && bert.optBoolean("enabled", false)) {
                anyEnabled = true;
            }
            if (!anyEnabled) {
                return "At least one model must be enabled.";
            }
        } else if (models != null) {
            // Multi-label validation
            JSONObject traditional = models.optJSONObject("traditional_ml");
            JSONObject deepLearning = models.optJSONObject("deep_learning");
            boolean anyEnabled = false;
            if (traditional != null) {
                anyEnabled = traditional.optJSONObject("random_forest").optBoolean("enabled", false) ||
                            traditional.optJSONObject("logistic_regression").optBoolean("enabled", false) ||
                            traditional.optJSONObject("multinomial_nb").optBoolean("enabled", false);
            }
            if (deepLearning != null) {
                anyEnabled = anyEnabled || 
                            deepLearning.optJSONObject("mlp").optBoolean("enabled", false) ||
                            deepLearning.optJSONObject("cnn").optBoolean("enabled", false);
            }
            if (!anyEnabled) {
                return "At least one model must be enabled.";
            }
        }
        
        return null; // No errors
    }

    private void addRow(JPanel panel, int row, String label, JComponent component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_START;
        left.insets = new Insets(4, 0, 4, 10);
        panel.add(new JLabel(label), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(4, 0, 4, 0);
        panel.add(component, right);
    }

    private void addFullRow(JPanel panel, int row, JComponent component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(component, gbc);
    }
}
