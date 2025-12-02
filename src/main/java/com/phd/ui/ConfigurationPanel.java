package com.phd.ui;

import com.phd.config.Configuration;
import com.phd.config.DatabaseProperties;
import com.phd.db.DBManager;
import com.phd.issue.FetchData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    static JButton browseDatabaseButton;
    static JFrame f;

    public ConfigurationPanel (JTabbedPane tp, final Configuration config) {

        f = new JFrame();
        JPanel base = new JPanel(new BorderLayout(0, 12));
        base.setBorder(new EmptyBorder(12, 12, 12, 12));
        base.add(createHeader(), BorderLayout.NORTH);

        JPanel formPanel = buildFormPanel();
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        base.add(scrollPane, BorderLayout.CENTER);

        base.add(buildActionsPanel(), BorderLayout.SOUTH);

        tp.add("Configuration", base);
        
        // Load default database location from properties file
        loadDefaultDatabaseLocation();
        
        createActionListener(loadConfiguration, saveConfiguration, createDataSet, browseDatabaseButton);

    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 28, 38));
        header.setBorder(new EmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel("Configuration");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Connect to the repository and database before processing data.");
        subtitle.setForeground(new Color(200, 210, 220));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 5, 10, 5));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        int row = 0;
        sslUsage = new JCheckBox("Use HTTPS when talking to APIs");
        addFormComponent(panel, gbc, row++, "HTTPS Usage", sslUsage);

        certUsage = new JCheckBox("Disable certificate validation (use with caution)");
        addFormComponent(panel, gbc, row++, "Server Certificate", certUsage);

        dbNameField = new JTextArea(1, 30);
        dbNameField.setBorder(BasicBorders.getTextFieldBorder());
        browseDatabaseButton = new JButton("Browse...");
        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        pathPanel.add(dbNameField, BorderLayout.CENTER);
        pathPanel.add(browseDatabaseButton, BorderLayout.EAST);
        addFormComponent(panel, gbc, row++, "Database File", pathPanel);

        accessTokenField = new JTextArea(1, 30);
        accessTokenField.setBorder(BasicBorders.getTextFieldBorder());
        addFormComponent(panel, gbc, row++, "Access Token", accessTokenField);

        repoField = new JTextArea(1, 30);
        repoField.setBorder(BasicBorders.getTextFieldBorder());
        addFormComponent(panel, gbc, row++, "Repository Name", repoField);

        recordFromField = new JTextArea(1, 6);
        recordFromField.setBorder(BasicBorders.getTextFieldBorder());
        addFormComponent(panel, gbc, row++, "Record From", recordFromField);

        recordToField = new JTextArea(1, 6);
        recordToField.setBorder(BasicBorders.getTextFieldBorder());
        addFormComponent(panel, gbc, row++, "Record To", recordToField);

        return panel;
    }

    private void addFormComponent(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel title = new JLabel(label);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        panel.add(title, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private JPanel buildActionsPanel() {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        loadConfiguration = new JButton("Load Configuration");
        saveConfiguration = new JButton("Save Configuration");
        createDataSet = new JButton("Populate SDP Dataset");
        createDataSet.setBackground(new Color(174, 214, 241));
        buttonsRow.add(loadConfiguration);
        buttonsRow.add(saveConfiguration);
        buttonsRow.add(createDataSet);

        JPanel progressRow = new JPanel(new BorderLayout());
        progressRow.setBorder(new EmptyBorder(10, 0, 0, 0));
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(280, 26));
        progressBar.setStringPainted(true);
        progressRow.add(progressBar, BorderLayout.CENTER);

        actionPanel.add(buttonsRow);
        actionPanel.add(progressRow);
        return actionPanel;
    }

    private static void createActionListener(JButton loadConfiguration, JButton saveConfiguration,JButton createDataSet, JButton browseButton) {
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

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select or Create Database File");
                
                // Set initial directory based on current database location
                String currentDbLocation = dbNameField.getText().trim();
                File currentDir;
                if (!currentDbLocation.isEmpty()) {
                    File currentFile = new File(currentDbLocation);
                    currentDir = currentFile.getParentFile();
                    if (currentDir != null && currentDir.exists()) {
                        fileChooser.setCurrentDirectory(currentDir);
                        fileChooser.setSelectedFile(currentFile);
                    }
                } else {
                    // Use project directory as default
                    currentDir = new File(System.getProperty("user.dir"));
                    fileChooser.setCurrentDirectory(currentDir);
                }
                
                // Allow saving to create new database file
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                
                int result = fileChooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    // Ensure .db extension
                    String path = selectedFile.getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".db")) {
                        path += ".db";
                        selectedFile = new File(path);
                    }
                    dbNameField.setText(selectedFile.getAbsolutePath());
                }
            }
        });

    }

    private static void updateUIModel(Configuration config) {
        accessTokenField.setText(config.getAccessToken());
        // Load from properties if config doesn't have a value
        if (config.getDbLocation() == null || config.getDbLocation().trim().isEmpty()) {
            dbNameField.setText(DatabaseProperties.getDatabaseLocation());
        } else {
            dbNameField.setText(config.getDbLocation());
        }
        repoField.setText(config.getRepoName());
        sslUsage.setSelected(config.isUseHttps());
        certUsage.setSelected(config.isValidateServeCertificate());
        recordFromField.setText(Integer.toString(config.getRecordFrom()));
        recordToField.setText(Integer.toString(config.getRecordTo()));
    }
    
    /**
     * Load default database location from properties file into UI
     */
    private static void loadDefaultDatabaseLocation() {
        String defaultLocation = DatabaseProperties.getDatabaseLocation();
        dbNameField.setText(defaultLocation);
        dbNameField.setToolTipText("Default from database.properties: " + DatabaseProperties.getRawDatabaseLocation());
        System.out.println("Loaded default database location: " + defaultLocation);
    }

    private static void setConfiguration() {
        Configuration.getConfig().setAccessToken(accessTokenField.getText().trim());
        
        // Get database location from UI
        String newDbLocation = dbNameField.getText().trim();
        Configuration.getConfig().setDbLocation(newDbLocation);
        
        // If user changed the database location, save it to properties file
        String currentPropertiesLocation = DatabaseProperties.getDatabaseLocation();
        if (!newDbLocation.isEmpty() && !newDbLocation.equals(currentPropertiesLocation)) {
            if (DatabaseProperties.setDatabaseLocation(newDbLocation)) {
                System.out.println("User override: Updated database location in properties file to: " + newDbLocation);
                JOptionPane.showMessageDialog(null, 
                    "Database location updated successfully!\n" +
                    "New location: " + newDbLocation + "\n" +
                    "Saved to: " + DatabaseProperties.getPropertiesFilePath(),
                    "Configuration Saved", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.err.println("Failed to save database location to properties file");
                JOptionPane.showMessageDialog(null, 
                    "Warning: Database location set for this session but not saved to properties file.",
                    "Save Warning", 
                    JOptionPane.WARNING_MESSAGE);
            }
        }
        
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
