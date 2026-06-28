package com.lfms.util;

import com.lfms.model.Notification;
import com.lfms.model.User;
import com.lfms.service.NotificationService;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.net.URL;
import java.util.List;

/**
 * Wires up the notification bell shown in every authenticated sidebar header. Given a bell
 * {@link Button} and its red count {@link Label}, it loads the current user's unread count,
 * and on click opens a dropdown listing their notifications (newest first) with timestamps,
 * marking them all read. Shared by the student and admin sidebars so the behaviour is identical.
 */
public final class NotificationBell {

    private static final int MAX_SHOWN = 40;
    private static final NotificationService SERVICE = new NotificationService();

    private NotificationBell() {
    }

    /** Installs the bell behaviour. Hides the bell entirely when there is no logged-in user. */
    public static void install(Button bell, Label badge) {
        if (bell == null) {
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            setShown(bell, false);
            setShown(badge, false);
            return;
        }
        int userId = user.getUserId();
        updateBadge(badge, SERVICE.unreadCount(userId));
        bell.setOnAction(e -> openPopup(bell, badge, userId));
    }

    private static void openPopup(Button bell, Label badge, int userId) {
        List<Notification> notes = SERVICE.forUser(userId);

        VBox card = new VBox(0);
        card.getStyleClass().add("notif-popup");
        applyStylesheet(card);

        Label title = new Label("Notifications");
        title.getStyleClass().add("notif-popup-title");

        VBox list = new VBox(0);
        if (notes.isEmpty()) {
            Label empty = new Label("You have no notifications yet.");
            empty.getStyleClass().add("notif-empty");
            list.getChildren().add(empty);
        } else {
            int shown = 0;
            for (Notification n : notes) {
                if (shown++ >= MAX_SHOWN) {
                    break;
                }
                list.getChildren().add(buildRow(n));
            }
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(380);
        scroll.getStyleClass().add("notif-scroll");

        card.getChildren().addAll(title, new Separator(), scroll);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(card);

        // Opening the bell marks everything read and clears the badge.
        SERVICE.markAllRead(userId);
        updateBadge(badge, 0);

        Bounds b = bell.localToScreen(bell.getBoundsInLocal());
        if (b != null) {
            popup.show(bell, b.getMinX(), b.getMaxY() + 6);
        }
    }

    private static VBox buildRow(Notification n) {
        VBox row = new VBox(3);
        row.getStyleClass().add("notif-row");
        if (!n.isRead()) {
            row.getStyleClass().add("notif-unread");
        }

        Label msg = new Label(n.getMessage());
        msg.setWrapText(true);
        msg.setMaxWidth(300);
        msg.getStyleClass().add("notif-msg");

        Label time = new Label(formatTime(n.getCreatedAt()));
        time.getStyleClass().add("notif-time");

        row.getChildren().addAll(msg, time);
        return row;
    }

    private static void updateBadge(Label badge, int count) {
        if (badge == null) {
            return;
        }
        if (count > 0) {
            badge.setText(count > 9 ? "9+" : String.valueOf(count));
            setShown(badge, true);
        } else {
            setShown(badge, false);
        }
    }

    /** Trims the SQLite {@code datetime('now')} timestamp to {@code yyyy-MM-dd HH:mm}. */
    private static String formatTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() >= 16 ? trimmed.substring(0, 16) : trimmed;
    }

    private static void applyStylesheet(VBox card) {
        URL css = NotificationBell.class.getResource("/com/lfms/css/main.css");
        if (css != null) {
            card.getStylesheets().add(css.toExternalForm());
        }
    }

    private static void setShown(javafx.scene.Node node, boolean shown) {
        if (node != null) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }
}
