package com.buddyfantasy.match.adapter.in.web;

import java.util.List;

import com.buddyfantasy.match.domain.MatchSheet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code result} is null in a draft of a scheduled match. The domain checks the remaining rules. */
record MatchSheetRequest(@Valid MatchResultRequest result,
                         @NotNull @Size(max = MatchSheet.MAX_PERFORMANCES)
                         List<@NotNull @Valid PlayerPerformanceRequest> performances) {
}
