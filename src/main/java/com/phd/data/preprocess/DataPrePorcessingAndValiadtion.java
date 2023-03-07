package com.phd.data.preprocess;

import com.phd.db.DBManager;
import com.phd.domain.CodeChangeDetails;
import com.phd.domain.CodeChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataPrePorcessingAndValiadtion {
    private static List<String> stopwords;

    static
    {
        try {
            stopwords = Files.readAllLines(Paths.get("C:\\GitHub\\SDPTool\\src\\main\\resources\\english-stop-word.txt"));
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

    public static void processCodeChanges(CodeChanges codeChanges) throws SQLException {
        Map<Integer, HashMap<String, String>> packageMap = new HashMap<Integer, HashMap<String, String>>();
        Map<Integer, HashMap<String, String>> classMap = new HashMap<Integer, HashMap<String, String>>();
        Map<Integer, HashMap<String, String>> authorMap = new HashMap<Integer, HashMap<String, String>>();
        // Get the name of the package
        String packageName;
        String className;

        int totalLineChanged = 0;
        int numberOfAuthors = 0;

        // Get the total number of classes
        String lines[] = codeChanges.getCodeChange().split("\\r?\\n");
        classMap = populateClassMap(lines, codeChanges.getIssueId());
        packageMap = populatePackageMap(lines, codeChanges.getIssueId());
        //populateAuthorMap(lines, authorMap, codeChanges.getIssueId());
        numberOfAuthors = getNumberOfAuthors(lines);
        totalLineChanged = getTotalLinesChanges(lines);

        // calculate the relative complexity
        int complexity = getCodeChangeComplexity(totalLineChanged, packageMap.size(), classMap.size());
        CodeChangeDetails codeDetails = new CodeChangeDetails();
        codeDetails.setIssueId(codeChanges.getIssueId());
        codeDetails.setNoOfClass(classMap.size());
        codeDetails.setNoOfPackages(packageMap.size());
        codeDetails.setNoOfLines(totalLineChanged);
        codeDetails.setNoOfAuthors(authorMap.size());
        codeDetails.setChangeComplexity(complexity);

        // Update the changes in the CODE_DETAILS table
        DBManager.insertCodeChangeDetails(codeDetails);

        // Update the classes used in the defect in the CLASS_DETAILS table
        if (classMap.size() > 0) {
            DBManager.insertClassDetails(classMap);
        }

        // Update the packages used in the defect in the PACKAGE_DETAILS table
        if (packageMap.size() > 0) {
            DBManager.insertPackageDetails(packageMap);
        }

        // Update the Authors name in the AUTHOR_DETAILS table
        if (authorMap.size() > 0) {
            DBManager.insertAuthor(authorMap);
        }

    }

    public static int getTotalLinesChanges(String[] lines) {
        int totalLineChanged =0;
        for (int lineNo = 0; lineNo < lines.length; lineNo++) {
            String line = lines[lineNo];
            // Get the total number of lines changed
            if (line.length() > 2 && (line.substring(0, 1).equals("+"))
                    && (!line.substring(1, 2).equals("+"))
            ) {
                totalLineChanged++;
            }

        }
        return totalLineChanged;
    }

    public static int getNumberOfAuthors(String[] lines) {
        int numberOfAuthors =0;
        for (int lineNo = 0; lineNo < lines.length; lineNo++) {
            String line = lines[lineNo];
            if (line.contains("@author")) {
                numberOfAuthors++;
            }
        }
        return numberOfAuthors;
    }


    public static void populateAuthorMap(String[] lines, Map<Integer, HashMap<String, String>> authorMap, int issueId) {
        for (int lineNo = 0; lineNo < lines.length; lineNo++) {
            String line = lines[lineNo];
            if (line.contains("@author")) {
                int authorIndex = line.indexOf("@author");
                line = line.substring(authorIndex, line.length());
                String authorName = line.replace("@author", "");
                if (authorName != null && authorName.trim().length() > 0) {
                    if (authorMap.get(issueId) != null) {
                        authorMap.get(issueId).put(authorName, authorName);
                    } else {
                        HashMap<String, String> newMap = new HashMap<>();
                        newMap.put(authorName, authorName);
                        authorMap.put(issueId, newMap);
                    }
                }
            }
        }
    }

    public static Map<Integer,HashMap<String, String>>  populatePackageMap(String[] lines,  int issueId) {
        Map<Integer,HashMap<String, String>> packageMap = new HashMap<>();
        for (int lineNo = 0; lineNo < lines.length; lineNo++) {
            String line = lines[lineNo];
            String className = getPackageName(line);
            if (className != null && className.trim().length() > 0) {
                if (packageMap.get(issueId) != null) {
                    packageMap.get(issueId).put(className, className);
                } else {
                    HashMap<String, String> newMap = new HashMap<>();
                    newMap.put(className, className);
                    packageMap.put(issueId, newMap);
                }

            }
        }
        return packageMap;
    }

    public static Map<Integer, HashMap<String, String>> populateClassMap(String[] lines, int issueId) {
        Map<Integer, HashMap<String, String>> classMap = new HashMap<>();
        for (int lineNo = 0; lineNo < lines.length; lineNo++) {
            String line = lines[lineNo];
            String className = getClassName(line);
            if (className != null && className.trim().length() > 0) {
                if (classMap.get(issueId) != null) {
                    classMap.get(issueId).put(className, className);
                } else {
                    HashMap<String, String> newMap = new HashMap<>();
                    newMap.put(className, className);
                    classMap.put(issueId, newMap);
                }

            }
        }
        return classMap;
    }


    private static int getCodeChangeComplexity(int totalLineChanged, int packageSize, int classSize ) {
        //LOW       : 1
        // MEDIUM   : 2
        //HIGH      : 3
        int complexity = 0;
        if ( totalLineChanged < 20  && packageSize <3 && classSize <5){
            complexity = 1;
        }
        else
        if ( totalLineChanged < 30  && packageSize <6 && classSize <10){
            complexity = 2;
        }
        complexity = 3;
        if(totalLineChanged>5000){
            complexity = 2;
        }
        if(totalLineChanged>10000){
            complexity = 1;
        }

        return complexity;
    }

    private static String getClassName(String line) {
        if(line.startsWith(("+++ b"))){
            int lastIndex = line.lastIndexOf("/");
            if(lastIndex>0){
                String className = line.substring(lastIndex+1, line.length());
                return className;
            }

        }
        return null;
    }

    private static String getPackageName(String line) {
        if(line.startsWith(("+++ b"))){
            int lastIndex = line.lastIndexOf("/");
            ;            if(lastIndex>0){
                String packageName = line.substring(0,lastIndex+1);
                packageName= packageName.replace("+++ b/","");
                return packageName;
            }
        }
        return null;
    }

}
