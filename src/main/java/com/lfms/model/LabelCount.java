package com.lfms.model;

/**
 * A simple label/count pair produced by aggregate queries — e.g. the most-reported lost
 * category, or a row in the "top loss locations" ranking. Immutable.
 */
public record LabelCount(String label, int count) {
}
