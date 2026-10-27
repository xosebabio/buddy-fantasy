package com.buddyfantasy.match.adapter.in.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * {@code result} is null in a draft of a scheduled match. The domain checks the remaining rules, including
 * {@code match.limits}.
 */
record MatchSheetRequest(@Valid MatchResultRequest result,
                         @NotNull List<@NotNull @Valid PlayerPerformanceRequest> performances) {
}
