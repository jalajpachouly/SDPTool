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

public class ConfigurationPanel {
    public static JButton createDataSet;
    public static JProgressBar progressBar;
    static JTextArea repoField;
    static JTextArea recordFromField;
    static JTextArea recordToField;
    static JCheckBox sslUsage;
    static JCheckBox certUsage;
    static JTextArea dbNameField;
    static JTextArea accessTokenField;
    static JButton saveConfiguration;
    static JButton loadConfiguration;
    static JFrame f;

    public ConfigurationPanel (JTabbedPane tp, final Configuration config) {

        f = new JFrame();
        JPanel base = new JPanel();
        base.setLayout(new FlowLayout(FlowLayout.LEFT));


        JPanel configurationPanel = new JPanel();
        base.add(configurationPanel);
        configurationPanel.setLayout(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.anchor = GridBagConstraints.LINE_START;
        c1.insets = new Insets(10, 5, 5, 5);

        c1.gridx =0;
        c1.gridy = 0;
        JLabel sslLabel = new JLabel();
        sslLabel.setText("Use HTTPS Protocol :");
        configurationPanel.add(sslLabel,c1);

        c1.gridx =1;
        c1.gridy = 0;
        sslUsage = new JCheckBox();
        configurationPanel.add(sslUsage,c1);

        c1.insets = new Insets(3, 5, 5, 5);

        c1.gridx =0;
        c1.gridy = 1;
        JLabel certValidation = new JLabel();
        certValidation.setText("Disable Server Certificate Validation :");
        configurationPanel.add(certValidation,c1);

        c1.gridx =1;
        c1.gridy = 1;
        certUsage = new JCheckBox();
        configurationPanel.add(certUsage,c1);

        c1.gridx =0;
        c1.gridy = 2;
        JLabel dbName = new JLabel();
        dbName.setText("*Full path of the Database :");
        configurationPanel.add(dbName,c1);


        c1.gridx =1;
        c1.gridy = 2;
        dbNameField = new JTextArea(1,35);
        dbNameField.setBorder(BasicBorders.getTextFieldBorder());
        configurationPanel.add(dbNameField,c1);


        Icon icon = new ImageIcon("C:\\PROJECTS\\Objective1\\src\\main\\resources\\folder.png");
        JLabel selectDBIcon = new JLabel();
        selectDBIcon.setIcon(icon);
        c1.gridx =2;
        c1.gridy = 2;
        configurationPanel.add(selectDBIcon,c1);




        c1.gridx =0;
        c1.gridy = 3;
        JLabel accessToken = new JLabel();
        accessToken.setText("Enter the Access Token :");
        configurationPanel.add(accessToken,c1);


        c1.gridx =1;
        c1.gridy =3;
        accessTokenField = new JTextArea(1,35);
        accessTokenField.setBorder(BasicBorders.getTextFieldBorder());
        configurationPanel.add(accessTokenField,c1);


        c1.gridx =0;
        c1.gridy = 4;
        JLabel repoName = new JLabel();
        repoName.setText("Enter Open Source Repository Name :");
        configurationPanel.add(repoName,c1);


        c1.gridx =1;
        c1.gridy =4;
        repoField = new JTextArea(1,35);
        repoField.setBorder(BasicBorders.getTextFieldBorder());
        configurationPanel.add(repoField,c1);


        c1.gridx =0;
        c1.gridy = 5;
        JLabel recordFromLabel = new JLabel();
        recordFromLabel.setText("Record From :");
        configurationPanel.add(recordFromLabel,c1);

        c1.gridx =1;
        c1.gridy =5;
        recordFromField = new JTextArea(1,5);
        recordFromField.setBorder(BasicBorders.getTextFieldBorder());
        configurationPanel.add(recordFromField,c1);


        c1.gridx =0;
        c1.gridy = 6;
        JLabel recordToLabel = new JLabel();
        recordToLabel.setText("Record To :");
        configurationPanel.add(recordToLabel,c1);

        c1.gridx =1;
        c1.gridy =6;
        recordToField = new JTextArea(1,5);
        recordToField.setBorder(BasicBorders.getTextFieldBorder());
        configurationPanel.add(recordToField,c1);







        JPanel buttonPanel = new JPanel();
        base.add(buttonPanel);
        c1.gridx =0;
        c1.gridy =7;
        loadConfiguration = new JButton("Load Configuration");
        buttonPanel.add(loadConfiguration,c1);

        c1.gridx =1;
        c1.gridy =7;
        saveConfiguration = new JButton("Save Configuration");
        buttonPanel.add(saveConfiguration,c1);

        c1.gridx =2;
        c1.gridy =7;
        createDataSet = new JButton("Populate SDP Dataset");
        buttonPanel.add(createDataSet,c1);
        createDataSet.setBackground(Color.CYAN);


        c1.gridx =3;
        c1.gridy =7;
        progressBar =  new JProgressBar();
        progressBar.setPreferredSize( new Dimension (300, 30));
        progressBar.setStringPainted(true);
        buttonPanel.add(progressBar,c1);
        createDataSet.setBackground(Color.CYAN);


        loadConfiguration.setSize(createDataSet.getSize());
        saveConfiguration.setSize(createDataSet.getSize());

        createActionListener(loadConfiguration,saveConfiguration,createDataSet,selectDBIcon);

        tp.add("Configuration", base);

    }

    private static void createActionListener(JButton loadConfiguration, JButton saveConfiguration,JButton createDataSet, JLabel label) {
        saveConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setConfiguration();
                if(isConfigurationValidForLoad(Configuration.config)) {
                    DBManager.saveConfiguration(Configuration.config);
                }
            }
        });

        createDataSet.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                FetchData thread = new FetchData();
                thread.start();
                createDataSet.setBackground(Color.red);
                f.invalidate();
            }
        });

        loadConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setConfiguration();
                if(isConfigurationValidForLoad(Configuration.getConfig())) {
                    Configuration.config = DBManager.loadConfiguration(Configuration.getConfig());
                    Configuration.setConfig(Configuration.getConfig());
                    updateUIModel(Configuration.getConfig());
                    f.invalidate();
                }
            }
        });

        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Database File");
                fileChooser.setCurrentDirectory(new File("C:\\SQLLITE\\DB"));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    dbNameField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

    }

    private static void updateUIModel(Configuration config) {
        accessTokenField.setText(config.getAccessToken());
        dbNameField.setText(config.getDbLocation());
        repoField.setText(config.getRepoName());
        sslUsage.setSelected(config.isUseHttps());
        certUsage.setSelected(config.isValidateServeCertificate());
        recordFromField.setText(Integer.toString(config.getRecordFrom()));
        recordToField.setText(Integer.toString(config.getRecordTo()));

    }

    private static void setConfiguration() {
        Configuration.getConfig().setAccessToken(accessTokenField.getText().trim());
        Configuration.getConfig().setDbLocation(dbNameField.getText().trim());
        Configuration.getConfig().setRepoName(repoField.getText().trim());
        Configuration.getConfig().setUseHttps(sslUsage.isSelected());
        Configuration.getConfig().setValidateServeCertificate(certUsage.isSelected());
        if(recordFromField.getText().trim().length()==0){
            recordFromField.setText("1");
        }
        else {
            Configuration.getConfig().setRecordFrom(Integer.parseInt(recordFromField.getText()));
        }
        if(recordToField.getText().trim().length()==0){
            recordToField.setText("10");
        }
        else{
            Configuration.getConfig().setRecordTo(Integer.parseInt(recordToField.getText()));
        }

    }

    private static boolean isConfigurationValidForLoad(Configuration config) {
        return true;
    }

    private static boolean isConfigurationValid(Configuration config) {
        if(config.getDbLocation()!=null) {
            return true;
        }
        else{
            return false;
        }
    }

    public void repaint() {
        updateUIModel(Configuration.getConfig());
    }


}
