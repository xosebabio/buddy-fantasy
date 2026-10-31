import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import {
  MatchApi,
  type MatchAuditEntryResponse,
  type MatchRequest,
  type MatchResponse,
  type MatchSheetRequest,
  type MatchSummaryResponse,
} from './match-api';

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

const REQUEST: MatchRequest = {
  matchday: 11,
  opponent: 'CD Arrabal',
  venue: 'HOME',
  kickoffAt: '2026-10-26T10:00:00Z',
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

  it('reads the limits of the match forms once the league is known', async () => {
    const leagueId = signal<string | undefined>(undefined);
    const limits = TestBed.runInInjectionContext(() => api.limits(leagueId));

    TestBed.tick();
    http.expectNone(() => true);

    leagueId.set(LEAGUE_ID);
    TestBed.tick();
    const body = {
      opponentMaxLength: 60,
      matchdayMin: 1,
      goalsMax: 30,
      statCountMax: 20,
      sheetPlayersMax: 30,
    };
    http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/matches/limits` }).flush(body);
    await settle();

    expect(limits.value()).toEqual(body);
  });

  it('reads the audit of a match once the league and the match are known', async () => {
    const matchId = signal<string | undefined>(undefined);
    const audit = TestBed.runInInjectionContext(() => api.audit(signal(LEAGUE_ID), matchId));

    TestBed.tick();
    http.expectNone(() => true);

    const entry: MatchAuditEntryResponse = {
      action: 'SCHEDULED',
      actorUserId: 'u1',
      occurredAt: '2026-10-20T10:00:00Z',
      before: null,
      after: null,
    };
    matchId.set('m1');
    TestBed.tick();
    http
      .expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/matches/m1/audit` })
      .flush([entry]);
    await settle();

    expect(audit.value()).toEqual([entry]);
  });

  it('schedules a match', async () => {
    const result = firstValueFrom(api.schedule(LEAGUE_ID, REQUEST));

    const request = http.expectOne({ method: 'POST', url: `/api/leagues/${LEAGUE_ID}/matches` });
    expect(request.request.body).toEqual(REQUEST);
    request.flush(MATCH, { status: 201, statusText: 'Created' });

    expect(await result).toEqual(MATCH);
  });

  it('replaces the data of a match', async () => {
    const result = firstValueFrom(api.update(LEAGUE_ID, 'm1', REQUEST));

    const request = http.expectOne({ method: 'PUT', url: `/api/leagues/${LEAGUE_ID}/matches/m1` });
    expect(request.request.body).toEqual(REQUEST);
    request.flush(MATCH);

    expect(await result).toEqual(MATCH);
  });

  it('deletes a match', async () => {
    const result = firstValueFrom(api.delete(LEAGUE_ID, 'm1'));

    http
      .expectOne({ method: 'DELETE', url: `/api/leagues/${LEAGUE_ID}/matches/m1` })
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(await result).toBeNull();
  });

  it('records the sheet of a match', async () => {
    const body: MatchSheetRequest = {
      result: { goalsFor: 3, goalsAgainst: 1 },
      performances: [],
    };
    const result = firstValueFrom(api.recordSheet(LEAGUE_ID, 'm1', body));

    const request = http.expectOne({
      method: 'PUT',
      url: `/api/leagues/${LEAGUE_ID}/matches/m1/sheet`,
    });
    expect(request.request.body).toEqual(body);
    request.flush({ warnings: ['GOALS_MISMATCH'] });

    expect(await result).toEqual({ warnings: ['GOALS_MISMATCH'] });
  });

  it('closes a match', async () => {
    const result = firstValueFrom(api.close(LEAGUE_ID, 'm1'));

    const request = http.expectOne({
      method: 'POST',
      url: `/api/leagues/${LEAGUE_ID}/matches/m1/close`,
    });
    expect(request.request.body).toBeNull();
    request.flush(MATCH);

    expect(await result).toEqual(MATCH);
  });
});
