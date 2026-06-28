package com.lfms.util;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Provides an image for a seed item. Prefers a real, bundled stock photo mapped from the
 * item's name/category (see {@code resources/com/lfms/images/seed/}); if nothing maps or the
 * resource is missing, it falls back to a generated Java2D icon — a coloured gradient keyed to
 * the category, a white vector icon and a caption. Either way no network is needed at runtime,
 * so seeding works fully offline.
 *
 * <p>Each call returns a temporary file; callers feed it through the normal
 * {@code ItemService.reportItem(...)} path, which copies it into
 * {@code <user.home>/LFMS/images/} under a UUID name.</p>
 */
public final class SeedImageFactory {

    private static final int W = 640;
    private static final int H = 440;

    private SeedImageFactory() {
    }

    private static final String SEED_DIR = "/com/lfms/images/seed/";

    /**
     * Ordered, most-specific-first mapping from a substring of the item name to a bundled
     * photo. The first keyword contained in the (lower-cased) item name wins.
     */
    private static final String[][] KEYWORD_PHOTOS = {
        {"power bank",    "powerbank.jpg"},
        {"charger",       "charger.jpg"},
        {"flash drive",   "flashdrive.jpg"},
        {"earbud",        "earbuds.jpg"},
        {"laptop",        "laptop.jpg"},
        {"phone",         "phone.jpg"},
        {"sunglass",      "sunglasses.jpg"},
        {"glass",         "glasses.jpg"},
        {"watch",         "watch.jpg"},
        {"bracelet",      "bracelet.jpg"},
        {"bottle",        "bottle.jpg"},
        {"backpack",      "backpack.jpg"},
        {"bag",           "backpack.jpg"},
        {"key",           "keys.jpg"},
        {"calculator",    "calculator.jpg"},
        {"notebook",      "notebook.jpg"},
        {"textbook",      "textbook.jpg"},
        {"book",          "textbook.jpg"},
        {"lab coat",      "labcoat.jpg"},
        {"hoodie",        "hoodie.jpg"},
        {"cap",           "cap.jpg"},
        {"scarf",         "scarf.jpg"},
        {"umbrella",      "umbrella.jpg"},
        {"wallet",        "wallet.jpg"},
        {"student id",    "idcard.jpg"},
        {"meal card",     "idcard.jpg"},
        {"borrower card", "idcard.jpg"},
        {"id card",       "idcard.jpg"},
        {"card",          "idcard.jpg"},
    };

    /**
     * Returns a real bundled photo for the item where one is available, copied to a temp file so
     * it can flow through the normal {@code ItemService.reportItem(...)} path. Falls back to a
     * generated category icon if no photo maps or the bundled resource is missing.
     */
    public static File generate(String category, String type, String name) {
        String photo = photoFor(category, name);
        if (photo != null) {
            File f = copyResourceToTemp(SEED_DIR + photo);
            if (f != null) {
                return f;
            }
        }
        return generateIcon(category, type, name);
    }

    /** Resolves the bundled photo filename for an item: keyword match first, then category. */
    private static String photoFor(String category, String name) {
        String n = name == null ? "" : name.toLowerCase();
        for (String[] kp : KEYWORD_PHOTOS) {
            if (n.contains(kp[0])) {
                return kp[1];
            }
        }
        return categoryPhoto(category);
    }

    private static String categoryPhoto(String category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case "Electronics"        -> "laptop.jpg";
            case "Accessories"        -> "watch.jpg";
            case "ID/Cards"           -> "idcard.jpg";
            case "Keys"               -> "keys.jpg";
            case "Bags"               -> "backpack.jpg";
            case "Books & Stationery" -> "textbook.jpg";
            case "Clothing"           -> "hoodie.jpg";
            case "Other"              -> "wallet.jpg";
            default                   -> null;
        };
    }

    /** Copies a bundled classpath resource to a temp file (deleted on exit), or null if absent. */
    private static File copyResourceToTemp(String resourcePath) {
        try (InputStream in = SeedImageFactory.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            Path tmp = Files.createTempFile("lfms-seed-", ".jpg");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            File file = tmp.toFile();
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    /** Renders a category icon image for {@code name} and returns the temp PNG file. */
    private static File generateIcon(String category, String type, String name) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color base = colorFor(category);
        Color top = base.brighter();
        g.setPaint(new GradientPaint(0, 0, lighten(base, 0.22f), 0, H, base));
        g.fillRect(0, 0, W, H);

        // Soft decorative circles
        g.setColor(new Color(255, 255, 255, 22));
        g.fill(new Ellipse2D.Double(W - 150, -90, 280, 280));
        g.fill(new Ellipse2D.Double(-120, H - 160, 260, 260));

        // Icon medallion
        int cx = W / 2;
        int cy = H / 2 - 24;
        g.setColor(new Color(255, 255, 255, 38));
        g.fill(new Ellipse2D.Double(cx - 92, cy - 92, 184, 184));

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawIcon(g, category, cx, cy);

        // Type tag (LOST / FOUND) top-left
        String tag = type == null ? "" : type.toUpperCase();
        if (!tag.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            int tw = g.getFontMetrics().stringWidth(tag) + 36;
            g.setColor(new Color(0, 0, 0, 55));
            g.fill(new RoundRectangle2D.Double(28, 28, tw, 44, 22, 22));
            g.setColor(Color.WHITE);
            g.drawString(tag, 46, 58);
        }

        // Footer caption with item name
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(0, H - 78, W, 78);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString(fit(g, name, W - 60), 32, H - 30);

        g.dispose();
        return writeTemp(img);
    }

    private static void drawIcon(Graphics2D g, String category, int cx, int cy) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("electronic")) {
            // Laptop
            g.drawRoundRect(cx - 60, cy - 42, 120, 78, 12, 12);
            g.drawLine(cx - 80, cy + 48, cx + 80, cy + 48);
            g.drawLine(cx - 60, cy + 36, cx - 80, cy + 48);
            g.drawLine(cx + 60, cy + 36, cx + 80, cy + 48);
        } else if (c.contains("book") || c.contains("station")) {
            // Open book
            g.drawLine(cx, cy - 50, cx, cy + 44);
            g.drawArc(cx - 78, cy - 50, 78, 96, 90, -180);
            g.drawArc(cx, cy - 50, 78, 96, 90, 180);
        } else if (c.contains("key")) {
            // Key
            g.drawOval(cx - 58, cy - 34, 60, 60);
            g.drawLine(cx, cy + 2, cx + 64, cy + 2);
            g.drawLine(cx + 44, cy + 2, cx + 44, cy + 24);
            g.drawLine(cx + 60, cy + 2, cx + 60, cy + 20);
        } else if (c.contains("cloth")) {
            // T-shirt
            g.drawPolyline(
                new int[]{cx - 58, cx - 28, cx - 18, cx + 18, cx + 28, cx + 58, cx + 40, cx + 40, cx - 40, cx - 40, cx - 58},
                new int[]{cy - 24, cy - 44, cy - 36, cy - 36, cy - 44, cy - 24, cy - 6, cy + 50, cy + 50, cy - 6, cy - 24}, 11);
        } else if (c.contains("id") || c.contains("card")) {
            // ID card
            g.drawRoundRect(cx - 70, cy - 44, 140, 88, 12, 12);
            g.fillOval(cx - 50, cy - 24, 34, 34);
            g.drawLine(cx + 2, cy - 18, cx + 54, cy - 18);
            g.drawLine(cx + 2, cy + 2, cx + 54, cy + 2);
            g.drawLine(cx - 50, cy + 24, cx + 54, cy + 24);
        } else if (c.contains("bag")) {
            // Backpack
            g.drawRoundRect(cx - 50, cy - 30, 100, 92, 26, 26);
            g.drawArc(cx - 30, cy - 58, 60, 60, 0, 180);
            g.drawLine(cx - 50, cy + 18, cx + 50, cy + 18);
        } else if (c.contains("access")) {
            // Sunglasses
            g.drawOval(cx - 66, cy - 16, 50, 44);
            g.drawOval(cx + 16, cy - 16, 50, 44);
            g.drawLine(cx - 16, cy + 2, cx + 16, cy + 2);
        } else {
            // Generic tag / box
            g.drawRoundRect(cx - 52, cy - 44, 104, 88, 16, 16);
            g.drawLine(cx - 52, cy - 6, cx + 52, cy - 6);
        }
    }

    private static Color colorFor(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("electronic"))            return new Color(0x1B6FB3);
        if (c.contains("cloth"))                 return new Color(0x12806A);
        if (c.contains("access"))                return new Color(0x7A4BB0);
        if (c.contains("book") || c.contains("station")) return new Color(0xC9701E);
        if (c.contains("key"))                   return new Color(0x9A6B12);
        if (c.contains("id") || c.contains("card")) return new Color(0x35506E);
        if (c.contains("bag"))                   return new Color(0x1F8A4C);
        return new Color(0x5A6B7A);
    }

    private static Color lighten(Color c, float amt) {
        int r = (int) Math.min(255, c.getRed() + 255 * amt);
        int g = (int) Math.min(255, c.getGreen() + 255 * amt);
        int b = (int) Math.min(255, c.getBlue() + 255 * amt);
        return new Color(r, g, b);
    }

    private static String fit(Graphics2D g, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (g.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (g.getFontMetrics().stringWidth(sb.toString() + ch + ellipsis) > maxWidth) {
                break;
            }
            sb.append(ch);
        }
        return sb + ellipsis;
    }

    private static File writeTemp(BufferedImage img) {
        try {
            Path tmp = Files.createTempFile("lfms-seed-", ".png");
            ImageIO.write(img, "png", tmp.toFile());
            File file = tmp.toFile();
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate seed image", e);
        }
    }
}
