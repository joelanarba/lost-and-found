package com.lfms.model;

/**
 * An in-app notification addressed to a single user. Rows live in the {@code notifications}
 * table and are raised when something relevant happens to that user: a match is found for
 * one of their items, a claim of theirs changes status, or an administrator acts on one of
 * their reports.
 */
public class Notification {

    private int notifId;
    private int userId;
    private String message;
    private boolean read;
    private String createdAt;

    public Notification() {
    }

    public Notification(int notifId, int userId, String message, boolean read, String createdAt) {
        this.notifId = notifId;
        this.userId = userId;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getNotifId() {
        return notifId;
    }

    public void setNotifId(int notifId) {
        this.notifId = notifId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
