package com.ammonium.adminshop.shop;

import java.util.ArrayList;
import java.util.List;

public class CSVParser {

    public static List<List<String>> parseCSV(String csv){
        List<List<String>> spreadsheet = new ArrayList<>();
        List<String> row = new ArrayList<>();
        boolean isQuote = false;

        int index = 0;
        StringBuilder cell = new StringBuilder();
        while(index < csv.length()){
            if(!isQuote){
                char next = csv.charAt(index);
                switch (next){
                    case ',': //New cell
                        row.add(cell.toString());
                        cell = new StringBuilder();
                        break;
                    case '"':
                        isQuote = true;
                        break;
                    case '\r':
                        break;
                    case '\n':
                        row.add(cell.toString());
                        cell = new StringBuilder();
                        spreadsheet.add(row);
                        row = new ArrayList<>();
                        break;
                    default:
                        cell.append(next);
                        break;
                }
            }
            else{
                char next = csv.charAt(index);
                switch (next){
                    case '"':
                        if(index+1 < csv.length() && csv.charAt(index+1) == '"'){
                            index++;
                            cell.append(next);
                        }else{
                            isQuote = false;
                        }
                        break;
                    default:
                        cell.append(next);
                        break;
                }
            }

            index++;
        }


        row.add(cell.toString());
        spreadsheet.add(row);
        return spreadsheet;
    }
}
