package com.lfms.model;

/**
 * An immutable record of an action performed in the system, used for the admin audit trail.
 * {@code actorName} is a joined display value set by the DAO.
 */
public class AuditLog {

    private int logId;
    private int actorId;
    private String action;
    private String targetType;
    private int targetId;
    private String note;
    private String timestamp;

    // Joined / display field
    private String actorName;

    public AuditLog() {
    }

    public AuditLog(int logId, int actorId, String action, String targetType, int targetId,
                    String note, String timestamp) {
        this.logId = logId;
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.note = note;
        this.timestamp = timestamp;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getActorId() {
        return actorId;
    }

    public void setActorId(int actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    @Override
    public String toString() {
        return "AuditLog{logId=" + logId + ", actorId=" + actorId + ", action='" + action + '\''
                + ", targetType='" + targetType + '\'' + ", targetId=" + targetId + '}';
    }
}
