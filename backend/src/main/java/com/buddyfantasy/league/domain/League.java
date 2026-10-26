package com.buddyfantasy.league.domain;

import java.time.Instant;

import com.buddyfantasy.shared.domain.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class League extends AggregateRoot {

    private final LeagueId id;
    private final String name;
    private final Instant createdAt;

    public static League create(String name, Instant now) {
        return new League(LeagueId.generate(), name, now);
    }
}
