package com.pgno49.salon_project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {


    public enum AppointmentStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }

    private long id;
    private Long customerId;
    private Long serviceId;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String notes;



    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


    public String toCsvString(String delimiter) {
        return id + delimiter +
                (customerId != null ? customerId : "") + delimiter +
                (serviceId != null ? serviceId : "") + delimiter +
                (appointmentDateTime != null ? appointmentDateTime.format(DATE_TIME_FORMATTER) : "") + delimiter +
                (status != null ? status.name() : "") + delimiter +
                escapeCsv(notes);
    }

    // Factory method to create from CSV String
    public static Appointment fromCsvString(String csvLine, String delimiter) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        String[] fields = csvLine.split(delimiter, -1);
        if (fields.length < 6) {
            System.err.println("Skipping malformed Appointment CSV line: " + csvLine);
            return null;
        }
        try {
            long id = Long.parseLong(fields[0]); // ID assumed to be present

            // Parse customerId and serviceId as Long, handle empty strings as null
            Long customerId = (fields[1] != null && !fields[1].trim().isEmpty()) ? Long.parseLong(fields[1]) : null;
            Long serviceId = (fields[2] != null && !fields[2].trim().isEmpty()) ? Long.parseLong(fields[2]) : null;

            LocalDateTime dateTime = null;
            if (fields[3] != null && !fields[3].trim().isEmpty()) {
                try { dateTime = LocalDateTime.parse(fields[3], DATE_TIME_FORMATTER); }
                catch (DateTimeParseException e) { System.err.println("Error parsing date/time: " + fields[3] + " - " + e.getMessage()); }
            }

            AppointmentStatus status = null;
            if (fields[4] != null && !fields[4].trim().isEmpty()) {
                try { status = AppointmentStatus.valueOf(fields[4].toUpperCase()); }
                catch (IllegalArgumentException e) { System.err.println("Error parsing status: " + fields[4] + " - " + e.getMessage()); }
            }

            String notes = unescapeCsv(fields[5]);


            return new Appointment(id, customerId, serviceId, dateTime, status, notes);

        } catch (NumberFormatException e) {
            System.err.println("Error parsing numeric value from Appointment CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing fields from Appointment CSV line (expected 6): " + csvLine + " - " + e.getMessage());
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