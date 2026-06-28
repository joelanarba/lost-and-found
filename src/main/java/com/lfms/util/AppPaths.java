package com.lfms.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fixed, absolute on-disk locations that the application persists data to.
 *
 * <p>All runtime data lives under <code>&lt;user.home&gt;/LFMS/</code> so it survives every
 * run and is completely independent of the directory the app happens to be launched from.
 * Nothing here uses a relative path. The directories are created at startup (and defensively
 * before the first image write) if they do not already exist.</p>
 *
 * <ul>
 *   <li>{@link #APP_HOME} &mdash; <code>&lt;user.home&gt;/LFMS</code></li>
 *   <li>{@link #DATABASE_FILE} &mdash; <code>&lt;user.home&gt;/LFMS/lfms.db</code></li>
 *   <li>{@link #IMAGES_DIR} &mdash; <code>&lt;user.home&gt;/LFMS/images</code></li>
 *   <li>{@link #QRCODES_DIR} &mdash; <code>&lt;user.home&gt;/LFMS/qrcodes</code></li>
 * </ul>
 */
public final class AppPaths {

    /** Root application data directory: {@code <user.home>/LFMS}. */
    public static final Path APP_HOME = Paths.get(System.getProperty("user.home"), "LFMS");

    /** Uploaded and generated item images: {@code <user.home>/LFMS/images}. */
    public static final Path IMAGES_DIR = APP_HOME.resolve("images");

    /** Generated QR-code PNGs for found items: {@code <user.home>/LFMS/qrcodes}. */
    public static final Path QRCODES_DIR = APP_HOME.resolve("qrcodes");

    /** The SQLite database file: {@code <user.home>/LFMS/lfms.db}. */
    public static final Path DATABASE_FILE = APP_HOME.resolve("lfms.db");

    private AppPaths() {
    }

    /**
     * JDBC URL pointing at the absolute database location. Path separators are normalised to
     * forward slashes, which the SQLite driver accepts on every platform (including Windows).
     */
    public static String databaseUrl() {
        return "jdbc:sqlite:" + DATABASE_FILE.toString().replace(File.separatorChar, '/');
    }

    /**
     * Creates {@link #APP_HOME} and {@link #IMAGES_DIR} if they do not yet exist. Safe to call
     * repeatedly; invoked once at startup before the database connection is opened.
     */
    public static void ensureDirectories() {
        try {
            Files.createDirectories(IMAGES_DIR);   // also creates the parent APP_HOME directory
            Files.createDirectories(QRCODES_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create application data directory: " + APP_HOME, e);
        }
    }
}
