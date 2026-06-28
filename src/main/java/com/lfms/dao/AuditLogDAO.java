package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the audit trail. Writes are <em>best-effort</em>: a logging
 * failure is reported to the console but never propagated, so it cannot break the
 * business action that triggered it.
 */
public class AuditLogDAO {

    private static final String BASE_SELECT =
            "SELECT a.log_id, a.actor_id, a.action, a.target_type, a.target_id, a.note, a.timestamp, "
                    + "u.name AS actor_name "
                    + "FROM audit_log a LEFT JOIN users u ON a.actor_id = u.user_id ";

    public void log(int actorId, String action, String targetType, int targetId, String note) {
        String sql = "INSERT INTO audit_log (actor_id, action, target_type, target_id, note) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (actorId > 0) {
                ps.setInt(1, actorId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, action);
            ps.setString(3, targetType);
            if (targetId > 0) {
                ps.setInt(4, targetId);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, note);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Best-effort: never let an audit failure break the calling action.
            System.err.println("[LFMS] Audit log write failed: " + e.getMessage());
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = BASE_SELECT + "ORDER BY a.log_id DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findRecent failed", e);
        }
    }

    public List<AuditLog> findAll() {
        String sql = BASE_SELECT + "ORDER BY a.log_id DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findAll failed", e);
        }
    }

    /** Audit entries against a specific target (e.g. a CLAIM or ITEM), oldest first. */
    public List<AuditLog> findByTarget(String targetType, int targetId) {
        String sql = BASE_SELECT + "WHERE a.target_type = ? AND a.target_id = ? ORDER BY a.log_id ASC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, targetType);
            ps.setInt(2, targetId);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDAO.findByTarget failed", e);
        }
    }

    private List<AuditLog> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<AuditLog> logs = new ArrayList<>();
            while (rs.next()) {
                AuditLog log = new AuditLog(
                        rs.getInt("log_id"),
                        rs.getInt("actor_id"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getInt("target_id"),
                        rs.getString("note"),
                        rs.getString("timestamp"));
                String actorName = rs.getString("actor_name");
                log.setActorName(actorName != null ? actorName : "System");
                logs.add(log);
            }
            return logs;
        }
    }
}
