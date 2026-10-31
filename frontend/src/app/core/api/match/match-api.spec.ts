import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { MatchApi, type MatchResponse, type MatchSummaryResponse } from './match-api';

const LEAGUE_ID = '3f6c2a1e';

const SUMMARY: MatchSummaryResponse = {
  id: 'm1',
  matchday: 11,
  opponent: 'CD Arrabal',
  venue: 'HOME',
  kickoffAt: '2026-10-26T10:00:00Z',
  status: 'CLOSED',
  closedAt: '2026-10-26T13:00:00Z',
  result: { goalsFor: 3, goalsAgainst: 1 },
};

const MATCH: MatchResponse = {
  ...SUMMARY,
  performances: [
    {
      playerId: 'p1',
      position: 'GOALKEEPER',
      goals: 0,
      penaltyGoals: 0,
      penaltiesSaved: 1,
      penaltiesMissed: 0,
      assists: 0,
      ownGoals: 0,
      yellowCard: false,
      doubleYellow: false,
      redCard: false,
    },
  ],
  warnings: null,
};

describe('MatchApi', () => {
  let api: MatchApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(MatchApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it('reads the calendar once the league is known', async () => {
    const leagueId = signal<string | undefined>(undefined);
    const matches = TestBed.runInInjectionContext(() => api.matches(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/matches` }).flush([SUMMARY]);
    await settle();

    expect(matches.value()).toEqual([SUMMARY]);
  });

  it('reads a match once the league and the match are known', async () => {
    const matchId = signal<string | undefined>(undefined);
    const match = TestBed.runInInjectionContext(() => api.match(signal(LEAGUE_ID), matchId));

    TestBed.tick();
    http.expectNone(() => true);

    matchId.set('m1');
    TestBed.tick();
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/matches/m1` }).flush(MATCH);
    await settle();

    expect(match.value()).toEqual(MATCH);
  });
});
