package com.buddyfantasy.league.adapter.in.web;

import com.buddyfantasy.league.domain.League;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateLeagueRequest(@NotBlank @Size(max = League.MAX_NAME_LENGTH) String name) {
}
