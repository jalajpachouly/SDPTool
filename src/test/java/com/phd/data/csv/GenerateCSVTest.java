package com.phd.data.csv;

import com.phd.domain.CSVModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCSVTest {

    @Test
    void writeFile() throws IOException {
        List<CSVModel> models = new ArrayList<>();
        CSVModel model = new CSVModel();
        model.setDefectType("Enhancement");
        model.setComments("comment1 comment2");
        model.setCodeChanges("class1 ,class2");
        model.setDesc("some text");
        model.setResources("jalaj,kamal");
        model.setComplexity(3);
        models.add(model);
        GenerateCSV.writeFile(models);
    }
}