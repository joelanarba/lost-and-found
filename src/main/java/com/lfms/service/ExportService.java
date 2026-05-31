package com.lfms.service;

import com.lfms.model.AuditLog;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.User;
import com.lfms.util.CsvExporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports domain data to CSV files chosen by the user via a FileChooser.
 */
public class ExportService {

    public void exportUsers(List<User> users, File destination) throws IOException {
        String[] headers = {"User ID", "Name", "Index No", "Email", "Phone", "Role", "Status", "Joined"};
        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            rows.add(new String[]{
                    String.valueOf(u.getUserId()),
                    nz(u.getName()),
                    nz(u.getIndexNo()),
                    nz(u.getEmail()),
                    nz(u.getPhone()),
                    nz(u.getRole()),
                    u.isActive() ? "Active" : "Inactive",
                    nz(u.getCreatedAt())
            });
        }
        CsvExporter.write(destination, headers, rows);
    }

    public void exportItems(List<Item> items, File destination) throws IOException {
        String[] headers = {"Item ID", "Type", "Name", "Category", "Description", "Location",
                "Status", "Date Reported", "Reporter ID", "Created At"};
        List<String[]> rows = new ArrayList<>();
        for (Item i : items) {
            rows.add(new String[]{
                    String.valueOf(i.getItemId()),
                    nz(i.getType()),
                    nz(i.getName()),
                    nz(i.getCategory()),
                    nz(i.getDescription()),
                    nz(i.getLocation()),
                    nz(i.getStatus()),
                    nz(i.getDateReported()),
                    String.valueOf(i.getUserId()),
                    nz(i.getCreatedAt())
            });
        }
        CsvExporter.write(destination, headers, rows);
    }

    public void exportClaims(List<Claim> claims, File destination) throws IOException {
        String[] headers = {"Claim ID", "Item", "Claimant", "Index No", "Email", "Status",
                "Admin Note", "Submitted"};
        List<String[]> rows = new ArrayList<>();
        for (Claim c : claims) {
            rows.add(new String[]{
                    String.valueOf(c.getClaimId()),
                    nz(c.getItemName()),
                    nz(c.getClaimantName()),
                    nz(c.getClaimantIndexNo()),
                    nz(c.getClaimantEmail()),
                    nz(c.getStatus()),
                    nz(c.getAdminNote()),
                    nz(c.getCreatedAt())
            });
        }
        CsvExporter.write(destination, headers, rows);
    }

    public void exportAuditLog(List<AuditLog> logs, File destination) throws IOException {
        String[] headers = {"Log ID", "Timestamp", "Actor", "Action", "Target Type", "Target ID", "Note"};
        List<String[]> rows = new ArrayList<>();
        for (AuditLog log : logs) {
            rows.add(new String[]{
                    String.valueOf(log.getLogId()),
                    nz(log.getTimestamp()),
                    nz(log.getActorName()),
                    nz(log.getAction()),
                    nz(log.getTargetType()),
                    log.getTargetId() > 0 ? String.valueOf(log.getTargetId()) : "",
                    nz(log.getNote())
            });
        }
        CsvExporter.write(destination, headers, rows);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
