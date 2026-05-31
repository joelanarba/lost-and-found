package com.lfms.service;

import com.lfms.dao.ItemDAO;
import com.lfms.dao.MatchDAO;
import com.lfms.model.Item;
import com.lfms.model.Match;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic matching engine that suggests likely LOST&#8596;FOUND pairings.
 *
 * <p>Scoring for each opposite-type OPEN candidate:</p>
 * <ul>
 *   <li>+3 exact category match</li>
 *   <li>+2 per shared keyword in the item NAME (stopwords removed)</li>
 *   <li>+1 per shared keyword in the DESCRIPTION</li>
 *   <li>+2 if the locations share at least one non-stopword token</li>
 *   <li>+1 if the reported dates are within 7 days of each other</li>
 * </ul>
 * Only scores &ge; 4 are kept. Confidence: &ge;8 HIGH, 5&ndash;7 MEDIUM, 4 LOW.
 */
public class MatchingEngine {

    private static final int THRESHOLD = 4;
    private static final int DATE_WINDOW_DAYS = 7;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "it", "in", "on", "at", "of", "to", "and", "or",
            "my", "i", "was", "were", "has", "have", "been", "this", "that", "with", "for");

    private final ItemDAO itemDAO = new ItemDAO();
    private final MatchDAO matchDAO = new MatchDAO();

    /**
     * Finds and persists matches for a newly reported item. Returns all qualifying
     * matches (newly created or already existing) sorted by descending score.
     */
    public List<Match> findMatches(Item newItem) {
        String oppositeType = newItem.isLost() ? Item.TYPE_FOUND : Item.TYPE_LOST;
        List<Item> candidates = itemDAO.findOpenByType(oppositeType);

        Set<String> newName = tokenize(newItem.getName());
        Set<String> newDesc = tokenize(newItem.getDescription());
        Set<String> newLoc = tokenize(newItem.getLocation());

        List<Match> results = new ArrayList<>();

        for (Item candidate : candidates) {
            if (candidate.getItemId() == newItem.getItemId()) {
                continue;
            }
            int score = 0;

            if (newItem.getCategory() != null
                    && newItem.getCategory().equalsIgnoreCase(candidate.getCategory())) {
                score += 3;
            }
            score += 2 * countSharedTokens(newName, tokenize(candidate.getName()));
            score += countSharedTokens(newDesc, tokenize(candidate.getDescription()));
            if (countSharedTokens(newLoc, tokenize(candidate.getLocation())) > 0) {
                score += 2;
            }
            if (withinDays(newItem.getDateReported(), candidate.getDateReported(), DATE_WINDOW_DAYS)) {
                score += 1;
            }

            if (score >= THRESHOLD) {
                int lostId = newItem.isLost() ? newItem.getItemId() : candidate.getItemId();
                int foundId = newItem.isFound() ? newItem.getItemId() : candidate.getItemId();

                if (!matchDAO.matchExists(lostId, foundId)) {
                    Match toPersist = new Match();
                    toPersist.setLostItemId(lostId);
                    toPersist.setFoundItemId(foundId);
                    toPersist.setScore(score);
                    matchDAO.create(toPersist);
                }

                Match result = new Match(0, lostId, foundId, score, null);
                result.setLostItemName(newItem.isLost() ? newItem.getName() : candidate.getName());
                result.setFoundItemName(newItem.isFound() ? newItem.getName() : candidate.getName());
                results.add(result);
            }
        }

        results.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return results;
    }

    /** Splits text into a set of lower-case, non-stopword tokens (length &ge; 2). */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) {
            return tokens;
        }
        for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
            if (token.length() >= 2 && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private int countSharedTokens(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int shared = 0;
        for (String token : a) {
            if (b.contains(token)) {
                shared++;
            }
        }
        return shared;
    }

    private boolean withinDays(String date1, String date2, int days) {
        LocalDate d1 = parseDate(date1);
        LocalDate d2 = parseDate(date2);
        if (d1 == null || d2 == null) {
            return false;
        }
        return Math.abs(ChronoUnit.DAYS.between(d1, d2)) <= days;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // Accept both "yyyy-MM-dd" and "yyyy-MM-dd HH:mm:ss".
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
