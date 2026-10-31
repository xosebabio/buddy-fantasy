import { HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { apiPath } from '../api-path';
import type { components, paths } from '../schema';

export type PlayerResponse = components['schemas']['PlayerResponse'];

type PlayersQuery = NonNullable<paths['/api/leagues/{id}/players']['get']['parameters']['query']>;

/**
 * Every screen asks for the whole squad, retired players included, to cross the ids of lineups,
 * sheets and points; the lists of the squad filter the retired ones themselves.
 */
const FULL_SQUAD: PlayersQuery = { includeRetired: true };

/**
 * Endpoints of the backend's `squad` module for the members. Its reads return an `httpResource`, so
 * they must be called in an injection context, such as a field initializer of the page; a read
 * whose arguments are `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class SquadApi {
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
}
