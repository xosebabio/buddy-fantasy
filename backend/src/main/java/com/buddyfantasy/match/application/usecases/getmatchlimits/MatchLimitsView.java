package com.buddyfantasy.match.application.usecases.getmatchlimits;

public record MatchLimitsView(int opponentMaxLength, int matchdayMin, int goalsMax, int statCountMax,
                              int sheetPlayersMax) {
}
