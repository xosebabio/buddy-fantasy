package com.buddyfantasy.match.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Venue;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "match_match")
class MatchEntity {

    @Id
    UUID id;

    @Column(name = "league_id", nullable = false)
    UUID leagueId;

    @Column(name = "matchday", nullable = false)
    short matchday;

    @Column(name = "opponent", nullable = false)
    String opponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue", nullable = false)
    Venue venue;

    @Column(name = "kickoff_at", nullable = false)
    Instant kickoffAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    MatchStatus status;

    @Column(name = "goals_for")
    Short goalsFor;

    @Column(name = "goals_against")
    Short goalsAgainst;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "closed_at")
    Instant closedAt;

    /** The sheet is replaced as a whole: Hibernate deletes and reinserts the rows when it changes. */
    @ElementCollection
    @CollectionTable(name = "match_player_performance", joinColumns = @JoinColumn(name = "match_id"))
    List<PlayerPerformanceEmbeddable> performances = new ArrayList<>();

    protected MatchEntity() {
    }

    MatchEntity(UUID id, UUID leagueId, short matchday, String opponent, Venue venue, Instant kickoffAt,
                MatchStatus status, Short goalsFor, Short goalsAgainst, Instant createdAt, Instant closedAt,
                List<PlayerPerformanceEmbeddable> performances) {
        this.id = id;
        this.leagueId = leagueId;
        this.matchday = matchday;
        this.opponent = opponent;
        this.venue = venue;
        this.kickoffAt = kickoffAt;
        this.status = status;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.performances = new ArrayList<>(performances);
    }
}
