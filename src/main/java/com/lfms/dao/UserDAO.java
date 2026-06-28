package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link User} rows. All access uses {@link PreparedStatement}
 * and try-with-resources.
 */
public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findByEmail failed", e);
        }
    }

    public User findByEmailOrIndex(String identifier) {
        String sql = "SELECT * FROM users WHERE email = ? OR index_no = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findByEmailOrIndex failed", e);
        }
    }

    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findById failed", e);
        }
    }

    public boolean create(User user) {
        String sql = "INSERT INTO users (name, index_no, email, password_hash, phone, role, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getIndexNo());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole() == null ? "STUDENT" : user.getRole());
            ps.setInt(7, user.isActive() ? 1 : 0);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.create failed", e);
        }
    }

    public boolean emailExists(String email) {
        return countWhere("email", email) > 0;
    }

    public boolean indexNoExists(String indexNo) {
        return countWhere("index_no", indexNo) > 0;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY name COLLATE NOCASE";
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAll failed", e);
        }
    }

    public boolean updateActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.updateActive failed", e);
        }
    }

    public boolean delete(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.delete failed", e);
        }
    }

    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.countByRole failed", e);
        }
    }

    private int countWhere(String column, String value) {
        String sql = "SELECT COUNT(*) FROM users WHERE " + column + " = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO count on " + column + " failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("index_no"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("phone"),
                rs.getString("role"),
                rs.getInt("is_active") == 1,
                rs.getString("created_at"));
    }
}
