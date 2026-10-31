import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { LineupApi, type LockedLineupsResponse, type MyLineupResponse } from './lineup-api';

const LEAGUE_ID = '3f6c2a1e';

const LINEUP: MyLineupResponse = {
  targetMatch: {
    id: 'm2',
    matchday: 12,
    kickoffAt: '2026-11-02T10:00:00Z',
    lockAt: '2026-11-02T09:00:00Z',
  },
  players: [{ id: 'p1', position: 'GOALKEEPER', value: 11.5, active: true }],
  totalValue: 11.5,
  budget: 100,
  changesUsed: 1,
  maxChanges: 2,
  changesRemaining: 1,
  savedAt: '2026-10-30T18:00:00Z',
  warnings: ['SIZE_MISMATCH'],
};

const LOCKED: LockedLineupsResponse = {
  matchId: 'm1',
  matchday: 11,
  kickoffAt: '2026-10-26T10:00:00Z',
  lockAt: '2026-10-26T09:00:00Z',
  lockedAt: '2026-10-26T09:00:02Z',
  lineups: [
    {
      userId: 'u2',
      players: [{ id: 'p1', value: 11 }],
      totalValue: 11,
      budget: 100,
      overBudget: false,
    },
  ],
};

describe('LineupApi', () => {
  let api: LineupApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(LineupApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it("reads the user's lineup once the league is known", async () => {
    const leagueId = signal<string | undefined>(undefined);
    const lineup = TestBed.runInInjectionContext(() => api.myLineup(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/lineup` }).flush(LINEUP);
    await settle();

    expect(lineup.value()).toEqual(LINEUP);
  });

  it("saves the user's lineup", async () => {
    const result = firstValueFrom(api.save(LEAGUE_ID, { playerIds: ['p1'] }));

    const request = http.expectOne({ method: 'PUT', url: `/api/leagues/${LEAGUE_ID}/lineup` });
    expect(request.request.body).toEqual({ playerIds: ['p1'] });
    request.flush(LINEUP);

    expect(await result).toEqual(LINEUP);
  });

  it('reads the locked lineups of a match, with the given context', async () => {
    const token = new HttpContextToken(() => false);
    const matchId = signal<string | undefined>(undefined);
    const locked = TestBed.runInInjectionContext(() =>
      api.locked(signal(LEAGUE_ID), matchId, new HttpContext().set(token, true)),
    );

    TestBed.tick();
    http.expectNone(() => true);

    matchId.set('m1');
    TestBed.tick();
    const request = http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/lineups/m1` });
    expect(request.request.context.get(token)).toBe(true);
    request.flush(LOCKED);
    await settle();

    expect(locked.value()).toEqual(LOCKED);
  });
});
