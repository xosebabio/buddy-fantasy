package com.buddyfantasy.league.domain;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTests {

    static final Instant NOW = Instant.parse("2026-01-10T10:00:00Z");

    @Test
    void issuedTokenIsUrlSafeAndOnlyItsHashIsStored() {
        Invitation.Issued issued = Invitation.issue(LeagueId.generate(), NOW);

        assertThat(issued.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(issued.invitation().tokenHash()).isEqualTo(Invitation.hash(issued.token()))
                .matches("[0-9a-f]{64}").isNotEqualTo(issued.token());
        assertThat(issued.invitation().isActive()).isTrue();
    }

    @Test
    void tokensAreUnique() {
        LeagueId league = LeagueId.generate();
        assertThat(Invitation.issue(league, NOW).token()).isNotEqualTo(Invitation.issue(league, NOW).token());
    }

    @Test
    void matchesOnlyItsOwnToken() {
        Invitation.Issued issued = Invitation.issue(LeagueId.generate(), NOW);

        assertThat(issued.invitation().matches(issued.token())).isTrue();
        assertThat(issued.invitation().matches("other")).isFalse();
        assertThat(issued.invitation().matches(null)).isFalse();
    }

    @Test
    void revokeIsIdempotentAndKeepsTheFirstInstant() {
        Invitation invitation = Invitation.issue(LeagueId.generate(), NOW).invitation();

        invitation.revoke(NOW.plusSeconds(10));
        invitation.revoke(NOW.plusSeconds(99));

        assertThat(invitation.isActive()).isFalse();
        assertThat(invitation.revokedAt()).isEqualTo(NOW.plusSeconds(10));
    }
}
