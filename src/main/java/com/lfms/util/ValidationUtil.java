package com.lfms.util;

import javafx.scene.control.Label;

import java.util.regex.Pattern;

/**
 * Form validation helpers and inline error-label management. Errors are shown beneath
 * fields via red {@code error-label} styled labels, not pop-up alerts.
 */
public class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isMinLength(String value, int min) {
        return value != null && value.trim().length() >= min;
    }

    /** Shows an error message in the label and makes it occupy layout space. */
    public static void showError(Label label, String message) {
        if (label == null) {
            return;
        }
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    /** Hides the label and frees its layout space. */
    public static void clearError(Label label) {
        if (label == null) {
            return;
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    public static void clearAllErrors(Label... labels) {
        for (Label label : labels) {
            clearError(label);
        }
    }
}
