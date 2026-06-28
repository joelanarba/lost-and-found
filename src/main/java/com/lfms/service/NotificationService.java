package com.lfms.service;

import com.lfms.dao.NotificationDAO;
import com.lfms.model.Notification;

import java.util.List;

/**
 * Thin business layer over {@link NotificationDAO}. Raising a notification is best-effort and
 * never throws, so callers can sprinkle {@link #notify(int, String)} into business flows without
 * defensive handling. Reads (list / count / mark-read) are used by the notification bell.
 */
public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final com.lfms.dao.UserDAO userDAO = new com.lfms.dao.UserDAO();

    /** Raises a notification for a user. Skips invalid users and swallows failures. */
    public void notify(int userId, String message) {
        if (userId <= 0 || message == null || message.isBlank()) {
            return;
        }
        notificationDAO.create(userId, message);
        
        com.lfms.model.User user = userDAO.findById(userId);
        if (user != null) {
            EmailService.sendEmail(user.getEmail(), "LFMS Notification", message);
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                SmsService.sendSms(user.getPhone(), message);
            }
        }
    }

    public List<Notification> forUser(int userId) {
        return notificationDAO.findByUser(userId);
    }

    public int unreadCount(int userId) {
        return notificationDAO.countUnread(userId);
    }

    public void markAllRead(int userId) {
        notificationDAO.markAllRead(userId);
    }
}
