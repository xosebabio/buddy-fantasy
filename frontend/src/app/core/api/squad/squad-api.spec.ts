import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { type PlayerResponse, SquadApi } from './squad-api';

const LEAGUE_ID = '3f6c2a1e';

const PLAYER: PlayerResponse = {
  id: 'p1',
  name: 'Diego',
  position: 'GOALKEEPER',
  shirtNumber: 1,
  initialValue: 10,
  currentValue: 11.5,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: null,
};

describe('SquadApi', () => {
  let api: SquadApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(SquadApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it('reads the whole squad, with the retired players, once the league is known', async () => {
    const leagueId = signal<string | undefined>(undefined);
    const players = TestBed.runInInjectionContext(() => api.players(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    http
      .expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/players?includeRetired=true` })
      .flush([PLAYER]);
    await settle();

    expect(players.value()).toEqual([PLAYER]);
  });

  it('reads a player once the league and the player are known, with the given context', async () => {
    const token = new HttpContextToken(() => false);
    const playerId = signal<string | undefined>(undefined);
    const player = TestBed.runInInjectionContext(() =>
      api.player(signal(LEAGUE_ID), playerId, new HttpContext().set(token, true)),
    );

    TestBed.tick();
    http.expectNone(() => true);

    playerId.set('p1');
    TestBed.tick();
    const request = http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/players/p1` });
    expect(request.request.context.get(token)).toBe(true);
    request.flush(PLAYER);
    await settle();

    expect(player.value()).toEqual(PLAYER);
  });
});
