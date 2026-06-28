package com.lfms.service;

import com.lfms.dao.ItemDAO;
import com.lfms.dao.MatchDAO;
import com.lfms.model.Item;
import com.lfms.model.Match;
import com.lfms.model.MatchBreakdown;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 *
 * <p>Every score is produced as a structured {@link MatchBreakdown} so the UI can show
 * <em>why</em> a pair matched without recomputing. {@link #explain(Item, Item)} regenerates
 * the breakdown for an already-persisted match; {@link #preview(Item)} scores a not-yet-saved
 * draft against the system without writing anything (used for real-time duplicate detection).</p>
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
     * matches (newly created or already existing) sorted by descending score, each carrying
     * its {@link MatchBreakdown}.
     */
    public List<Match> findMatches(Item newItem) {
        List<Match> results = scanCandidates(newItem, true);
        results.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * Scores a not-yet-persisted draft item against the existing opposite-type OPEN items
     * <em>without writing any match rows</em>. Used by the report forms to warn about a likely
     * existing match before the user submits. Results are sorted by descending score.
     */
    public List<Match> preview(Item draft) {
        List<Match> results = scanCandidates(draft, false);
        results.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return results;
    }

    /**
     * Regenerates the structured breakdown for a known lost/found pair. The scoring is
     * symmetric, so argument order does not matter. Returns an empty breakdown if either
     * item is missing.
     */
    public MatchBreakdown explain(Item lost, Item found) {
        if (lost == null || found == null) {
            return new MatchBreakdown();
        }
        return computeBreakdown(lost, found);
    }

    /** Shared scan used by both {@link #findMatches} and {@link #preview}. */
    private List<Match> scanCandidates(Item newItem, boolean persist) {
        String oppositeType = newItem.isLost() ? Item.TYPE_FOUND : Item.TYPE_LOST;
        List<Item> candidates = itemDAO.findOpenByType(oppositeType);

        List<Match> results = new ArrayList<>();
        for (Item candidate : candidates) {
            if (candidate.getItemId() == newItem.getItemId() && newItem.getItemId() != 0) {
                continue;
            }

            MatchBreakdown breakdown = computeBreakdown(newItem, candidate);
            if (breakdown.getScore() < THRESHOLD) {
                continue;
            }

            int lostId = newItem.isLost() ? newItem.getItemId() : candidate.getItemId();
            int foundId = newItem.isFound() ? newItem.getItemId() : candidate.getItemId();

            if (persist && !matchDAO.matchExists(lostId, foundId)) {
                Match toPersist = new Match();
                toPersist.setLostItemId(lostId);
                toPersist.setFoundItemId(foundId);
                toPersist.setScore(breakdown.getScore());
                matchDAO.create(toPersist);
            }

            Match result = new Match(0, lostId, foundId, breakdown.getScore(), null);
            result.setLostItemName(newItem.isLost() ? newItem.getName() : candidate.getName());
            result.setFoundItemName(newItem.isFound() ? newItem.getName() : candidate.getName());
            result.setBreakdown(breakdown);
            results.add(result);
        }
        return results;
    }

    /** Produces the structured, point-by-point explanation for a pair of items. */
    private MatchBreakdown computeBreakdown(Item a, Item b) {
        MatchBreakdown breakdown = new MatchBreakdown();

        if (a.getCategory() != null && a.getCategory().equalsIgnoreCase(b.getCategory())) {
            breakdown.add("Same category: " + a.getCategory(), 3);
        }

        Set<String> nameShared = sharedTokens(tokenize(a.getName()), tokenize(b.getName()));
        Set<String> descShared = sharedTokens(tokenize(a.getDescription()), tokenize(b.getDescription()));
        int keywordPoints = 2 * nameShared.size() + descShared.size();
        if (keywordPoints > 0) {
            Set<String> allWords = new LinkedHashSet<>(nameShared);
            allWords.addAll(descShared);
            breakdown.add("Shared keywords: " + String.join(", ", allWords), keywordPoints);
        }

        if (!sharedTokens(tokenize(a.getLocation()), tokenize(b.getLocation())).isEmpty()) {
            String loc = b.getLocation() != null && !b.getLocation().isBlank() ? b.getLocation() : a.getLocation();
            breakdown.add("Same location: " + loc, 2);
        }

        if (withinDays(a.getDateReported(), b.getDateReported(), DATE_WINDOW_DAYS)) {
            breakdown.add("Reported around the same time", 1);
        }

        return breakdown;
    }

    /** Splits text into a set of lower-case, non-stopword tokens (length &ge; 2). */
    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
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

    /** The tokens present in both sets (insertion order of {@code a} preserved). */
    private Set<String> sharedTokens(Set<String> a, Set<String> b) {
        Set<String> shared = new LinkedHashSet<>();
        if (a.isEmpty() || b.isEmpty()) {
            return shared;
        }
        for (String token : a) {
            if (b.contains(token)) {
                shared.add(token);
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
