package com.lfms.dao;

import com.lfms.database.DatabaseManager;
import com.lfms.model.Claim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@link Claim} rows. Read queries join {@code items} and
 * {@code users} (both the claimant and the item's reporter/finder) to populate the
 * display fields on {@link Claim}.
 */
public class ClaimDAO {

    private static final String BASE_SELECT =
            "SELECT c.claim_id, c.item_id, c.claimant_id, c.features_desc, c.proof_desc, c.proof_image, "
                    + "c.status, c.admin_note, c.created_at, "
                    + "i.name AS item_name, "
                    + "cl.name AS claimant_name, cl.index_no AS claimant_index, "
                    + "cl.email AS claimant_email, cl.phone AS claimant_phone, "
                    + "rp.name AS reporter_name, rp.email AS reporter_email, rp.phone AS reporter_phone "
                    + "FROM claims c "
                    + "JOIN items i  ON c.item_id = i.item_id "
                    + "JOIN users cl ON c.claimant_id = cl.user_id "
                    + "JOIN users rp ON i.user_id = rp.user_id ";

    public int create(Claim claim) {
        String sql = "INSERT INTO claims (item_id, claimant_id, features_desc, proof_desc, proof_image, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, claim.getItemId());
            ps.setInt(2, claim.getClaimantId());
            ps.setString(3, claim.getFeaturesDesc());
            ps.setString(4, claim.getProofDesc());
            ps.setString(5, claim.getProofImage());
            ps.setString(6, claim.getStatus() == null ? Claim.STATUS_PENDING : claim.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.create failed", e);
        }
    }

    public Claim findById(int claimId) {
        String sql = BASE_SELECT + "WHERE c.claim_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.findById failed", e);
        }
    }

    public List<Claim> findByItem(int itemId) {
        String sql = BASE_SELECT + "WHERE c.item_id = ? ORDER BY c.created_at DESC";
        return queryInt(sql, itemId, "findByItem");
    }

    public List<Claim> findPending() {
        String sql = BASE_SELECT + "WHERE c.status = 'PENDING' ORDER BY c.created_at DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.findPending failed", e);
        }
    }

    public List<Claim> findByClaimant(int claimantId) {
        String sql = BASE_SELECT + "WHERE c.claimant_id = ? ORDER BY c.created_at DESC";
        return queryInt(sql, claimantId, "findByClaimant");
    }

    public boolean existsActiveClaimByUser(int itemId, int userId) {
        String sql = "SELECT COUNT(*) FROM claims WHERE item_id = ? AND claimant_id = ? AND status = 'PENDING'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.existsActiveClaimByUser failed", e);
        }
    }

    public boolean updateStatus(int claimId, String status, String adminNote) {
        String sql = "UPDATE claims SET status = ?, admin_note = ? WHERE claim_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminNote);
            ps.setInt(3, claimId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.updateStatus failed", e);
        }
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM claims WHERE status = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.countByStatus failed", e);
        }
    }

    /** Returns the average time (in hours) between an item being reported and a claim for it being APPROVED. */
    public double averageClaimTimeHours() {
        String sql = "SELECT AVG((julianday(c.created_at) - julianday(i.created_at)) * 24.0) "
                   + "FROM claims c JOIN items i ON c.item_id = i.item_id "
                   + "WHERE c.status = 'APPROVED'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.averageClaimTimeHours failed", e);
        }
        return 0.0;
    }

    /** Deletes all claims belonging to an item (used when an item is removed). */
    public int deleteByItem(int itemId) {
        String sql = "DELETE FROM claims WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.deleteByItem failed", e);
        }
    }

    public int countSuccessfulReturns(int userId) {
        String sql = "SELECT count(*) FROM claims c JOIN items i ON c.item_id = i.item_id "
                   + "WHERE i.user_id = ? AND c.status = 'APPROVED'";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO.countSuccessfulReturns failed", e);
        }
        return 0;
    }

    // ---- helpers ----

    private List<Claim> queryInt(String sql, int param, String label) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            return collect(ps);
        } catch (SQLException e) {
            throw new RuntimeException("ClaimDAO." + label + " failed", e);
        }
    }

    private List<Claim> collect(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Claim> claims = new ArrayList<>();
            while (rs.next()) {
                claims.add(map(rs));
            }
            return claims;
        }
    }

    private Claim map(ResultSet rs) throws SQLException {
        Claim claim = new Claim(
                rs.getInt("claim_id"),
                rs.getInt("item_id"),
                rs.getInt("claimant_id"),
                rs.getString("features_desc"),
                rs.getString("proof_desc"),
                rs.getString("proof_image"),
                rs.getString("status"),
                rs.getString("admin_note"),
                rs.getString("created_at"));
        claim.setItemName(rs.getString("item_name"));
        claim.setClaimantName(rs.getString("claimant_name"));
        claim.setClaimantIndexNo(rs.getString("claimant_index"));
        claim.setClaimantEmail(rs.getString("claimant_email"));
        claim.setClaimantPhone(rs.getString("claimant_phone"));
        claim.setReporterName(rs.getString("reporter_name"));
        claim.setReporterEmail(rs.getString("reporter_email"));
        claim.setReporterPhone(rs.getString("reporter_phone"));
        return claim;
    }
}
