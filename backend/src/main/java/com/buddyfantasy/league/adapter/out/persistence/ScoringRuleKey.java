package com.buddyfantasy.league.adapter.out.persistence;

import java.io.Serializable;
import java.util.UUID;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.ScoringAction;

record ScoringRuleKey(UUID leagueId, ScoringAction action, Position position) implements Serializable {
}
