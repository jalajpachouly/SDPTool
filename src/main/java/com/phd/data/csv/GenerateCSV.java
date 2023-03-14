package com.phd.data.csv;

import com.phd.db.DBManager;
import com.phd.domain.CSVModel;

import java.util.List;

public class GenerateCSV {
    public List<CSVModel> getRecords(int count){

        return DBManager.getCSVModel(count);
    }
}
