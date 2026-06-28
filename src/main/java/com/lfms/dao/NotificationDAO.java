package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link Notification} rows. Writes are <em>best-effort</em> (a failure
 * is logged but never propagated) so raising a notification can never break the business action
 * that triggered it — the same contract the audit log uses.
 */
public class NotificationDAO {

    /** Inserts a notification for a user. Returns false (logged) on failure. */
    public boolean create(int userId, String message) {
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("[LFMS] Notification write failed: " + e.getMessage());
            return false;
        }
    }

    /** All notifications for a user, newest first. */
    public List<Notification> findByUser(int userId) {
        String sql = "SELECT notif_id, user_id, message, is_read, created_at "
                + "FROM notifications WHERE user_id = ? ORDER BY notif_id DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("NotificationDAO.findByUser failed", e);
        }
    }

    /** Count of unread notifications for a user. */
    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("NotificationDAO.countUnread failed", e);
        }
    }

    /** Marks every notification for a user as read. Returns the number updated. */
    public int markAllRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("NotificationDAO.markAllRead failed", e);
        }
    }

    private List<Notification> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Notification> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Notification(
                        rs.getInt("notif_id"),
                        rs.getInt("user_id"),
                        rs.getString("message"),
                        rs.getInt("is_read") != 0,
                        rs.getString("created_at")));
            }
            return list;
        }
    }
}
