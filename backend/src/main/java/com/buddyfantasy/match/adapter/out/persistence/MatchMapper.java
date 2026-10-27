package com.buddyfantasy.match.adapter.out.persistence;

import com.buddyfantasy.match.MatchSchedule;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.MatchResult;
import com.buddyfantasy.match.domain.MatchSheet;
import com.buddyfantasy.match.domain.MatchStatus;
import com.buddyfantasy.match.domain.Matchday;
import com.buddyfantasy.match.domain.PlayerPerformance;
import lombok.experimental.UtilityClass;

@UtilityClass
class MatchMapper {

    Match toDomain(MatchEntity e) {
        MatchResult result = e.goalsFor == null ? null : new MatchResult(e.goalsFor, e.goalsAgainst);
        MatchSheet sheet = new MatchSheet(result, e.performances.stream().map(MatchMapper::toPerformance).toList());
        return new Match(new MatchId(e.id), e.leagueId, new Matchday(e.matchday), e.opponent, e.venue, e.kickoffAt,
                e.status, sheet, e.createdAt, e.closedAt);
    }

    MatchEntity toEntity(Match m) {
        MatchResult result = m.sheet().result();
        return new MatchEntity(m.id().value(), m.leagueId(), (short) m.matchday().value(), m.opponent(), m.venue(),
                m.kickoffAt(), m.status(), result == null ? null : (short) result.goalsFor(),
                result == null ? null : (short) result.goalsAgainst(), m.createdAt(), m.closedAt(),
                m.sheet().performances().stream().map(MatchMapper::toEmbeddable).toList());
    }

    /** Does not touch the lazy sheet. */
    MatchSchedule toSchedule(MatchEntity e) {
        return new MatchSchedule(e.id, e.leagueId, e.matchday, e.kickoffAt, e.status == MatchStatus.CLOSED);
    }

    PlayerPerformance toPerformance(PlayerPerformanceEmbeddable e) {
        return new PlayerPerformance(e.playerId, e.position, e.goals, e.penaltyGoals, e.penaltiesSaved,
                e.penaltiesMissed, e.assists, e.ownGoals, e.yellowCard, e.doubleYellow, e.redCard);
    }

    PlayerPerformanceEmbeddable toEmbeddable(PlayerPerformance p) {
        return new PlayerPerformanceEmbeddable(p.playerId(), p.position(), (short) p.goals(), (short) p.penaltyGoals(),
                (short) p.penaltiesSaved(), (short) p.penaltiesMissed(), (short) p.assists(), (short) p.ownGoals(),
                p.yellowCard(), p.doubleYellow(), p.redCard());
    }
}
