package com.phd.data.validation;

import com.phd.domain.CSVModel;

/*
Ensure we have good quality data generated for machine learning.
 */
public class ValidateFinalRecord {

    public static boolean validate (CSVModel model){
        if(model.getDesc()==null){
            return false;
        }
        if(model.getComments()==null){
            return false;
        }
        if(model.getComplexity()==null){
            return false;
        }
        if(model.getDefectType()==null){
            return false;
        }
        if(model.getResources()==null){
            return false;
        }
        if(model.getTags()==null){
            return false;
        }
        if(model.getCodeChanges()==null){
            return false;
        }
        return true;
    }
}
