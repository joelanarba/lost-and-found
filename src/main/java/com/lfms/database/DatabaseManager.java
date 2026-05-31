package com.lfms.database;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central database bootstrap and connection factory.
 *
 * <p>This is the only class outside the DAO layer that talks to JDBC. It creates the
 * SQLite schema (idempotently) and seeds a default administrator on first run. Every
 * connection it hands out has foreign-key enforcement enabled.</p>
 */
public final class DatabaseManager {

    private static final String DATA_DIR   = "data";
    private static final String IMAGES_DIR = "data/images";
    private static final String DB_FILE    = "data/lfms.db";
    private static final String DB_URL     = "jdbc:sqlite:" + DB_FILE;

    static {
        // Defensive: modern JDBC auto-registers via ServiceLoader, but this guarantees it.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // The driver will still auto-register when present on the classpath.
        }
    }

    private DatabaseManager() {
    }

    /**
     * Opens a fresh connection to the SQLite database with foreign keys enabled.
     * Callers (DAOs) own the returned connection and must close it (try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /**
     * Creates the data directories, builds the schema if absent and seeds the admin.
     * Safe to call on every startup.
     */
    public static void initialize() {
        ensureDirectories();
        createSchema();
        seedAdmin();
    }

    private static void ensureDirectories() {
        try {
            Files.createDirectories(Paths.get(IMAGES_DIR)); // also creates the parent data/ dir
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directories (" + DATA_DIR + ")", e);
        }
    }

    private static void createSchema() {
        final String users = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    name          TEXT NOT NULL,
                    index_no      TEXT NOT NULL UNIQUE,
                    email         TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    phone         TEXT,
                    role          TEXT NOT NULL DEFAULT 'STUDENT',
                    is_active     INTEGER NOT NULL DEFAULT 1,
                    created_at    TEXT NOT NULL DEFAULT (datetime('now'))
                )""";

        final String items = """
                CREATE TABLE IF NOT EXISTS items (
                    item_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id       INTEGER NOT NULL,
                    type          TEXT NOT NULL,
                    name          TEXT NOT NULL,
                    category      TEXT NOT NULL,
                    description   TEXT NOT NULL,
                    location      TEXT,
                    image_path    TEXT,
                    date_reported TEXT,
                    status        TEXT NOT NULL DEFAULT 'OPEN',
                    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )""";

        final String claims = """
                CREATE TABLE IF NOT EXISTS claims (
                    claim_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id       INTEGER NOT NULL,
                    claimant_id   INTEGER NOT NULL,
                    features_desc TEXT NOT NULL,
                    proof_desc    TEXT NOT NULL,
                    status        TEXT NOT NULL DEFAULT 'PENDING',
                    admin_note    TEXT,
                    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (item_id)     REFERENCES items(item_id),
                    FOREIGN KEY (claimant_id) REFERENCES users(user_id)
                )""";

        final String matches = """
                CREATE TABLE IF NOT EXISTS matches (
                    match_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    lost_item_id  INTEGER NOT NULL,
                    found_item_id INTEGER NOT NULL,
                    score         INTEGER NOT NULL,
                    created_at    TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (lost_item_id)  REFERENCES items(item_id),
                    FOREIGN KEY (found_item_id) REFERENCES items(item_id)
                )""";

        final String auditLog = """
                CREATE TABLE IF NOT EXISTS audit_log (
                    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    actor_id    INTEGER,
                    action      TEXT NOT NULL,
                    target_type TEXT,
                    target_id   INTEGER,
                    note        TEXT,
                    timestamp   TEXT NOT NULL DEFAULT (datetime('now'))
                )""";

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(users);
            st.execute(items);
            st.execute(claims);
            st.execute(matches);
            st.execute(auditLog);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }

    /**
     * Inserts the default administrator if no ADMIN user exists yet.
     * Password is BCrypt-hashed (gensalt 12) — identical to how registration hashes,
     * so {@code HashUtil.verify} works against it.
     */
    private static void seedAdmin() {
        final String checkSql  = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        final String insertSql =
                "INSERT INTO users (name, index_no, email, password_hash, phone, role, is_active) "
                        + "VALUES (?, ?, ?, ?, ?, 'ADMIN', 1)";

        try (Connection conn = getConnection()) {
            boolean hasAdmin;
            try (PreparedStatement ps = conn.prepareStatement(checkSql);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                hasAdmin = rs.getInt(1) > 0;
            }

            if (!hasAdmin) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, "System Admin");
                    ps.setString(2, "ADMIN001");
                    ps.setString(3, "admin@lfms.edu");
                    ps.setString(4, BCrypt.hashpw("Admin@1234", BCrypt.gensalt(12)));
                    ps.setString(5, null);
                    ps.executeUpdate();
                }
                System.out.println("[LFMS] Seeded default admin account (admin@lfms.edu / Admin@1234).");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed default admin user", e);
        }
    }
}
