package com.buddyfantasy.league;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.buddyfantasy.PostgresTestConfig;
import com.buddyfantasy.league.application.port.out.LeagueDefaultsRepository;
import com.buddyfantasy.league.application.port.out.LeagueLimitsRepository;
import com.buddyfantasy.league.application.port.out.LeagueRepository;
import com.buddyfantasy.league.application.port.out.MembershipRepository;
import com.buddyfantasy.league.domain.League;
import com.buddyfantasy.league.domain.Membership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@SpringBootTest
@Import(PostgresTestConfig.class)
class LeagueAccessTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Autowired
    LeagueAccess access;

    @Autowired
    LeagueRepository leagues;

    @Autowired
    LeagueDefaultsRepository defaults;

    @Autowired
    LeagueLimitsRepository limits;

    @Autowired
    MembershipRepository memberships;

    private League league(String name) {
        League league = League.create(name, NOW, defaults.settings(), defaults.scoringRules(), limits.get());
        leagues.save(league);
        return league;
    }

    private void join(League league, UUID userId, LeagueRole role) {
        join(league, userId, role, NOW);
    }

    private void join(League league, UUID userId, LeagueRole role, Instant now) {
        memberships.save(Membership.join(league.id(), userId, role, now));
    }

    @Test
    @DisplayName("An admin passes both the member and the admin checks")
    void adminPassesBothChecks() {
        League league = league("A");
        UUID admin = UUID.randomUUID();
        join(league, admin, LeagueRole.ADMIN);

        assertThat(access.roleOf(admin, league.id().value())).contains(LeagueRole.ADMIN);
        assertThatCode(() -> access.requireMember(admin, league.id().value())).doesNotThrowAnyException();
        assertThat(access.requireMemberSince(admin, league.id().value())).isEqualTo(NOW);
        assertThatCode(() -> access.requireAdmin(admin, league.id().value())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A manager passes only the member check")
    void managerPassesOnlyRequireMember() {
        League league = league("A");
        UUID manager = UUID.randomUUID();
        join(league, manager, LeagueRole.MANAGER);

        assertThat(access.roleOf(manager, league.id().value())).contains(LeagueRole.MANAGER);
        assertThatCode(() -> access.requireMember(manager, league.id().value())).doesNotThrowAnyException();
        assertThat(access.requireMemberSince(manager, league.id().value())).isEqualTo(NOW);
        assertThatThrownBy(() -> access.requireAdmin(manager, league.id().value()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("A non-member is denied")
    void nonMemberIsDenied() {
        League league = league("A");
        UUID stranger = UUID.randomUUID();

        assertThat(access.roleOf(stranger, league.id().value())).isEmpty();
        assertThatThrownBy(() -> access.requireMember(stranger, league.id().value()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.requireMemberSince(stranger, league.id().value()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.requireAdmin(stranger, league.id().value()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("memberSince lists the admin and managers of that league only, with their join instant")
    void memberSinceListsAdminAndManagersOfThatLeagueOnlyWithTheirJoinInstant() {
        League league = league("A");
        League other = league("B");
        UUID admin = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        UUID foreigner = UUID.randomUUID();
        Instant later = NOW.plus(Duration.ofDays(3));
        join(league, admin, LeagueRole.ADMIN);
        join(league, manager, LeagueRole.MANAGER, later);
        join(other, foreigner, LeagueRole.ADMIN);

        assertThat(access.memberSince(league.id().value()))
                .containsOnly(entry(admin, NOW), entry(manager, later));
        assertThat(access.memberSince(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("A member who leaves and joins again is a member since joining again")
    void aMemberWhoLeavesAndJoinsAgainIsAMemberSinceJoiningAgain() {
        League league = league("A");
        UUID manager = UUID.randomUUID();
        join(league, manager, LeagueRole.MANAGER);
        memberships.delete(memberships.findByLeagueIdAndUserId(league.id().value(), manager).orElseThrow());
        Instant rejoined = NOW.plus(Duration.ofDays(3));
        join(league, manager, LeagueRole.MANAGER, rejoined);

        assertThat(access.requireMemberSince(manager, league.id().value())).isEqualTo(rejoined);
        assertThat(access.memberSince(league.id().value())).containsOnly(entry(manager, rejoined));
    }

    @Test
    @DisplayName("The same user can be admin in one league and manager in another")
    void sameUserCanBeAdminInOneLeagueAndManagerInAnother() {
        League a = league("A");
        League b = league("B");
        UUID user = UUID.randomUUID();
        join(a, user, LeagueRole.ADMIN);
        join(b, user, LeagueRole.MANAGER);

        assertThat(access.roleOf(user, a.id().value())).contains(LeagueRole.ADMIN);
        assertThat(access.roleOf(user, b.id().value())).contains(LeagueRole.MANAGER);
        assertThatCode(() -> access.requireAdmin(user, a.id().value())).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireAdmin(user, b.id().value()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
