import { HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type MatchSummaryResponse = components['schemas']['MatchSummaryResponse'];
export type MatchResponse = components['schemas']['MatchResponse'];
export type MatchResultResponse = components['schemas']['MatchResultResponse'];
export type PlayerPerformanceResponse = components['schemas']['PlayerPerformanceResponse'];
export type MatchStatus = MatchResponse['status'];
export type Venue = MatchResponse['venue'];

/**
 * Endpoints of the backend's `match` module for the members. Its reads return an `httpResource`, so
 * they must be called in an injection context, such as a field initializer of the page; a read
 * whose arguments are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class MatchApi {
  /** The league's calendar, with the result of the closed matches (RF-12). */
  matches(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchSummaryResponse[] | undefined> {
    return httpResource<MatchSummaryResponse[]>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/matches', { id }), context };
    });
  }

  /** A match with its sheet once it is closed (RF-13, RF-14). */
  match(
    leagueId: () => string | undefined,
    matchId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchResponse | undefined> {
    return httpResource<MatchResponse>(() => {
      const id = leagueId();
      const match = matchId();
      return id === undefined || match === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/matches/{matchId}', { id, matchId: match }), context };
    });
  }
}
