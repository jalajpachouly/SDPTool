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

public class PreProcessingPanel extends JPanel {









    static JButton loadConfiguration;
    static JFrame f;
    static JProgressBar progressBar;
    static JLabel progress =null;

    public PreProcessingPanel(JTabbedPane tp, final Configuration config) {

        f = new JFrame();
        progress = new JLabel();

        // create a progressbar
        progressBar = new JProgressBar();
        progressBar.setPreferredSize( new Dimension (700, 30));

        this.setLayout(new FlowLayout(FlowLayout.LEFT));


        JPanel configurationPanel = new JPanel();
        this.add(configurationPanel);
        configurationPanel.setLayout(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.anchor = GridBagConstraints.LINE_START;
        c1.insets = new Insets(10, 5, 5, 5);

        c1.gridx =0;
        c1.gridy = 0;
        JLabel sslLabel = new JLabel();
        sslLabel.setText("1-----------Stop Word Removal");
        configurationPanel.add(sslLabel,c1);

        c1.gridx =1;
        c1.gridy = 0;



        c1.insets = new Insets(3, 5, 5, 5);

        c1.gridx =0;
        c1.gridy = 1;
        JLabel certValidation = new JLabel();
        certValidation.setText("2-----------Lower-casing");
        configurationPanel.add(certValidation,c1);

        c1.gridx =1;
        c1.gridy = 1;



        c1.gridx =0;
        c1.gridy = 2;
        JLabel dbName = new JLabel();
        dbName.setText("3-----------Remove Incomplete and Redundant Data");
        configurationPanel.add(dbName,c1);


        c1.gridx =1;
        c1.gridy = 2;



        c1.gridx =0;
        c1.gridy = 3;
        JLabel accessToken = new JLabel();
        accessToken.setText("4-----------Word Extraction");
        configurationPanel.add(accessToken,c1);


        c1.gridx =1;
        c1.gridy =3;


        c1.gridx =0;
        c1.gridy = 4;
        JLabel repoName = new JLabel();
        repoName.setText("5-----------Data Enrichment and Discretization");
        configurationPanel.add(repoName,c1);


        c1.gridx =0;
        c1.gridy = 5;
        JLabel recordFromLabel = new JLabel();
        recordFromLabel.setText("6-----------Data Labelling");
        configurationPanel.add(recordFromLabel,c1);

        c1.gridx =0;
        c1.gridy = 6;
        JLabel dataValidation = new JLabel();
        recordFromLabel.setText("7-----------Data Validation");
        configurationPanel.add(dataValidation,c1);




        c1.gridx =0;
        c1.gridy =7;
        loadConfiguration = new JButton("Perform Database PreProcessing & Validation");
        configurationPanel.add(loadConfiguration,c1);


        c1.gridx =0;
        c1.gridy =9;

        //progress.setText("Progress :");
        configurationPanel.add(progress,c1);

        c1.gridx =0;
        c1.gridy =9;
        configurationPanel.add(progressBar,c1);

        progressBar.setStringPainted(true);
        //progressBar.setBackground(Color.yellow);


        tp.add("Database PreProcessing & Validation", this);
        createActionListener(loadConfiguration);

    }

    private static void createActionListener(JButton loadConfiguration) {

        loadConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fill();
            }
        });
    }

    // function to increase progress
    public static void fill()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 10;
                try {
                    while (i <= 100) {
                        // fill the menu bar

                        progressBar.setString("1-----------Stop Word Removal");
                        progressBar.setValue(i);

                        // delay the thread
                        Thread.sleep(100);
                        i += 1;
                        if(i==30){
                            System.out.println("");
                        }
                    }
                }
                catch (Exception e) {
                }

            }
        }).start();

    }

    private static void setConfiguration() {


    }

    private static boolean isConfigurationValidForLoad(Configuration config) {
        return true;
    }



    public void repaint() {

    }


}
