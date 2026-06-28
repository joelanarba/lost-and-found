package com.lfms.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Generates and locates QR-code PNGs for found items. Each found item gets a QR encoding the
 * text {@code LFMS-ITEM-<id>}, written to {@link AppPaths#QRCODES_DIR}. Generation is
 * best-effort — a failure is logged and returns {@code null} so it can never break reporting.
 */
public final class QrCodeUtil {

    private static final int SIZE = 320;

    private QrCodeUtil() {
    }

    /** The on-disk location of an item's QR PNG (whether or not it exists yet). */
    public static Path qrPathForItem(int itemId) {
        return AppPaths.QRCODES_DIR.resolve("item-" + itemId + ".png");
    }

    /** The text encoded in the QR code for an item. */
    public static String payloadForItem(int itemId) {
        return "LFMS-ITEM-" + itemId;
    }

    /** True once a QR PNG has been generated for the item. */
    public static boolean exists(int itemId) {
        return Files.exists(qrPathForItem(itemId));
    }

    /**
     * Generates (overwriting) the QR PNG for an item and returns its path, or {@code null} on
     * failure. Called when a found item is reported.
     */
    public static Path generateForItem(int itemId) {
        Path out = qrPathForItem(itemId);
        try {
            AppPaths.ensureDirectories();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter()
                    .encode(payloadForItem(itemId), BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", out);
            return out;
        } catch (Exception e) {
            System.err.println("[LFMS] QR generation failed for item " + itemId + ": " + e.getMessage());
            return null;
        }
    }

    /** Returns the QR path, generating it first if it does not already exist. May be {@code null}. */
    public static Path ensureForItem(int itemId) {
        return exists(itemId) ? qrPathForItem(itemId) : generateForItem(itemId);
    }

    public static Path qrPathForClaim(int claimId) {
        return AppPaths.QRCODES_DIR.resolve("claim-" + claimId + ".png");
    }

    public static String payloadForClaim(int claimId) {
        return "LFMS-CLAIM-" + claimId;
    }

    public static Path generateForClaim(int claimId) {
        Path out = qrPathForClaim(claimId);
        try {
            AppPaths.ensureDirectories();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter()
                    .encode(payloadForClaim(claimId), BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
            MatrixToImageWriter.writeToPath(matrix, "PNG", out);
            return out;
        } catch (Exception e) {
            System.err.println("[LFMS] QR generation failed for claim " + claimId + ": " + e.getMessage());
            return null;
        }
    }

    public static Path ensureForClaim(int claimId) {
        return Files.exists(qrPathForClaim(claimId)) ? qrPathForClaim(claimId) : generateForClaim(claimId);
    }
}
