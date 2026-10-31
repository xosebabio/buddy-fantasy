import { HttpClient, HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type RatingWindowResponse = components['schemas']['RatingWindowResponse'];
export type MatchRatingsResponse = components['schemas']['MatchRatingsResponse'];
export type PlayerRatingResponse = components['schemas']['PlayerRatingResponse'];
export type RatingsRequest = components['schemas']['RatingsRequest'];
export type PlayerRatingRequest = components['schemas']['PlayerRatingRequest'];
export type RatingFormat = MatchRatingsResponse['format'];
export type RatingWindowStatus = MatchRatingsResponse['status'];

/**
 * Endpoints of the backend's `rating` module for the members. The reads return an `httpResource`,
 * so they must be called in an injection context, such as a field initializer of the page; a read
 * whose arguments are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class RatingApi {
  private readonly http = inject(HttpClient);

  /** The league's rating windows, with how many players the user has rated (RF-17). */
  windows(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<RatingWindowResponse[] | undefined> {
    return httpResource<RatingWindowResponse[]>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/ratings', { id }), context };
    });
  }

  /**
   * The ratings of a match: only the user's own while the window is open, and the results once it
   * is closed; `409` `MATCH_NOT_CLOSED` before the match is (RF-18 to RF-23).
   */
  ratings(
    leagueId: () => string | undefined,
    matchId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MatchRatingsResponse | undefined> {
    return httpResource<MatchRatingsResponse>(() => {
      const id = leagueId();
      const match = matchId();
      return id === undefined || match === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/ratings/{matchId}', { id, matchId: match }), context };
    });
  }

  /**
   * Replaces the user's ratings of a match; `400` if one is not valid and `409` if the user cannot
   * rate it (RF-18 to RF-20).
   */
  save(leagueId: string, matchId: string, body: RatingsRequest): Observable<MatchRatingsResponse> {
    return this.http.put<MatchRatingsResponse>(
      apiPath('/api/leagues/{id}/ratings/{matchId}', { id: leagueId, matchId }),
      body,
    );
  }
}
