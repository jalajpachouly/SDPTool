package com.phd.data.preprocess;

import com.phd.domain.CodeChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataPrePorcessingAndValiadtion {
    private static List<String> stopwords;

    static
    {
        try {
            stopwords = Files.readAllLines(Paths.get("C:\\SDPTool\\src\\main\\resources\\english-stop-word.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String removeStopWordsFromString(String rawData)
    {
        String data = rawData.toLowerCase();
        ArrayList<String> allWords = Stream.of(data.split(" "))
                .collect(Collectors.toCollection(ArrayList<String>::new));
        allWords.removeAll(stopwords);
        StringBuilder builder = new StringBuilder();
        for(String temp: allWords){
            builder.append(temp).append(" ");
        }
        return builder.toString();
    }

    public static void processCodeChanges(CodeChanges codeChanges)
    {
        // Get the name of the package
        String packageName;
        // Get the total number of classes
        List<String> classNames = new ArrayList<String>();
        // Get the total number of lines changed
        int totalLineChanged;
        // calculate the relative complexity
        int complexity;

        // Update the changes in the CODE_DETAILS table

        // Update the classes used in the defect in the CLASS_DETAILS table

    }

}
