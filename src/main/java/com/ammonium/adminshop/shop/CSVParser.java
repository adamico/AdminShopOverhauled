package com.ammonium.adminshop.shop;

import java.util.ArrayList;
import java.util.List;

public class CSVParser {
    public static List<List<String>> parseCSV(String csv) {
        List<List<String>> spreadsheet = new ArrayList<>();
        List<String> row = new ArrayList<>();
        boolean isQuote = false;
        StringBuilder cell = new StringBuilder();
        int index = 0;
        while (index < csv.length()) {
            char next = csv.charAt(index);
            if (!isQuote) {
                switch (next) {
                    case ',':
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
            } else {
                switch (next) {
                    case '"':
                        if (index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                            cell.append('"');
                            index++;
                        } else {
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
        if(cell.length() > 0 || isQuote) { // Handling the last cell/row
            row.add(cell.toString());
            spreadsheet.add(row);
        }
        return spreadsheet;
    }
}
