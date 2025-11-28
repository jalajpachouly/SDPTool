package com.phd.data;

import java.util.List;

public class CSVHelper {
    public static String toString(List<String> comments) {
        StringBuilder builder = new StringBuilder();
        for(String str : comments){
            str = str.replace(",", " ");
            str = str.replace("\"", " ");
            str = str.toLowerCase();
            builder.append(str);
        }
        return builder.toString();
    }

    public static String toStr(String  str) {
        if(str==null){
            return "";
        }
            str = str.replaceAll(",", " ");
            str = str.replaceAll("'", "");
            str = str.replaceAll("\\{", "");
            str = str.replaceAll("\\]", "");
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("\\}", "");
            str = str.replaceAll("#", "");
            str = str.replaceAll("\\(", "");
            str = str.replaceAll("\\)", "");
            str = str.replaceAll("\"", " ");
            str = str.replaceAll("`", "");

            str = str.toLowerCase();
            str = str.replaceAll(">", "");
            str = str.replaceAll("\r", " ").replaceAll("\n", " ");

        return str;
    }
}
