package com.pgno49.salon_project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    private long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int durationInMinutes;

    public String toCsvString(String delimiter) {
        return id + delimiter +
                escapeCsv(name) + delimiter +
                escapeCsv(description) + delimiter +
                (price != null ? price.toPlainString() : "") + delimiter +
                durationInMinutes;
    }

    public static Service fromCsvString(String csvLine, String delimiter) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        String[] fields = csvLine.split(delimiter, -1);
        if (fields.length < 5) {
            System.err.println("Skipping malformed Service CSV line: " + csvLine);
            return null;
        }
        try {
            long id = Long.parseLong(fields[0]);
            String name = unescapeCsv(fields[1]);
            String description = unescapeCsv(fields[2]);

            BigDecimal price = (fields[3] != null && !fields[3].trim().isEmpty()) ? new BigDecimal(fields[3]) : BigDecimal.ZERO;
            int durationInMinutes = Integer.parseInt(fields[4]);
            return new Service(id, name, description, price, durationInMinutes);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value from Service CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing fields from Service CSV line (expected 5): " + csvLine + " - " + e.getMessage());
            return null;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String result = value.replace("\"", "\"\"");
        if (result.contains(",") || result.contains("\"") || result.contains("\n")) {
            result = "\"" + result + "\"";
        }
        return result;
    }

    private static String unescapeCsv(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            value = value.replace("\"\"", "\"");
        }
        return value;
    }
}