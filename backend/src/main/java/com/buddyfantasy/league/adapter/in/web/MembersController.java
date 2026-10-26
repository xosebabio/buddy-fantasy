package com.buddyfantasy.league.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.buddyfantasy.league.application.usecases.leaveleague.LeaveLeagueCommand;
import com.buddyfantasy.league.application.usecases.listleaguemembers.ListLeagueMembersQuery;
import com.buddyfantasy.league.application.usecases.listleaguemembers.MemberView;
import com.buddyfantasy.league.application.usecases.removemember.RemoveMemberCommand;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MembersController {

    private final CommandBus bus;

    MembersController(CommandBus bus) {
        this.bus = bus;
    }

    @GetMapping("/api/leagues/{id}/members")
    List<MemberView> list(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new ListLeagueMembersQuery(user.userId(), id));
    }

    @DeleteMapping("/api/leagues/{id}/members/{memberId}")
    ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                @PathVariable UUID memberId) {
        bus.dispatch(new RemoveMemberCommand(user.userId(), id, memberId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/leagues/{id}/members/me")
    ResponseEntity<Void> leave(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        bus.dispatch(new LeaveLeagueCommand(user.userId(), id));
        return ResponseEntity.noContent().build();
    }
}
