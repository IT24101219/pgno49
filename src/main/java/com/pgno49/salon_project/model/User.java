package com.pgno49.salon_project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {


    public enum Role {
        CUSTOMER,
        STAFF_PENDING,
        STAFF_APPROVED,
        MAIN_ADMIN
    }


    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        PENDING_APPROVAL,
        REJECTED
    }

    private long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String fullName;
    private Role role;
    private AccountStatus status;


    public String toCsvString(String delimiter) {
        return id + delimiter +
                escapeCsv(username) + delimiter +
                escapeCsv(email) + delimiter +
                escapeCsv(phoneNumber) + delimiter +
                escapeCsv(passwordHash) + delimiter +
                escapeCsv(fullName) + delimiter +
                (role != null ? role.name() : "") + delimiter +
                (status != null ? status.name() : "");
    }

    public static User fromCsvString(String csvLine, String delimiter) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            return null;
        }
        String[] fields = csvLine.split(delimiter, -1);

        if (fields.length < 8) {
            System.err.println("Skipping malformed User CSV line: " + csvLine + " (Expected 8 fields, found " + fields.length + ")");
            return null;
        }
        try {
            long id = Long.parseLong(fields[0]);
            String username = unescapeCsv(fields[1]);
            String email = unescapeCsv(fields[2]);
            String phoneNumber = unescapeCsv(fields[3]);
            String passwordHash = unescapeCsv(fields[4]);
            String fullName = unescapeCsv(fields[5]);

            Role role = null;
            if (fields[6] != null && !fields[6].trim().isEmpty()) {
                try { role = Role.valueOf(fields[6].toUpperCase()); }
                catch (IllegalArgumentException e) { System.err.println("Invalid role in User CSV: " + fields[6]); }
            }

            AccountStatus status = null;
            if (fields[7] != null && !fields[7].trim().isEmpty()) {
                try { status = AccountStatus.valueOf(fields[7].toUpperCase()); }
                catch (IllegalArgumentException e) { System.err.println("Invalid status in User CSV: " + fields[7]); }
            }


            return new User(id, username, email, phoneNumber, passwordHash, fullName, role, status);

        } catch (NumberFormatException e) {
            System.err.println("Error parsing user ID from CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing fields from User CSV line (expected 8): " + csvLine + " - " + e.getMessage());
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