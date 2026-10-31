import { HttpClient, HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type MatchSummaryResponse = components['schemas']['MatchSummaryResponse'];
export type MatchResponse = components['schemas']['MatchResponse'];
export type MatchResultResponse = components['schemas']['MatchResultResponse'];
export type PlayerPerformanceResponse = components['schemas']['PlayerPerformanceResponse'];
export type MatchRequest = components['schemas']['MatchRequest'];
export type MatchSheetRequest = components['schemas']['MatchSheetRequest'];
export type MatchSheetResponse = components['schemas']['MatchSheetResponse'];
export type MatchResultRequest = components['schemas']['MatchResultRequest'];
export type PlayerPerformanceRequest = components['schemas']['PlayerPerformanceRequest'];
export type MatchAuditEntryResponse = components['schemas']['MatchAuditEntryResponse'];
export type MatchSnapshotResponse = components['schemas']['MatchSnapshotResponse'];
export type MatchLimitsResponse = components['schemas']['MatchLimitsResponse'];
export type MatchStatus = MatchResponse['status'];
export type Venue = MatchResponse['venue'];

/**
 * Endpoints of the backend's `match` module, with the admin's writes. Its reads return an `httpResource`, so
 * they must be called in an injection context, such as a field initializer of the page; a read
 * whose arguments are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class MatchApi {
  private readonly http = inject(HttpClient);

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

  /** The configured limits of the match and sheet forms (RF-12, RF-13); only for the admin. */
  limits(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchLimitsResponse | undefined> {
    return httpResource<MatchLimitsResponse>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/matches/limits', { id }), context };
    });
  }

  /** The changes the admin made to a match, the latest first (RF-16, RNF-04). */
  audit(
    leagueId: () => string | undefined,
    matchId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchAuditEntryResponse[] | undefined> {
    return httpResource<MatchAuditEntryResponse[]>(() => {
      const id = leagueId();
      const match = matchId();
      return id === undefined || match === undefined
        ? undefined
        : {
            url: apiPath('/api/leagues/{id}/matches/{matchId}/audit', { id, matchId: match }),
            context,
          };
    });
  }

  /** Schedules a match (RF-12). */
  schedule(leagueId: string, body: MatchRequest): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(
      apiPath('/api/leagues/{id}/matches', { id: leagueId }),
      body,
    );
  }

  /** Replaces the data of a match, which postpones it if the matchday or the time change (RF-12). */
  update(leagueId: string, matchId: string, body: MatchRequest): Observable<MatchResponse> {
    return this.http.put<MatchResponse>(
      apiPath('/api/leagues/{id}/matches/{matchId}', { id: leagueId, matchId }),
      body,
    );
  }

  /** Deletes a match that is not closed; `409` `MATCH_CLOSED` otherwise (RF-12). */
  delete(leagueId: string, matchId: string): Observable<null> {
    return this.http.delete<null>(
      apiPath('/api/leagues/{id}/matches/{matchId}', { id: leagueId, matchId }),
    );
  }

  /** Records the sheet of a match, or corrects it once closed (RF-13, RF-14, RF-16). */
  recordSheet(
    leagueId: string,
    matchId: string,
    body: MatchSheetRequest,
  ): Observable<MatchSheetResponse> {
    return this.http.put<MatchSheetResponse>(
      apiPath('/api/leagues/{id}/matches/{matchId}/sheet', { id: leagueId, matchId }),
      body,
    );
  }

  /** Closes a match, which opens its rating window (RF-14). */
  close(leagueId: string, matchId: string, context?: HttpContext): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(
      apiPath('/api/leagues/{id}/matches/{matchId}/close', { id: leagueId, matchId }),
      null,
      { context },
    );
  }
}
