package com.phd.data.csv;

import com.phd.config.Configuration;
import com.phd.db.DBManager;
import com.phd.domain.CSVModel;

import java.io.*;
import java.util.List;

public class GenerateCSV {
    public static List<CSVModel> getRecords(int count){

        return DBManager.getCSVModel(count);
    }

    public static void main(String[] args) throws IOException {
        Configuration config =  Configuration.getConfig();
        config.setDbLocation("C:\\DB\\SDP.db");
        List<CSVModel> models = getRecords(1);
        writeFile(models);
    }

    public static void writeFile(  List<CSVModel> models) throws IOException {
        WriteCsvToDisk.writeFile(models, "c:\\temp\\defect_prediction_new.txt");
        WriteCsvToDisk.writeFileTags(models, "c:\\temp\\defect_prediction_new_tags.txt");
    }
}
