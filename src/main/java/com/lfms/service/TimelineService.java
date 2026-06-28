package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.ClaimDAO;
import com.lfms.dao.ItemDAO;
import com.lfms.dao.MatchDAO;
import com.lfms.model.AuditLog;
import com.lfms.model.Claim;
import com.lfms.model.Item;
import com.lfms.model.Match;
import com.lfms.model.TimelineEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Assembles the chronological history of an item from the existing tables — items, matches,
 * claims and the audit log. Only events that actually occurred are returned, in the natural
 * lifecycle order: reported &rarr; matched &rarr; claimed &rarr; claim approved &rarr; resolved.
 */
public class TimelineService {

    private final ItemDAO itemDAO = new ItemDAO();
    private final MatchDAO matchDAO = new MatchDAO();
    private final ClaimDAO claimDAO = new ClaimDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();

    /** Builds the ordered list of timeline events for an item (empty if the item is gone). */
    public List<TimelineEvent> build(int itemId) {
        List<TimelineEvent> events = new ArrayList<>();
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            return events;
        }

        // 🔵 Reported — always present.
        events.add(new TimelineEvent("🔵", "Item Reported",
                date(firstNonBlank(item.getDateReported(), item.getCreatedAt()))));

        // 🟡 Match found — earliest match the item participates in.
        List<Match> matches = item.isLost()
                ? matchDAO.findByLostItem(itemId)
                : matchDAO.findByFoundItem(itemId);
        String firstMatch = earliest(matches, Match::getCreatedAt);
        if (firstMatch != null) {
            events.add(new TimelineEvent("🟡", "Match Found", date(firstMatch)));
        }

        // 🟠 Claim submitted — earliest claim on the item.
        List<Claim> claims = claimDAO.findByItem(itemId);
        String firstClaim = earliest(claims, Claim::getCreatedAt);
        if (firstClaim != null) {
            events.add(new TimelineEvent("🟠", "Claim Submitted", date(firstClaim)));
        }

        // 🟢 Claim approved — timestamp from the audit entry on the approved claim.
        for (Claim claim : claims) {
            if (Claim.STATUS_APPROVED.equals(claim.getStatus())) {
                String approvedAt = auditTimestamp("CLAIM", claim.getClaimId(), "APPROVE_CLAIM", null);
                events.add(new TimelineEvent("🟢", "Claim Approved",
                        date(firstNonBlank(approvedAt, claim.getCreatedAt()))));
                break;
            }
        }

        // ✅ Item resolved — timestamp from the status-change audit entry.
        if (Item.STATUS_RESOLVED.equalsIgnoreCase(item.getStatus())) {
            String resolvedAt = auditTimestamp("ITEM", itemId, "CHANGE_STATUS", "RESOLVED");
            events.add(new TimelineEvent("✅", "Item Resolved",
                    date(firstNonBlank(resolvedAt, item.getCreatedAt()))));
        }

        return events;
    }

    /**
     * Timestamp of the first audit entry against a target matching {@code action} (and, if given,
     * whose note contains {@code noteContains}). Returns {@code null} when none is found.
     */
    private String auditTimestamp(String targetType, int targetId, String action, String noteContains) {
        for (AuditLog log : auditDAO.findByTarget(targetType, targetId)) {
            if (!action.equals(log.getAction())) {
                continue;
            }
            if (noteContains != null && (log.getNote() == null
                    || !log.getNote().toUpperCase().contains(noteContains.toUpperCase()))) {
                continue;
            }
            return log.getTimestamp();
        }
        return null;
    }

    /** The lexicographically-smallest (earliest, ISO-formatted) timestamp in a list, or null. */
    private <T> String earliest(List<T> list, Function<T, String> getter) {
        String min = null;
        for (T element : list) {
            String value = getter.apply(element);
            if (value == null || value.isBlank()) {
                continue;
            }
            if (min == null || value.compareTo(min) < 0) {
                min = value;
            }
        }
        return min;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    /** Trims an ISO timestamp to just the date portion ({@code yyyy-MM-dd}). */
    private String date(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
    }
}
