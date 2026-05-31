package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.Match;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link Match} rows. Read queries join {@code items} twice
 * to resolve the lost and found item names for display.
 */
public class MatchDAO {

    private static final String BASE_SELECT =
            "SELECT m.match_id, m.lost_item_id, m.found_item_id, m.score, m.created_at, "
                    + "li.name AS lost_name, fi.name AS found_name "
                    + "FROM matches m "
                    + "JOIN items li ON m.lost_item_id = li.item_id "
                    + "JOIN items fi ON m.found_item_id = fi.item_id ";

    public boolean create(Match match) {
        String sql = "INSERT INTO matches (lost_item_id, found_item_id, score) VALUES (?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, match.getLostItemId());
            ps.setInt(2, match.getFoundItemId());
            ps.setInt(3, match.getScore());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("MatchDAO.create failed", e);
        }
    }

    public List<Match> findByLostItem(int lostItemId) {
        String sql = BASE_SELECT + "WHERE m.lost_item_id = ? ORDER BY m.score DESC";
        return queryInt(sql, lostItemId, "findByLostItem");
    }

    public List<Match> findByFoundItem(int foundItemId) {
        String sql = BASE_SELECT + "WHERE m.found_item_id = ? ORDER BY m.score DESC";
        return queryInt(sql, foundItemId, "findByFoundItem");
    }

    /** Matches involving any item that belongs to the given user (as loser or finder). */
    public List<Match> findByUser(int userId) {
        String sql = BASE_SELECT + "WHERE li.user_id = ? OR fi.user_id = ? ORDER BY m.score DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("MatchDAO.findByUser failed", e);
        }
    }

    public boolean matchExists(int lostItemId, int foundItemId) {
        String sql = "SELECT COUNT(*) FROM matches WHERE lost_item_id = ? AND found_item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lostItemId);
            ps.setInt(2, foundItemId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("MatchDAO.matchExists failed", e);
        }
    }

    /** Deletes all matches referencing an item on either side (used when an item is removed). */
    public int deleteByItem(int itemId) {
        String sql = "DELETE FROM matches WHERE lost_item_id = ? OR found_item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, itemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("MatchDAO.deleteByItem failed", e);
        }
    }

    private List<Match> queryInt(String sql, int param, String label) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("MatchDAO." + label + " failed", e);
        }
    }

    private List<Match> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Match> matches = new ArrayList<>();
            while (rs.next()) {
                matches.add(map(rs));
            }
            return matches;
        }
    }

    private Match map(ResultSet rs) throws SQLException {
        Match match = new Match(
                rs.getInt("match_id"),
                rs.getInt("lost_item_id"),
                rs.getInt("found_item_id"),
                rs.getInt("score"),
                rs.getString("created_at"));
        match.setLostItemName(rs.getString("lost_name"));
        match.setFoundItemName(rs.getString("found_name"));
        return match;
    }
}
