package com.phd.data.csv;

import com.phd.domain.CSVModel;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WriteCsvToDisk {
    public static void writeFile(List<CSVModel> models, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            bw.write(",Text");
            bw.newLine();
            for (CSVModel model : models) {
                bw.write(model.getText());
            }
        }
    }

    public static void writeFileTags(  List<CSVModel> models, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            bw.write(",Tags");
            bw.newLine();

            for (CSVModel model : models) {
                bw.write(model.getTagsData());
            }
        }
    }
}
