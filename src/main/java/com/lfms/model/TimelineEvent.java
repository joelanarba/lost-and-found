package com.lfms.model;

/**
 * A single entry in an item's history timeline: a coloured dot (emoji), a short title and the
 * date it occurred. Built by {@link com.lfms.service.TimelineService} from the items, matches,
 * claims and audit-log tables.
 */
public record TimelineEvent(String icon, String title, String date) {
}
