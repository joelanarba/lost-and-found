package com.lfms.util;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Helpers for rendering coloured status/type/confidence badges, including a reusable
 * {@link TableColumn} cell factory so any table can show a string column as a pill badge.
 */
public class Badges {

    private Badges() {
    }

    /** Maps a domain value (type/status/confidence) to its badge CSS class. */
    public static String styleFor(String value) {
        if (value == null) {
            return "badge-neutral";
        }
        switch (value.trim().toUpperCase()) {
            case "LOST":          return "badge-lost";
            case "FOUND":         return "badge-found";
            case "OPEN":          return "badge-open";
            case "PENDING":
            case "CLAIM_PENDING": return "badge-pending";
            case "APPROVED":      return "badge-approved";
            case "REJECTED":      return "badge-rejected";
            case "RESOLVED":
            case "CLOSED":        return "badge-resolved";
            case "HIGH":          return "badge-high";
            case "MEDIUM":        return "badge-medium";
            case "LOW":           return "badge-low";
            default:              return "badge-neutral";
        }
    }

    /** Human-friendly label text (e.g. CLAIM_PENDING -&gt; "CLAIM PENDING"). */
    public static String prettify(String value) {
        return value == null ? "" : value.replace("_", " ");
    }

    public static Label label(String value) {
        Label label = new Label(prettify(value));
        label.getStyleClass().add(styleFor(value));
        return label;
    }

    /** Cell factory that renders the column's string value as a coloured badge. */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> badgeCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null : label(value));
            }
        };
    }
}
