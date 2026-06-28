package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.ClaimDAO;
import com.lfms.dao.ItemDAO;
import com.lfms.model.Claim;
import com.lfms.model.Item;

import java.util.List;

/**
 * Claim lifecycle business logic: submission, approval and rejection, keeping the
 * related item's status in sync.
 */
public class ClaimService {

    private final ClaimDAO claimDAO = new ClaimDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Submits a claim: persists it, moves the item to CLAIM_PENDING and logs the action.
     */
    public boolean submitClaim(Claim claim) {
        int id = claimDAO.create(claim);
        if (id < 0) {
            return false;
        }
        itemDAO.updateStatus(claim.getItemId(), Item.STATUS_CLAIM_PENDING);
        auditDAO.log(claim.getClaimantId(), "SUBMIT_CLAIM", "CLAIM", id,
                "Claim submitted for item " + claim.getItemId());
        return true;
    }

    /**
     * Approves a claim: marks it APPROVED, sets the item to APPROVED, rejects any other
     * pending claims on the same item, and logs the action.
     */
    public boolean approveClaim(int claimId, int adminId) {
        Claim claim = claimDAO.findById(claimId);
        if (claim == null) {
            return false;
        }
        boolean ok = claimDAO.updateStatus(claimId, Claim.STATUS_APPROVED, "Claim approved by administrator.");
        if (ok) {
            itemDAO.updateStatus(claim.getItemId(), Item.STATUS_APPROVED);
            rejectOtherPendingClaims(claim.getItemId(), claimId, adminId);
            auditDAO.log(adminId, "APPROVE_CLAIM", "CLAIM", claimId,
                    "Claim approved for item " + claim.getItemId());
            notificationService.notify(claim.getClaimantId(),
                    "Your claim on \"" + claim.getItemName()
                            + "\" was approved. Contact the finder to arrange collection.");
        }
        return ok;
    }

    /**
     * Rejects a claim with a reason. Reverts the item to OPEN only if no other pending
     * claims remain on it. Logs the action.
     */
    public boolean rejectClaim(int claimId, String reason, int adminId) {
        Claim claim = claimDAO.findById(claimId);
        if (claim == null) {
            return false;
        }
        boolean ok = claimDAO.updateStatus(claimId, Claim.STATUS_REJECTED, reason);
        if (ok) {
            if (!hasOtherPendingClaims(claim.getItemId(), claimId)) {
                itemDAO.updateStatus(claim.getItemId(), Item.STATUS_OPEN);
            }
            auditDAO.log(adminId, "REJECT_CLAIM", "CLAIM", claimId,
                    "Claim rejected for item " + claim.getItemId() + ": " + reason);
            notificationService.notify(claim.getClaimantId(),
                    "Your claim on \"" + claim.getItemName() + "\" was rejected — reason: " + reason + ".");
        }
        return ok;
    }

    // ---- read operations (used by controllers) ----

    public List<Claim> findPending() {
        return claimDAO.findPending();
    }

    public List<Claim> findByClaimant(int userId) {
        return claimDAO.findByClaimant(userId);
    }

    public List<Claim> findByItem(int itemId) {
        return claimDAO.findByItem(itemId);
    }

    public Claim findById(int claimId) {
        return claimDAO.findById(claimId);
    }

    public int countByStatus(String status) {
        return claimDAO.countByStatus(status);
    }

    public double averageClaimTimeHours() {
        return claimDAO.averageClaimTimeHours();
    }

    public int countSuccessfulReturns(int userId) {
        return claimDAO.countSuccessfulReturns(userId);
    }

    public boolean hasPendingClaim(int itemId, int userId) {
        return claimDAO.existsActiveClaimByUser(itemId, userId);
    }

    public int countPendingByClaimant(int userId) {
        int count = 0;
        for (Claim claim : claimDAO.findByClaimant(userId)) {
            if (Claim.STATUS_PENDING.equals(claim.getStatus())) {
                count++;
            }
        }
        return count;
    }

    // ---- helpers ----

    private void rejectOtherPendingClaims(int itemId, int approvedClaimId, int adminId) {
        for (Claim other : claimDAO.findByItem(itemId)) {
            if (other.getClaimId() != approvedClaimId && Claim.STATUS_PENDING.equals(other.getStatus())) {
                claimDAO.updateStatus(other.getClaimId(), Claim.STATUS_REJECTED,
                        "Another claim was approved for this item.");
                auditDAO.log(adminId, "REJECT_CLAIM", "CLAIM", other.getClaimId(),
                        "Auto-rejected: another claim approved for item " + itemId);
                notificationService.notify(other.getClaimantId(),
                        "Your claim on \"" + other.getItemName()
                                + "\" was not approved — another claim was approved for this item.");
            }
        }
    }

    private boolean hasOtherPendingClaims(int itemId, int excludingClaimId) {
        List<Claim> claims = claimDAO.findByItem(itemId);
        for (Claim claim : claims) {
            if (claim.getClaimId() != excludingClaimId && Claim.STATUS_PENDING.equals(claim.getStatus())) {
                return true;
            }
        }
        return false;
    }
}
