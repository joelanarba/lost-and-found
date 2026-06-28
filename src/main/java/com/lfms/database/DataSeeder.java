package com.lfms.database;

import com.lfms.model.Item;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Utility for generating demo data. Wipes existing tables (except Admin user)
 * and inserts realistic dummy records to populate the system.
 */
public class DataSeeder {

    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] LOCATIONS = {
        "Main Library", "Science Block", "SRC Hall", "Lecture Theatre 1",
        "Cafeteria", "Sports Complex", "Administration Block"
    };

    private static final String[][] ITEMS = {
        {"Laptop", "Electronics", "HP EliteBook 840 G5, silver color with a sticker on the back."},
        {"Wallet", "Personal Items", "Brown leather wallet containing student ID and some cash."},
        {"Keys", "Personal Items", "Set of 3 keys with a red lanyard."},
        {"Smartphone", "Electronics", "iPhone 13 Pro, black case with a cracked screen protector."},
        {"Backpack", "Bags", "Black Nike backpack, contains notebooks and a water bottle."},
        {"Jacket", "Clothing", "Blue denim jacket, size Medium."},
        {"Glasses", "Personal Items", "Reading glasses in a black case."},
        {"Headphones", "Electronics", "Sony WH-1000XM4, black."}
    };

    private static final String[] USERS = {
        "Alice Smith", "Bob Johnson", "Charlie Brown", "Diana Prince", "Evan Wright",
        "Fiona Gallagher", "George Miller", "Hannah Abbott", "Ian Somerhalder", "Julia Roberts"
    };

    public static void seedDemoData() {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Wipe Existing Data
            wipeData(conn);

            // 2. Seed Users
            seedUsers(conn);

            // 3. Seed Items (Lost and Found)
            seedItems(conn);

            // 4. Seed Claims
            seedClaims(conn);

            // 5. Seed Matches (Run the Matching Engine or insert directly)
            seedMatches(conn);

            conn.commit();
            System.out.println("[LFMS] Demo data seeded successfully.");
        } catch (SQLException e) {
            System.err.println("[LFMS] Error seeding demo data: " + e.getMessage());
        }
    }

    private static void wipeData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = OFF");
            st.execute("DELETE FROM matches");
            st.execute("DELETE FROM claims");
            st.execute("DELETE FROM notifications");
            st.execute("DELETE FROM audit_log");
            st.execute("DELETE FROM items");
            st.execute("DELETE FROM users WHERE role != 'ADMIN'");
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    private static void seedUsers(Connection conn) throws SQLException {
        String sql = "INSERT INTO users (name, index_no, email, password_hash, phone, role) VALUES (?, ?, ?, ?, ?, 'STUDENT')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String defaultHash = BCrypt.hashpw("Password123", BCrypt.gensalt(10));
            for (int i = 0; i < 50; i++) {
                String name = i < USERS.length ? USERS[i] : "Student " + i;
                ps.setString(1, name);
                ps.setString(2, "STU" + (1000 + i));
                ps.setString(3, "stu" + (1000 + i) + "@lfms.edu");
                ps.setString(4, defaultHash);
                ps.setString(5, "050000" + String.format("%04d", i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedItems(Connection conn) throws SQLException {
        String sql = "INSERT INTO items (user_id, type, name, category, description, location, date_reported, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 100; i++) {
                int userId = random.nextInt(50) + 2; // Admin is 1, users 2-51
                boolean isLost = random.nextBoolean();
                String type = isLost ? Item.TYPE_LOST : Item.TYPE_FOUND;
                
                String[] template = ITEMS[random.nextInt(ITEMS.length)];
                String name = template[0] + " " + i;
                String category = template[1];
                String description = template[2] + " (Demo Item " + i + ")";
                String location = LOCATIONS[random.nextInt(LOCATIONS.length)];
                
                // Random date within the last 60 days
                LocalDate reportDate = LocalDate.now().minusDays(random.nextInt(60));
                
                String status = Item.STATUS_OPEN;
                if (random.nextDouble() > 0.7) {
                    status = isLost ? Item.STATUS_RESOLVED : Item.STATUS_APPROVED;
                }

                ps.setInt(1, userId);
                ps.setString(2, type);
                ps.setString(3, name);
                ps.setString(4, category);
                ps.setString(5, description);
                ps.setString(6, location);
                ps.setString(7, reportDate.format(DATE_FORMATTER));
                ps.setString(8, status);
                ps.setString(9, reportDate.atTime(12, 0).toString().replace("T", " "));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedClaims(Connection conn) throws SQLException {
        String sql = "INSERT INTO claims (item_id, claimant_id, features_desc, proof_desc, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 20; i++) {
                // Assume first 20 items are found and have claims
                int itemId = i;
                int claimantId = random.nextInt(50) + 2;
                String features = "It has a scratch on the side.";
                String proof = "I have the purchase receipt.";
                String status = random.nextBoolean() ? "APPROVED" : "PENDING";
                LocalDate claimDate = LocalDate.now().minusDays(random.nextInt(30));

                ps.setInt(1, itemId);
                ps.setInt(2, claimantId);
                ps.setString(3, features);
                ps.setString(4, proof);
                ps.setString(5, status);
                ps.setString(6, claimDate.atTime(14, 0).toString().replace("T", " "));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedMatches(Connection conn) throws SQLException {
        String sql = "INSERT INTO matches (lost_item_id, found_item_id, score, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 30; i++) {
                int lostId = random.nextInt(100) + 1;
                int foundId = random.nextInt(100) + 1;
                int score = 4 + random.nextInt(7); // Score between 4 and 10

                ps.setInt(1, lostId);
                ps.setInt(2, foundId);
                ps.setInt(3, score);
                ps.setString(4, LocalDate.now().minusDays(random.nextInt(30)).atTime(10, 0).toString().replace("T", " "));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
