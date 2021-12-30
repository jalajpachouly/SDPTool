package com.phd.ui;

import com.phd.config.Configuration;
import com.phd.db.DBManager;
import com.phd.issue.FetchData;

import javax.swing.*;
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
        tp.setBounds(0, 0, 840, 325);
        f.setTitle("Software Defect Prediction...");
        f.setSize(840, 325);
        getCongigurationPanel(tp, config);
        getPreProcessingPanel(tp,config);
        JPanel p3 = new JPanel();
        JPanel p4 = new JPanel();
        JPanel p5 = new JPanel();


        tp.add("Feature and Sample Selection", p3);
        tp.add("Artificial Intelligence Techniques", p4);
        tp.add("Predictions", p5);
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
        config = new Configuration();
        new Home();
    }



}



























