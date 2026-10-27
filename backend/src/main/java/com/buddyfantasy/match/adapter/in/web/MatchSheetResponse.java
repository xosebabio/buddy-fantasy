package com.buddyfantasy.match.adapter.in.web;

import java.util.List;

import com.buddyfantasy.match.domain.MatchSheetWarning;

/** Warning codes ({@code GOALS_MISMATCH}…) that the frontend translates; they are not errors. */
record MatchSheetResponse(List<MatchSheetWarning> warnings) {
}
