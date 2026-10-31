import { HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type StandingResponse = components['schemas']['StandingResponse'];
export type MatchdayScoresResponse = components['schemas']['MatchdayScoresResponse'];
export type MatchdayStandingResponse = components['schemas']['MatchdayStandingResponse'];
export type MatchdayPlayerResponse = components['schemas']['MatchdayPlayerResponse'];
export type ManagerMatchdayResponse = components['schemas']['ManagerMatchdayResponse'];
export type LinedUpPlayerResponse = components['schemas']['LinedUpPlayerResponse'];
export type PlayerMatchdayResponse = components['schemas']['PlayerMatchdayResponse'];
export type ScoringAction = components['schemas']['ScoringAction'];

/**
 * Endpoints of the backend's `scoring` module. Its reads return an `httpResource`, so they must be
 * called in an injection context, such as a field initializer of the page; a read whose arguments
 * are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class ScoringApi {
  /** The overall standings (RF-32). */
  standings(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<StandingResponse[] | undefined> {
    return httpResource<StandingResponse[]>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/standings', { id }), context };
    });
  }

  /**
   * The points of a matchday, of the managers and of the players; `409` `MATCH_NOT_SCORED` until
   * its rating window closes (RF-30 to RF-32).
   */
  matchday(
    leagueId: () => string | undefined,
    matchId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchdayScoresResponse | undefined> {
    return httpResource<MatchdayScoresResponse>(() => {
      const id = leagueId();
      const match = matchId();
      return id === undefined || match === undefined
        ? undefined
        : {
            url: apiPath('/api/leagues/{id}/standings/{matchId}', { id, matchId: match }),
            context,
          };
    });
  }

  /** A manager's points per matchday, from the oldest one (RF-33). */
  managerHistory(
    leagueId: () => string | undefined,
    userId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<ManagerMatchdayResponse[] | undefined> {
    return httpResource<ManagerMatchdayResponse[]>(() => {
      const id = leagueId();
      const user = userId();
      return id === undefined || user === undefined
        ? undefined
        : {
            url: apiPath('/api/leagues/{id}/managers/{userId}/history', { id, userId: user }),
            context,
          };
    });
  }

  /** A player's points per matchday (RF-11). */
  playerPoints(
    leagueId: () => string | undefined,
    playerId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<PlayerMatchdayResponse[] | undefined> {
    return httpResource<PlayerMatchdayResponse[]>(() => {
      const id = leagueId();
      const player = playerId();
      return id === undefined || player === undefined
        ? undefined
        : {
            url: apiPath('/api/leagues/{id}/players/{playerId}/points', { id, playerId: player }),
            context,
          };
    });
  }
}
