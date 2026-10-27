package com.buddyfantasy.league.adapter.out.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.LeagueSettings;
import org.springframework.stereotype.Repository;

@Repository
class JpaLeagueRepository implements LeagueRepository {

    private final SpringDataLeagueRepository leagues;
    private final SpringDataLeagueSettingsRepository settings;
    private final SpringDataScoringRuleRepository rules;

    JpaLeagueRepository(SpringDataLeagueRepository leagues, SpringDataLeagueSettingsRepository settings,
                        SpringDataScoringRuleRepository rules) {
        this.leagues = leagues;
        this.settings = settings;
        this.rules = rules;
    }

    @Override
    public Optional<League> findById(UUID id) {
        return leagues.findById(id).map(league -> LeagueMapper.toDomain(league,
                settings.findById(id).orElseThrow(), rules.findByLeagueId(id)));
    }

    @Override
    public Map<UUID, LeagueSettings> findSettingsByIds(Collection<UUID> ids) {
        return settings.findAllById(ids).stream()
                .collect(Collectors.toMap(e -> e.leagueId, LeagueSettingsMapper::toDomain));
    }

    @Override
    public void save(League league) {
        UUID id = league.id().value();
        leagues.save(LeagueMapper.toEntity(league));
        settings.save(LeagueSettingsMapper.toEntity(id, league.settings()));
        rules.saveAll(ScoringRuleMapper.toEntities(id, league.scoringRules()));
    }
}
