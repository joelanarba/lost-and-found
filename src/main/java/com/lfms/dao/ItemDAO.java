package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link Item} rows.
 */
public class ItemDAO {

    /** Inserts a new item and returns the generated item_id (or -1 on failure). */
    public int create(Item item) {
        String sql = "INSERT INTO items "
                + "(user_id, type, name, category, description, location, image_path, date_reported, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getUserId());
            ps.setString(2, item.getType());
            ps.setString(3, item.getName());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getDescription());
            ps.setString(6, item.getLocation());
            ps.setString(7, item.getImagePath());
            ps.setString(8, item.getDateReported());
            ps.setString(9, item.getStatus() == null ? Item.STATUS_OPEN : item.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.create failed", e);
        }
    }

    public Item findById(int itemId) {
        String sql = "SELECT * FROM items WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.findById failed", e);
        }
    }

    public List<Item> findAll() {
        return query("SELECT * FROM items ORDER BY item_id DESC");
    }

    public List<Item> findByType(String type) {
        return querySingleParam("SELECT * FROM items WHERE type = ? ORDER BY item_id DESC", type);
    }

    public List<Item> findByUser(int userId) {
        String sql = "SELECT * FROM items WHERE user_id = ? ORDER BY item_id DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.findByUser failed", e);
        }
    }

    public List<Item> findOpenByType(String type) {
        String sql = "SELECT * FROM items WHERE type = ? AND status = ? ORDER BY item_id DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, Item.STATUS_OPEN);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.findOpenByType failed", e);
        }
    }

    public List<Item> search(String keyword, String type, String category, String location,
                             String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (notBlank(keyword)) {
            sql.append(" AND (name LIKE ? OR description LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (notBlank(type) && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" AND type = ?");
            params.add(type.trim().toUpperCase());
        }
        if (notBlank(category) && !"ALL".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        if (notBlank(location)) {
            sql.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }
        if (notBlank(dateFrom)) {
            sql.append(" AND date_reported >= ?");
            params.add(dateFrom.trim());
        }
        if (notBlank(dateTo)) {
            sql.append(" AND date_reported <= ?");
            params.add(dateTo.trim());
        }
        sql.append(" ORDER BY item_id DESC");

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.search failed", e);
        }
    }

    public boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE items SET status = ? WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.updateStatus failed", e);
        }
    }

    public boolean update(Item item) {
        String sql = "UPDATE items SET name = ?, category = ?, description = ?, location = ?, "
                + "image_path = ?, date_reported = ? WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getLocation());
            ps.setString(5, item.getImagePath());
            ps.setString(6, item.getDateReported());
            ps.setInt(7, item.getItemId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.update failed", e);
        }
    }

    public boolean delete(int itemId) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.delete failed", e);
        }
    }

    public int countByType(String type) {
        return count("SELECT COUNT(*) FROM items WHERE type = ?", type);
    }

    public int countByStatus(String status) {
        return count("SELECT COUNT(*) FROM items WHERE status = ?", status);
    }

    /** Items created within the last {@code months} months (newest first) — chart data. */
    public List<Item> findRecentByMonth(int months) {
        String sql = "SELECT * FROM items WHERE created_at >= datetime('now', ?) ORDER BY created_at DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "-" + months + " months");
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO.findRecentByMonth failed", e);
        }
    }

    // ---- helpers ----

    private List<Item> query(String sql) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO query failed: " + sql, e);
        }
    }

    private List<Item> querySingleParam(String sql, String param) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO query failed: " + sql, e);
        }
    }

    private int count(String sql, String param) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ItemDAO count failed: " + sql, e);
        }
    }

    private List<Item> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Item> items = new ArrayList<>();
            while (rs.next()) {
                items.add(map(rs));
            }
            return items;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private Item map(ResultSet rs) throws SQLException {
        return new Item(
                rs.getInt("item_id"),
                rs.getInt("user_id"),
                rs.getString("type"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("location"),
                rs.getString("image_path"),
                rs.getString("date_reported"),
                rs.getString("status"),
                rs.getString("created_at"));
    }
}
