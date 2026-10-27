package com.buddyfantasy.league.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code league.limits} in {@code application.yml}; validated by the domain when the application starts. */
@ConfigurationProperties("league.limits")
record LeagueLimitsProperties(int nameMaxLength, int lineupLockHoursMax) {
}
