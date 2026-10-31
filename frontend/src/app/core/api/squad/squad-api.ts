import { HttpClient, HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components, paths } from '../schema';

export type PlayerResponse = components['schemas']['PlayerResponse'];
export type PlayerRequest = components['schemas']['PlayerRequest'];
export type SquadLimitsResponse = components['schemas']['SquadLimitsResponse'];

type PlayersQuery = NonNullable<paths['/api/leagues/{id}/players']['get']['parameters']['query']>;

/**
 * Every screen asks for the whole squad, retired players included, to cross the ids of lineups,
 * sheets and points; the lists of the squad filter the retired ones themselves.
 */
const FULL_SQUAD: PlayersQuery = { includeRetired: true };

/**
 * Endpoints of the backend's `squad` module, with the admin's writes (the photo endpoints have no
 * screen in v1). Its reads return an `httpResource`, so
 * they must be called in an injection context, such as a field initializer of the page; a read
 * whose arguments are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class SquadApi {
  private readonly http = inject(HttpClient);

  /** The whole squad, with the retired players (RF-11). */
  players(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<PlayerResponse[] | undefined> {
    return httpResource<PlayerResponse[]>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/players', { id }), params: FULL_SQUAD, context };
    });
  }

  /** A player, retired or not (RF-11). */
  player(
    leagueId: () => string | undefined,
    playerId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<PlayerResponse | undefined> {
    return httpResource<PlayerResponse>(() => {
      const id = leagueId();
      const player = playerId();
      return id === undefined || player === undefined
        ? undefined
        : {
            url: apiPath('/api/leagues/{id}/players/{playerId}', { id, playerId: player }),
            context,
          };
    });
  }

  /** The configured limits of the player form (RF-09, RF-10); only for the admin. */
  limits(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<SquadLimitsResponse | undefined> {
    return httpResource<SquadLimitsResponse>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/players/limits', { id }), context };
    });
  }

  /** Registers a player in the squad (RF-09). */
  register(leagueId: string, body: PlayerRequest): Observable<PlayerResponse> {
    return this.http.post<PlayerResponse>(
      apiPath('/api/leagues/{id}/players', { id: leagueId }),
      body,
    );
  }

  /** Replaces a player's data; `409` `PLAYER_RETIRED` if the player is retired (RF-09). */
  update(leagueId: string, playerId: string, body: PlayerRequest): Observable<PlayerResponse> {
    return this.http.put<PlayerResponse>(
      apiPath('/api/leagues/{id}/players/{playerId}', { id: leagueId, playerId }),
      body,
    );
  }

  /** Retires a player; it stays in sheets, points and histories (RF-10). */
  retire(leagueId: string, playerId: string): Observable<null> {
    return this.http.delete<null>(
      apiPath('/api/leagues/{id}/players/{playerId}', { id: leagueId, playerId }),
    );
  }
}
