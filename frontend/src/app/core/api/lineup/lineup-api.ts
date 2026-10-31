import { HttpClient, HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type MyLineupResponse = components['schemas']['MyLineupResponse'];
export type LineupPlayerResponse = components['schemas']['LineupPlayerResponse'];
export type TargetMatchResponse = components['schemas']['TargetMatchResponse'];
export type LineupRequest = components['schemas']['LineupRequest'];
export type LockedLineupsResponse = components['schemas']['LockedLineupsResponse'];
export type LockedLineupResponse = components['schemas']['LockedLineupResponse'];
export type LockedPlayerResponse = components['schemas']['LockedPlayerResponse'];
export type LineupWarning = MyLineupResponse['warnings'][number];

/**
 * Endpoints of the backend's `lineup` module. The reads return an `httpResource`, so they must be
 * called in an injection context, such as a field initializer of the page; a read whose arguments
 * are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class LineupApi {
  private readonly http = inject(HttpClient);

  /** The user's working lineup, for the next match to lock (RF-24, RF-25). */
  myLineup(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MyLineupResponse | undefined> {
    return httpResource<MyLineupResponse>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/lineup', { id }), context };
    });
  }

  /**
   * Replaces the user's working lineup; `400` if it is not valid and `409` `MAX_CHANGES_EXCEEDED`
   * (RF-24 to RF-29).
   */
  save(leagueId: string, body: LineupRequest): Observable<MyLineupResponse> {
    return this.http.put<MyLineupResponse>(
      apiPath('/api/leagues/{id}/lineup', { id: leagueId }),
      body,
    );
  }

  /** Every manager's lineup locked for a match; `409` `LINEUPS_NOT_LOCKED` before that (RF-27). */
  locked(
    leagueId: () => string | undefined,
    matchId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<LockedLineupsResponse | undefined> {
    return httpResource<LockedLineupsResponse>(() => {
      const id = leagueId();
      const match = matchId();
      return id === undefined || match === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/lineups/{matchId}', { id, matchId: match }), context };
    });
  }
}
