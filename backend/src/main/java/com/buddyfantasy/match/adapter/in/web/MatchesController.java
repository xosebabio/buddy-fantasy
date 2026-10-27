package com.buddyfantasy.match.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.buddyfantasy.match.application.usecases.closematch.CloseMatchCommand;
import com.buddyfantasy.match.application.usecases.deletematch.DeleteMatchCommand;
import com.buddyfantasy.match.application.usecases.getmatch.GetMatchQuery;
import com.buddyfantasy.match.application.usecases.getmatch.MatchView;
import com.buddyfantasy.match.application.usecases.listmatches.ListMatchesQuery;
import com.buddyfantasy.match.application.usecases.listmatches.MatchSummaryView;
import com.buddyfantasy.match.application.usecases.recordmatchsheet.RecordMatchSheetCommand;
import com.buddyfantasy.match.application.usecases.schedulematch.ScheduleMatchCommand;
import com.buddyfantasy.match.application.usecases.updatematch.UpdateMatchCommand;
import com.buddyfantasy.match.domain.MatchId;
import com.buddyfantasy.match.domain.MatchSheetWarning;
import com.buddyfantasy.shared.application.CommandBus;
import com.buddyfantasy.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MatchesController {

    private final CommandBus bus;

    MatchesController(CommandBus bus) {
        this.bus = bus;
    }

    @PostMapping("/api/leagues/{id}/matches")
    ResponseEntity<MatchView> schedule(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                       @Valid @RequestBody MatchRequest request) {
        MatchId matchId = bus.dispatch(new ScheduleMatchCommand(user.userId(), id, request.matchday(),
                request.opponent(), request.venue(), request.kickoffAt()));
        return ResponseEntity.created(URI.create("/api/leagues/" + id + "/matches/" + matchId.value()))
                .body(get(user, id, matchId.value()));
    }

    @GetMapping("/api/leagues/{id}/matches")
    List<MatchSummaryView> list(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return bus.ask(new ListMatchesQuery(user.userId(), id));
    }

    @GetMapping("/api/leagues/{id}/matches/{matchId}")
    MatchView get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID matchId) {
        return bus.ask(new GetMatchQuery(user.userId(), id, matchId));
    }

    @PutMapping("/api/leagues/{id}/matches/{matchId}")
    MatchView update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID matchId,
                     @Valid @RequestBody MatchRequest request) {
        bus.dispatch(new UpdateMatchCommand(user.userId(), id, matchId, request.matchday(), request.opponent(),
                request.venue(), request.kickoffAt()));
        return get(user, id, matchId);
    }

    @DeleteMapping("/api/leagues/{id}/matches/{matchId}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                @PathVariable UUID matchId) {
        bus.dispatch(new DeleteMatchCommand(user.userId(), id, matchId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/leagues/{id}/matches/{matchId}/sheet")
    MatchSheetResponse recordSheet(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                   @PathVariable UUID matchId, @Valid @RequestBody MatchSheetRequest request) {
        MatchResultRequest result = request.result();
        List<MatchSheetWarning> warnings = bus.dispatch(new RecordMatchSheetCommand(user.userId(), id, matchId,
                result == null ? null : result.goalsFor(), result == null ? null : result.goalsAgainst(),
                request.performances().stream().map(PlayerPerformanceRequest::toLine).toList()));
        return new MatchSheetResponse(warnings);
    }

    @PostMapping("/api/leagues/{id}/matches/{matchId}/close")
    MatchView close(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id, @PathVariable UUID matchId) {
        bus.dispatch(new CloseMatchCommand(user.userId(), id, matchId));
        return get(user, id, matchId);
    }
}
