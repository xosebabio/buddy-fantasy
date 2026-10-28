import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import {
  type CreateLeagueResponse,
  type InvitationPreviewResponse,
  type InvitationTokenResponse,
  type JoinLeagueResponse,
  LeagueApi,
  type LeagueResponse,
  type LeagueSettingsBody,
  type MemberResponse,
  type MyLeagueResponse,
} from './league-api';

const LEAGUE_ID = '3f6c2a1e';

const LEAGUE: LeagueResponse = {
  id: LEAGUE_ID,
  name: 'Peña Os Amigos',
  role: 'ADMIN',
  createdAt: '2026-01-11T10:00:00Z',
};

const MEMBER: MemberResponse = {
  userId: 'u2',
  displayName: 'Iago',
  avatarUrl: null,
  role: 'MANAGER',
  joinedAt: '2026-01-12T10:00:00Z',
};

const SETTINGS: LeagueSettingsBody = {
  ratingWindowHours: 48,
  ratingFormat: 'SPADES',
  ratingSpadesFactor: 1,
  ratingSpadesBase: 0,
  ratingDecimalFactor: 1,
  ratingDecimalBase: 0,
  ratingMinVotes: 3,
  defaultRating: null,
  lineupLockHours: 1,
  lineupMaxChanges: 2,
  lineupSize: 7,
  lineupFormationEnabled: true,
  lineupMinGoalkeepers: 1,
  lineupMinDefenders: 2,
  lineupMinMidfielders: 2,
  lineupMinForwards: 1,
  lineupBudgetEnabled: false,
  lineupBudget: 100,
  marketK: 0.1,
  marketMaxChangePct: 10,
  marketMinValue: 1,
  scoringRules: { GOAL: { FORWARD: 4 } },
};

describe('LeagueApi', () => {
  let api: LeagueApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(LeagueApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function settle(): Promise<void> {
    await TestBed.inject(ApplicationRef).whenStable();
  }

  describe('observable endpoints', () => {
    it("gets the user's leagues", async () => {
      const leagues: MyLeagueResponse[] = [{ leagueId: LEAGUE_ID, name: 'Peña', role: 'ADMIN' }];
      const result = firstValueFrom(api.myLeagues());

      http.expectOne({ method: 'GET', url: '/api/me/leagues' }).flush(leagues);

      expect(await result).toEqual(leagues);
    });

    it('creates a league', async () => {
      const created: CreateLeagueResponse = { id: LEAGUE_ID, invitationToken: 'tok' };
      const result = firstValueFrom(api.create({ name: 'Peña' }));

      const request = http.expectOne({ method: 'POST', url: '/api/leagues' });
      expect(request.request.body).toEqual({ name: 'Peña' });
      request.flush(created, { status: 201, statusText: 'Created' });

      expect(await result).toEqual(created);
    });

    it('regenerates the invitation', async () => {
      const invitation: InvitationTokenResponse = { token: 'tok' };
      const result = firstValueFrom(api.regenerateInvitation(LEAGUE_ID));

      http
        .expectOne({ method: 'POST', url: `/api/leagues/${LEAGUE_ID}/invitation` })
        .flush(invitation);

      expect(await result).toEqual(invitation);
    });

    it('revokes the invitation', async () => {
      const done = firstValueFrom(api.revokeInvitation(LEAGUE_ID));

      http
        .expectOne({ method: 'DELETE', url: `/api/leagues/${LEAGUE_ID}/invitation` })
        .flush(null, { status: 204, statusText: 'No Content' });

      await expect(done).resolves.toBeNull();
    });

    it('joins with an invitation, with the given context', async () => {
      const token = new HttpContextToken(() => false);
      const joined: JoinLeagueResponse = { leagueId: LEAGUE_ID };
      const result = firstValueFrom(api.join('a/b', new HttpContext().set(token, true)));

      const request = http.expectOne({ method: 'POST', url: '/api/invitations/a%2Fb/join' });
      expect(request.request.context.get(token)).toBe(true);
      request.flush(joined);

      expect(await result).toEqual(joined);
    });

    it('leaves the league', async () => {
      const done = firstValueFrom(api.leave(LEAGUE_ID));

      http
        .expectOne({ method: 'DELETE', url: `/api/leagues/${LEAGUE_ID}/members/me` })
        .flush(null, { status: 204, statusText: 'No Content' });

      await expect(done).resolves.toBeNull();
    });

    it('removes a member', async () => {
      const done = firstValueFrom(api.removeMember(LEAGUE_ID, 'u2'));

      http
        .expectOne({ method: 'DELETE', url: `/api/leagues/${LEAGUE_ID}/members/u2` })
        .flush(null, { status: 204, statusText: 'No Content' });

      await expect(done).resolves.toBeNull();
    });

    it('updates the settings', async () => {
      const result = firstValueFrom(api.updateSettings(LEAGUE_ID, SETTINGS));

      const request = http.expectOne({ method: 'PUT', url: `/api/leagues/${LEAGUE_ID}/settings` });
      expect(request.request.body).toEqual(SETTINGS);
      request.flush(SETTINGS);

      expect(await result).toEqual(SETTINGS);
    });
  });

  describe('resources', () => {
    it('stays idle while the league is unknown, and reads it once it is set', async () => {
      const leagueId = signal<string | undefined>(undefined);
      const league = TestBed.runInInjectionContext(() => api.league(leagueId));

      TestBed.tick();
      http.expectNone(() => true);

      leagueId.set(LEAGUE_ID);
      TestBed.tick();
      http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}` }).flush(LEAGUE);
      await settle();

      expect(league.value()).toEqual(LEAGUE);
    });

    it('reads the invitation of a token, with the given context', async () => {
      const token = new HttpContextToken(() => false);
      const preview: InvitationPreviewResponse = { leagueName: 'Peña', alreadyMember: false };
      const invitation = TestBed.runInInjectionContext(() =>
        api.invitation(signal('tok'), new HttpContext().set(token, true)),
      );

      TestBed.tick();
      const request = http.expectOne({ method: 'GET', url: '/api/invitations/tok' });
      expect(request.request.context.get(token)).toBe(true);
      request.flush(preview);
      await settle();

      expect(invitation.value()).toEqual(preview);
    });

    it('reads the members', async () => {
      const members = TestBed.runInInjectionContext(() => api.members(signal(LEAGUE_ID)));

      TestBed.tick();
      http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/members` }).flush([MEMBER]);
      await settle();

      expect(members.value()).toEqual([MEMBER]);
    });

    it('reads the settings', async () => {
      const settings = TestBed.runInInjectionContext(() => api.settings(signal(LEAGUE_ID)));

      TestBed.tick();
      http.expectOne({ method: 'GET', url: `/api/leagues/${LEAGUE_ID}/settings` }).flush(SETTINGS);
      await settle();

      expect(settings.value()).toEqual(SETTINGS);
    });
  });
});
