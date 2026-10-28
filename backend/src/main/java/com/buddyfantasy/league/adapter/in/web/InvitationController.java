package com.buddyfantasy.league.adapter.in.web;

import java.util.UUID;

import com.buddyfantasy.league.application.usecases.joinleague.JoinLeagueCommand;
import com.buddyfantasy.league.application.usecases.previewinvitation.InvitationPreview;
import com.buddyfantasy.league.application.usecases.previewinvitation.PreviewInvitationQuery;
import com.buddyfantasy.league.application.usecases.regenerateinvitation.RegenerateInvitationCommand;
import com.buddyfantasy.league.application.usecases.revokeinvitation.RevokeInvitationCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invitation tokens travel in the path: never log request URLs under {@code /api/invitations/}. */
@RestController
@RequiredArgsConstructor
class InvitationController {

    private final CommandBus bus;

    @PostMapping("/api/leagues/{id}/invitation")
    InvitationTokenResponse regenerate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return new InvitationTokenResponse(bus.dispatch(new RegenerateInvitationCommand(user.userId(), id)));
    }

    @DeleteMapping("/api/leagues/{id}/invitation")
    ResponseEntity<Void> revoke(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        bus.dispatch(new RevokeInvitationCommand(user.userId(), id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/invitations/{token}")
    InvitationPreview preview(@AuthenticationPrincipal CurrentUser user, @PathVariable String token) {
        return bus.ask(new PreviewInvitationQuery(user.userId(), token));
    }

    @PostMapping("/api/invitations/{token}/join")
    JoinLeagueResponse join(@AuthenticationPrincipal CurrentUser user, @PathVariable String token) {
        return new JoinLeagueResponse(bus.dispatch(new JoinLeagueCommand(user.userId(), token)).value());
    }
}
