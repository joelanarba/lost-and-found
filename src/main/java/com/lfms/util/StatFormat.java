package com.lfms.util;

import com.lfms.model.LabelCount;

/**
 * Shared formatting for the dashboard insight numbers so the student and admin dashboards
 * present them identically.
 */
public final class StatFormat {

    private StatFormat() {
    }

    /** Recovery rate to one decimal place, e.g. {@code "82.5%"}. */
    public static String recoveryRate(double rate) {
        return String.format("%.1f%%", rate);
    }

    /**
     * "Most lost category" line, e.g. {@code "Electronics · 34%"}, where the percentage is the
     * top category's share of all lost reports. Returns {@code "—"} when nothing is lost yet.
     */
    public static String mostLostCategory(LabelCount count, int totalLost) {
        if (count == null || count.count() == 0 || totalLost == 0) {
            return "N/A";
        }
        int pct = (int) Math.round((count.count() * 100.0) / totalLost);
        return count.label() + " (" + pct + "%)";
    }

    public static String averageClaimTime(double hours) {
        if (hours <= 0) return "N/A";
        if (hours < 24) return String.format("%.1f hrs", hours);
        double days = hours / 24.0;
        return String.format("%.1f days", days);
    }
}
