package com.buddyfantasy.match.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code match.limits} in {@code application.yml}; validated by the domain when the application starts. */
@ConfigurationProperties("match.limits")
record MatchLimitsProperties(int opponentMaxLength, int matchdayMin, int goalsMax, int statCountMax,
                             int sheetPlayersMax) {
}
