package com.lfms.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Image storage and loading helper. Uploaded images are copied into
 * {@code <user.home>/LFMS/images/} under a random UUID filename; only the filename is
 * stored in the database.
 */
public class ImageUtil {

    private static final String PLACEHOLDER = "/com/lfms/images/placeholder.png";

    private ImageUtil() {
    }

    /**
     * Copies the chosen image into {@code <user.home>/LFMS/images/} with a unique filename.
     * @return the stored filename (not the full path), or null if no file was given.
     */
    public static String copyImageToStorage(File sourceFile) {
        if (sourceFile == null) {
            return null;
        }
        try {
            Files.createDirectories(AppPaths.IMAGES_DIR);
            String ext = extensionOf(sourceFile.getName());
            String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            Path target = AppPaths.IMAGES_DIR.resolve(filename);
            Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image: " + sourceFile.getName(), e);
        }
    }

    /** Loads a full-size image from storage, falling back to the bundled placeholder. */
    public static Image loadImage(String filename) {
        File file = fileFor(filename);
        if (file != null && file.exists()) {
            return new Image(file.toURI().toString(), true);
        }
        return placeholder(0, 0);
    }

    /** Loads a size-bounded thumbnail from storage, falling back to the placeholder. */
    public static Image loadThumbnail(String filename, double w, double h) {
        File file = fileFor(filename);
        if (file != null && file.exists()) {
            return new Image(file.toURI().toString(), w, h, true, true, true);
        }
        return placeholder(w, h);
    }

    private static Image placeholder(double w, double h) {
        URL url = ImageUtil.class.getResource(PLACEHOLDER);
        if (url == null) {
            return null;
        }
        if (w > 0 && h > 0) {
            return new Image(url.toExternalForm(), w, h, true, true, true);
        }
        return new Image(url.toExternalForm());
    }

    private static File fileFor(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        return AppPaths.IMAGES_DIR.resolve(filename).toFile();
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }
}
