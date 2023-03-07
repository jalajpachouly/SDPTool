package com.phd;

import com.phd.data.preprocess.DataPrePorcessingAndValiadtion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class DataPrePorcessingAndValiadtionTest {


    @Test
    void testRemoveStopWordsFromString() {
        String rawData = "Yes, very much. There is a Lucene spatial module that I need to look at. " +
                "There are other solutions running around. When I support that, " +
                "I want that to be very closely integrated in terms that you will have a type: location (or something similar).\n" +
                "\n" +
                "Not sure it will make it into 0.5, but its certainly on the short list for 0.6.0.\n";
        String output = DataPrePorcessingAndValiadtion.removeStopWordsFromString(rawData);
        System.out.println(output);

    }
    @Test
    void testPopulateClassMap(){
        Map<Integer, HashMap<String, String>> classMap = DataPrePorcessingAndValiadtion.populateClassMap(TestConstants.classData.split("\\r?\\n"),100);
        Assertions.assertEquals(classMap.get(100).values().toString().replace("[", "").replace("]",""),"AwsEc2Service.java, AwsS3Service.java");
    }

}