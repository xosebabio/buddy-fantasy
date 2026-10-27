package com.buddyfantasy.match.domain;

/** Consistency warnings of a sheet. They never block saving or closing: in a Sunday league scorers may be missing. */
public enum MatchSheetWarning {
    /** Goals plus penalty goals of our players differ from the goals for. */
    GOALS_MISMATCH,
    /** Our players scored more own goals than the goals against. */
    OWN_GOALS_EXCEED_GOALS_AGAINST
}
