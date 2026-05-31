package com.lfms.model;

/**
 * A suggested pairing between a LOST and a FOUND item produced by the matching engine.
 *
 * <p>{@code lostItemName} and {@code foundItemName} are joined display values set by the DAO.
 * {@code confidence} is derived from {@code score}: {@code >=8 HIGH}, {@code 5-7 MEDIUM},
 * {@code 4 LOW}.</p>
 */
public class Match {

    private int matchId;
    private int lostItemId;
    private int foundItemId;
    private int score;
    private String createdAt;

    // Joined / display fields
    private String lostItemName;
    private String foundItemName;

    public Match() {
    }

    public Match(int matchId, int lostItemId, int foundItemId, int score, String createdAt) {
        this.matchId = matchId;
        this.lostItemId = lostItemId;
        this.foundItemId = foundItemId;
        this.score = score;
        this.createdAt = createdAt;
    }

    /** Confidence label derived from the score. */
    public String getConfidence() {
        if (score >= 8) {
            return "HIGH";
        } else if (score >= 5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getLostItemId() {
        return lostItemId;
    }

    public void setLostItemId(int lostItemId) {
        this.lostItemId = lostItemId;
    }

    public int getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(int foundItemId) {
        this.foundItemId = foundItemId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLostItemName() {
        return lostItemName;
    }

    public void setLostItemName(String lostItemName) {
        this.lostItemName = lostItemName;
    }

    public String getFoundItemName() {
        return foundItemName;
    }

    public void setFoundItemName(String foundItemName) {
        this.foundItemName = foundItemName;
    }

    @Override
    public String toString() {
        return "Match{matchId=" + matchId + ", lostItemId=" + lostItemId + ", foundItemId=" + foundItemId
                + ", score=" + score + ", confidence='" + getConfidence() + '\'' + '}';
    }
}
