package com.phd.ui;

import com.phd.config.Configuration;
import com.phd.data.preprocess.DataPrePorcessingAndValiadtion;
import com.phd.db.DBManager;
import com.phd.domain.CodeChanges;
import com.phd.domain.Comments;
import com.phd.issue.Issue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class PreProcessingPanel extends JPanel {

    static JButton performDataPreProcessingButton;
    static JFrame f;
    static JProgressBar progressBar;
    static JLabel progress =null;
    static SimpleDateFormat formatter = new SimpleDateFormat(
            "EEEE, dd/MM/yyyy/hh:mm:ss");

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
        performDataPreProcessingButton = new JButton("Perform Database PreProcessing & Validation");
        configurationPanel.add(performDataPreProcessingButton,c1);


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
        createActionListener(performDataPreProcessingButton);

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

                processDataValidationForComments();
                processDataValidationForIssues();
                processDataValidationForCodeChanges();

            }
        }).start();

    }

    private static void processDataValidationForCodeChanges() {
        int i = 10;
        int j=0;
        //Get all the comments from the Comment Table and remove the Stop Words
        List<CodeChanges> listOfCodeChanges = DBManager.getListOfCodeChanges();
        try {
            while (j < listOfCodeChanges.size()) {

                CodeChanges codeChanges = listOfCodeChanges.get(j);
                DataPrePorcessingAndValiadtion.processCodeChanges(codeChanges);
                i += 1;
                j +=1;
                if(i ==30){
                    System.out.println("");
                }
                // fill the menu bar
                progressBar.setValue(i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static long betweenDates(Date firstDate, Date secondDate) throws IOException
    {
        return ChronoUnit.DAYS.between(firstDate.toInstant(), secondDate.toInstant());
    }

    private static void processDataValidationForIssues() {
        progressBar.setString("----Database PreProcessing & Validation For Issues-----");
        int i = 10;
        int j=0;
        /*  1- Get all the Issues from the Issue Table
            2- Remove the Stop Words and update the processed data in the table
            3-Enrich the Issue Table, populate the total time
         */
        List<Issue> listOfIssue = DBManager.getListOfIssues();
        try {
            while (j < listOfIssue.size()) {

                Issue issue = listOfIssue.get(j);
                Date openDate = formatter.parse(issue.getCreatedAt());
                Date closeDate = formatter.parse(issue.getClosedAt());
                long timeTaken = betweenDates(closeDate, openDate);
                issue.setTimeTakenToFix(timeTaken);
                issue.setProcessedTitle(DataPrePorcessingAndValiadtion.removeStopWordsFromString(issue.getTitle()));
                issue.setProcessedBody(DataPrePorcessingAndValiadtion.removeStopWordsFromString(issue.getBody()));
                DBManager.updateIssue(issue);
                i += 1;
                j +=1;
                if(i ==30){
                    System.out.println("");
                }
                // fill the menu bar
                progressBar.setValue(i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDataValidationForComments() {
        int i = 10;
        int j=0;
        //Get all the comments from the Comment Table and remove the Stop Words
        List<Comments> listOfComments = DBManager.getListOfComments();
        try {
            while (j < listOfComments.size()) {

                Comments comments = listOfComments.get(j);
                comments.setProcessedComments(DataPrePorcessingAndValiadtion.removeStopWordsFromString(comments.getComment()));
                DBManager.updateComment(comments);
                i += 1;
                j +=1;
                if(i ==30){
                    System.out.println("");
                }
                // fill the menu bar
                progressBar.setValue(i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setConfiguration() {


    }

    private static boolean isConfigurationValidForLoad(Configuration config) {
        return true;
    }



    public void repaint() {

    }


}
