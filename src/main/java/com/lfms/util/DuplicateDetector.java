package com.lfms.util;

import com.lfms.model.Item;
import com.lfms.model.Match;
import com.lfms.model.MatchBreakdown;
import com.lfms.service.MatchingEngine;

/**
 * Real-time "you may be reporting a duplicate" helper shared by both report forms. It runs the
 * matching engine in non-persisting {@link MatchingEngine#preview(Item) preview} mode against a
 * draft the user is still typing, and surfaces the first HIGH-confidence match (if any). Nothing
 * is written to the database.
 */
public final class DuplicateDetector {

    private static final MatchingEngine ENGINE = new MatchingEngine();

    private DuplicateDetector() {
    }

    /**
     * The first HIGH-confidence match for the in-progress draft, or {@code null} if there is none
     * (or there is not yet enough text to compare).
     */
    public static Match firstHighMatch(String type, String name, String category,
                                       String description, String location, String date) {
        if (isBlank(name) && isBlank(description)) {
            return null;
        }
        Item draft = new Item();
        draft.setItemId(0);
        draft.setType(type);
        draft.setName(name);
        draft.setCategory(category);
        draft.setDescription(description);
        draft.setLocation(location);
        draft.setDateReported(date);

        for (Match match : ENGINE.preview(draft)) {
            MatchBreakdown breakdown = match.getBreakdown();
            if (breakdown != null && "HIGH".equals(breakdown.getConfidence())) {
                return match;
            }
        }
        return null;
    }

    /** The id of the existing item a draft match points at (the side that is not the draft). */
    public static int counterpartId(Match match) {
        return match.getLostItemId() != 0 ? match.getLostItemId() : match.getFoundItemId();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
