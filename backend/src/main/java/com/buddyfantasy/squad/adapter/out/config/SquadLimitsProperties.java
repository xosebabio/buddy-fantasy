package com.buddyfantasy.squad.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** {@code squad.limits} in {@code application.yml}; validated by the domain when the application starts. */
@ConfigurationProperties("squad.limits")
record SquadLimitsProperties(int nameMaxLength, int shirtNumberMin, int shirtNumberMax, DataSize photoMaxSize) {
}
