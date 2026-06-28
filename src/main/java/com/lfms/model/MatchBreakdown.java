package com.lfms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A structured, human-readable explanation of <em>why</em> two items were matched by the
 * {@link com.lfms.service.MatchingEngine}. Produced once when the engine scores a pair and
 * carried on the {@link Match} so the UI can render the breakdown without recomputing.
 *
 * <p>Each {@link Reason} is one scoring rule that fired (e.g. "Same category", "+3"). The sum
 * of every reason's points always equals {@link #getScore()}, and the confidence label is
 * derived from that score exactly as {@link Match#getConfidence()} does.</p>
 */
public class MatchBreakdown {

    /** A single scoring rule that contributed to the match, with its point value. */
    public static final class Reason {
        private final String text;
        private final int points;

        public Reason(String text, int points) {
            this.text = text;
            this.points = points;
        }

        public String getText() {
            return text;
        }

        public int getPoints() {
            return points;
        }

        /** Display form, e.g. {@code "Same category (+3)"}. */
        public String getLabel() {
            return text + " (+" + points + ")";
        }
    }

    private final List<Reason> reasons = new ArrayList<>();

    /** Adds a reason worth {@code points}; ignored when {@code points <= 0}. */
    public void add(String text, int points) {
        if (points > 0 && text != null && !text.isBlank()) {
            reasons.add(new Reason(text, points));
        }
    }

    public List<Reason> getReasons() {
        return reasons;
    }

    /** Total score — the sum of all reason points. */
    public int getScore() {
        int total = 0;
        for (Reason r : reasons) {
            total += r.getPoints();
        }
        return total;
    }

    /** Confidence label derived from the score: {@code >=8 HIGH}, {@code 5-7 MEDIUM}, else {@code LOW}. */
    public String getConfidence() {
        int score = getScore();
        if (score >= 8) {
            return "HIGH";
        } else if (score >= 5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public boolean isEmpty() {
        return reasons.isEmpty();
    }
}
