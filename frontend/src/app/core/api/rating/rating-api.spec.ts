import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { type MatchRatingsResponse, RatingApi, type RatingWindowResponse } from './rating-api';

const LEAGUE_ID = '3f6c2a1e';

const WINDOW: RatingWindowResponse = {
  matchId: 'm1',
  matchday: 11,
  kickoffAt: '2026-10-26T10:00:00Z',
  closesAt: '2026-10-28T13:00:00Z',
  status: 'OPEN',
  players: 9,
  myRatings: 3,
  excluded: false,
};

const RATINGS: MatchRatingsResponse = {
  matchId: 'm1',
  matchday: 11,
  kickoffAt: '2026-10-26T10:00:00Z',
  closesAt: '2026-10-28T13:00:00Z',
  status: 'OPEN',
  format: 'SPADES',
  excluded: false,
  excludedUserIds: null,
  players: [
    {
      playerId: 'p1',
      position: 'GOALKEEPER',
      myRating: 3,
      average: null,
      votes: null,
      ratingPoints: null,
    },
  ],
};

describe('RatingApi', () => {
  let api: RatingApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RatingApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it('reads the rating windows once the league is known', async () => {
    const leagueId = signal<string | undefined>(undefined);
    const windows = TestBed.runInInjectionContext(() => api.windows(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/ratings` }).flush([WINDOW]);
    await settle();

    expect(windows.value()).toEqual([WINDOW]);
  });

  it('reads the ratings of a match, with the given context', async () => {
    const token = new HttpContextToken(() => false);
    const matchId = signal<string | undefined>(undefined);
    const ratings = TestBed.runInInjectionContext(() =>
      api.ratings(signal(LEAGUE_ID), matchId, new HttpContext().set(token, true)),
    );

    TestBed.tick();
    http.expectNone(() => true);

    matchId.set('m1');
    TestBed.tick();
    const request = http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/ratings/m1` });
    expect(request.request.context.get(token)).toBe(true);
    request.flush(RATINGS);
    await settle();

    expect(ratings.value()).toEqual(RATINGS);
  });

  it("saves the user's ratings of a match", async () => {
    const body = { ratings: [{ playerId: 'p1', value: 3 }] };
    const result = firstValueFrom(api.save(LEAGUE_ID, 'm1', body));

    const request = http.expectOne({ method: 'PUT', url: `/api/leagues/${LEAGUE_ID}/ratings/m1` });
    expect(request.request.body).toEqual(body);
    request.flush(RATINGS);

    expect(await result).toEqual(RATINGS);
  });
});
