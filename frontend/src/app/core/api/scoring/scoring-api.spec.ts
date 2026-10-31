import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import {
  type ManagerMatchdayResponse,
  type MatchdayScoresResponse,
  type PlayerMatchdayResponse,
  ScoringApi,
  type StandingResponse,
} from './scoring-api';

const LEAGUE_ID = '3f6c2a1e';

const STANDING: StandingResponse = {
  userId: 'u2',
  position: 1,
  points: 38,
  matchdays: 1,
  lastMatchdayPoints: 38,
};

const MATCHDAY: MatchdayScoresResponse = {
  matchId: 'm1',
  matchday: 11,
  kickoffAt: '2026-10-26T10:00:00Z',
  scoredAt: '2026-10-28T13:00:02Z',
  standings: [{ userId: 'u2', position: 1, points: 38, overBudget: false, playerIds: ['p1'] }],
  players: [{ id: 'p1', position: 'GOALKEEPER', actionPoints: 5, ratingPoints: 2, points: 7 }],
};

const HISTORY: ManagerMatchdayResponse[] = [
  {
    matchId: 'm1',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    points: 7,
    overBudget: false,
    inStandings: true,
    players: [
      {
        id: 'p1',
        position: 'GOALKEEPER',
        played: true,
        actions: { PENALTY_SAVED: 1 },
        actionPoints: 5,
        ratingPoints: 2,
        points: 7,
      },
    ],
  },
];

const PLAYER_POINTS: PlayerMatchdayResponse[] = [
  {
    matchId: 'm1',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    position: 'GOALKEEPER',
    actions: { PENALTY_SAVED: 1 },
    actionPoints: 5,
    ratingPoints: 2,
    points: 7,
  },
];

describe('ScoringApi', () => {
  let api: ScoringApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ScoringApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it('reads the overall standings once the league is known', async () => {
    const leagueId = signal<string | undefined>(undefined);
    const standings = TestBed.runInInjectionContext(() => api.standings(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/standings` }).flush([STANDING]);
    await settle();

    expect(standings.value()).toEqual([STANDING]);
  });

  it('reads the points of a matchday, with the given context', async () => {
    const token = new HttpContextToken(() => false);
    const matchId = signal<string | undefined>(undefined);
    const matchday = TestBed.runInInjectionContext(() =>
      api.matchday(signal(LEAGUE_ID), matchId, new HttpContext().set(token, true)),
    );

    TestBed.tick();
    http.expectNone(() => true);

    matchId.set('m1');
    TestBed.tick();
    const request = http.expectOne({
      method: 'GET',
      url: `/api/leagues/${LEAGUE_ID}/standings/m1`,
    });
    expect(request.request.context.get(token)).toBe(true);
    request.flush(MATCHDAY);
    await settle();

    expect(matchday.value()).toEqual(MATCHDAY);
  });

  it("reads a manager's history once the manager is known", async () => {
    const userId = signal<string | undefined>(undefined);
    const history = TestBed.runInInjectionContext(() =>
      api.managerHistory(signal(LEAGUE_ID), userId),
    );

    TestBed.tick();
    http.expectNone(() => true);

    userId.set('u2');
    TestBed.tick();
    http
      .expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/managers/u2/history` })
      .flush(HISTORY);
    await settle();

    expect(history.value()).toEqual(HISTORY);
  });

  it("reads a player's points once the player is known", async () => {
    const playerId = signal<string | undefined>(undefined);
    const points = TestBed.runInInjectionContext(() =>
      api.playerPoints(signal(LEAGUE_ID), playerId),
    );

    TestBed.tick();
    http.expectNone(() => true);

    playerId.set('p1');
    TestBed.tick();
    http
      .expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/players/p1/points` })
      .flush(PLAYER_POINTS);
    await settle();

    expect(points.value()).toEqual(PLAYER_POINTS);
  });
});
