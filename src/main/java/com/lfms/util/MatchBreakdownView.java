package com.lfms.util;

import com.lfms.model.MatchBreakdown;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Builds the green "Why this matched" panel from a {@link MatchBreakdown}, used everywhere a
 * match is shown (dashboard suggestion cards, the duplicate-detection warning) so the scoring
 * is always explained the same way instead of shown as a bare number.
 *
 * <pre>
 * Why this matched:
 * ✓ Same category: Electronics (+3)
 * ✓ Shared keywords: laptop, hp (+4)
 * ✓ Same location: Sam Jonah Library (+2)
 * </pre>
 */
public final class MatchBreakdownView {

    private MatchBreakdownView() {
    }

    public static VBox build(MatchBreakdown breakdown) {
        VBox box = new VBox(3.0);
        box.getStyleClass().add("breakdown-panel");

        Label title = new Label("Reasons:");
        title.getStyleClass().add("breakdown-title");
        box.getChildren().add(title);

        if (breakdown == null || breakdown.isEmpty()) {
            box.getChildren().add(reason("✓ Matched on overall similarity"));
        } else {
            for (MatchBreakdown.Reason reason : breakdown.getReasons()) {
                box.getChildren().add(reason("✓ " + reason.getLabel()));
            }
        }
        return box;
    }

    private static Label reason(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("breakdown-reason");
        label.setWrapText(true);
        return label;
    }
}
