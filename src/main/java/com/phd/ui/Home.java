package com.phd.ui;

import com.phd.config.Configuration;
import com.phd.config.ConfigManager;
import com.phd.db.DBManager;
import com.phd.issue.FetchData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class Home extends Component {


    public static JFrame getF() {
        return f;
    }

    static JFrame f;
    static Configuration config;
    static JCheckBox sslUsage;
    static JCheckBox certUsage;
    static JTextArea dbNameField;
    static JTextArea accessTokenField;
    static JButton saveConfiguration;
    static JButton loadConfiguration;

    public static JButton getCreateDataSet() {
        return createDataSet;
    }

    static JButton createDataSet;
    static JTextArea repoField;
    static JTextArea recordFromField;
    static JTextArea recordToField;



    Home() {
        config = Configuration.getConfig();
        f = new JFrame();
        JTabbedPane tp = new JTabbedPane();
        tp.setBorder(new EmptyBorder(4, 4, 4, 4));
        tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        f.setTitle("Software Defect Prediction...");
        f.setSize(900, 480);
        f.setMinimumSize(new Dimension(900, 420));
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getCongigurationPanel(tp, config);
        getPreProcessingPanel(tp,config);
        
        // Label Filtering Panel for data quality
        LabelFilterPanel labelFilterPanel = new LabelFilterPanel();
        tp.add("Label Filtering", labelFilterPanel);
        
        // Create panels - they will use ConfigManager's shared config objects
        FeatureSamplePanel featureSamplePanel = new FeatureSamplePanel();
        VisualizationPanel visualizationPanel = new VisualizationPanel();
        AITechniquePanel aiTechniquePanel = new AITechniquePanel(
            AITechniquePanel.DEFAULT_MULTILABEL_SCRIPT_PATH,
            AITechniquePanel.DEFAULT_MULTICLASS_SCRIPT_PATH,
            visualizationPanel,
            featureSamplePanel
        );
           PredictionPanel predictionPanel = new PredictionPanel();

           tp.add("Feature and Sample Selection", featureSamplePanel);
           tp.add("Artificial Intelligence Techniques", aiTechniquePanel);
           tp.add("Visualizations", visualizationPanel);
           tp.add("Predictions", predictionPanel);
           f.add(tp);
           f.setVisible(true);
    }

    private void getPreProcessingPanel(JTabbedPane tp, Configuration config) {
        PreProcessingPanel processingPanel = new PreProcessingPanel(tp, config);
    }

    private void getCongigurationPanel(JTabbedPane tp, Configuration config) {
        ConfigurationPanel configPanel = new ConfigurationPanel(tp, config);
    }


    private static boolean isConfigurationValidForLoad(Configuration config) {
        return true;
    }



    public static void main(String[] args) {
        // Initialize shared configuration manager FIRST
        ConfigManager.initialize();
        
        config = new Configuration();
        new Home();
    }



}



























