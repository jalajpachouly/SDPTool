package com.phd.ui;

import com.phd.config.Configuration;
import com.phd.data.DataExporter;
import com.phd.data.preprocess.DataPrePorcessingAndValiadtion;
import com.phd.db.DBManager;
import com.phd.domain.CodeChanges;
import com.phd.domain.Comments;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class PreProcessingPanel extends JPanel {

    private static final String[] PIPELINE_STEPS = {
            "Stop Word Removal",
            "Lower-casing",
            "Remove Incomplete and Redundant Data",
            "Word Extraction",
            "Data Enrichment and Discretization",
            "Data Labelling",
            "Data Validation"
    };

    static JButton performDataPreProcessingButton;
    static JButton exportMultilabelButton;
    static JButton exportMulticlassButton;
    static JProgressBar progressBar;
    static JLabel statusLabel;

    public PreProcessingPanel(JTabbedPane tp, Configuration config) {

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(600, 26));
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Status: Waiting to start");

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        tp.add("Database PreProcessing & Validation", this);
        createActionListener(performDataPreProcessingButton);

    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 28, 38));
        header.setBorder(new EmptyBorder(12, 15, 12, 15));
        JLabel title = new JLabel("Database PreProcessing & Validation");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Run the cleansing pipeline before generating features.");
        subtitle.setForeground(new Color(200, 210, 220));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JComponent createBody() {
        JPanel cardsPanel = new JPanel(new GridLayout(0, 2, 12, 12));
        cardsPanel.setBorder(new EmptyBorder(20, 0, 10, 0));

        for (int i = 0; i < PIPELINE_STEPS.length; i++) {
            cardsPanel.add(createStepCard(i + 1, PIPELINE_STEPS[i]));
        }

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        return scrollPane;
    }

    private JPanel createStepCard(int index, String description) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 223, 230)),
                new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel badge = new JLabel(String.format("STEP %02d", index));
        badge.setFont(badge.getFont().deriveFont(Font.BOLD));
        badge.setForeground(new Color(66, 84, 102));
        JLabel text = new JLabel(description);
        text.setFont(text.getFont().deriveFont(Font.PLAIN, 13f));
        card.add(badge, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JComponent createFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel hint = new JLabel("This workflow may take a few minutes depending on dataset size.");
        hint.setForeground(new Color(80, 90, 102));
        footer.add(hint, BorderLayout.NORTH);

        // Create buttons panel with horizontal layout
        performDataPreProcessingButton = new JButton("Perform Database PreProcessing & Validation");
        exportMultilabelButton = new JButton("Export Multilabel Dataset");
        exportMulticlassButton = new JButton("Export Multiclass Dataset");
        
        // Style export buttons
        exportMultilabelButton.setBackground(new Color(46, 125, 50)); // Green
        exportMultilabelButton.setForeground(Color.WHITE);
        exportMulticlassButton.setBackground(new Color(25, 118, 210)); // Blue
        exportMulticlassButton.setForeground(Color.WHITE);
        
        // Add action listeners for export buttons
        exportMultilabelButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                DataExporter.exportMultilabel((JFrame) SwingUtilities.getWindowAncestor(PreProcessingPanel.this));
            });
        });
        
        exportMulticlassButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                DataExporter.exportMulticlass((JFrame) SwingUtilities.getWindowAncestor(PreProcessingPanel.this));
            });
        });
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.add(performDataPreProcessingButton);
        controls.add(exportMultilabelButton);
        controls.add(exportMulticlassButton);
        footer.add(controls, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressPanel.add(statusLabel);
        progressPanel.add(Box.createVerticalStrut(6));
        progressPanel.add(progressBar);
        footer.add(progressPanel, BorderLayout.SOUTH);

        return footer;
    }

    private static void createActionListener(JButton loadConfiguration) {

        loadConfiguration.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performDataPreProcessingButton.setEnabled(false);
                progressBar.setValue(0);
                statusLabel.setText("Status: Running preprocessing steps...");
                fill();
            }
        });
    }

    // function to increase progress
    public static void fill()
    {
        SwingWorker sw1 = new SwingWorker()
        {

            @Override
            protected String doInBackground() throws Exception
            {
                publish(20);
                processDataValidationForComments();
                publish(40);
                DataPrePorcessingAndValiadtion.processDataValidationForIssues();
                publish(60);
                processDataValidationForCodeChanges();
                publish(90);
                return null;
            }

            @Override
            protected void process(List chunks)
            {
                int val = (int) chunks.get(chunks.size()-1);
                progressBar.setValue(val);

            }

            @Override
            protected void done()
            {
                progressBar.setValue(100);
                statusLabel.setText("Status: Completed");
                performDataPreProcessingButton.setEnabled(true);
            }
        };

        // executes the swingworker on worker thread
        sw1.execute();

    }

    private static void processDataValidationForCodeChanges() {
        int j=0;
        //Get all the comments from the Comment Table and remove the Stop Words
        List<CodeChanges> listOfCodeChanges = DBManager.getListOfCodeChanges();
        try {
            while (j < listOfCodeChanges.size()) {

                CodeChanges codeChanges = listOfCodeChanges.get(j);
                DataPrePorcessingAndValiadtion.processCodeChanges(codeChanges);
                   j +=1;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }




    public  static void processDataValidationForComments() {

        //Get all the comments from the Comment Table and remove the Stop Words
        List<Comments> listOfComments = DBManager.getListOfComments();
        DataPrePorcessingAndValiadtion.processComments(listOfComments);
    }



    public void repaint() {

    }


}
