package com.buddyfantasy.match.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buddyfantasy.match.application.port.out.MatchRepository;
import com.buddyfantasy.match.domain.Match;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.Matchday;
import com.buddyfantasy.shared.domain.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaMatchRepository implements MatchRepository {

    /** Unique indexes of V8 on {@code (league_id, matchday)} and {@code (league_id, kickoff_at)}. */
    private static final String MATCHDAY_INDEX = "match_match_league_matchday_idx";
    private static final String KICKOFF_INDEX = "match_match_league_kickoff_idx";

    private final SpringDataMatchRepository jpa;

    JpaMatchRepository(SpringDataMatchRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Match> findByLeagueIdAndId(UUID leagueId, MatchId id) {
        return jpa.findByLeagueIdAndId(leagueId, id.value()).map(MatchMapper::toDomain);
    }

    @Override
    public boolean existsMatchday(UUID leagueId, Matchday matchday) {
        return jpa.existsByLeagueIdAndMatchday(leagueId, (short) matchday.value());
    }

    @Override
    public boolean existsKickoff(UUID leagueId, Instant kickoffAt) {
        return jpa.existsByLeagueIdAndKickoffAt(leagueId, kickoffAt);
    }

    @Override
    public void save(Match match) {
        try {
            jpa.saveAndFlush(MatchMapper.toEntity(match));
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains(MATCHDAY_INDEX)) {
                throw new ConflictException("Matchday already has a match");
            }
            if (message.contains(KICKOFF_INDEX)) {
                throw new ConflictException("Another match already starts at that time");
            }
            throw e;
        }
    }
}
