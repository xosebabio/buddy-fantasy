import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import type { UserResponse } from '../api/identity/identity-api';
import type { MyLeagueResponse } from '../api/league/league-api';
import { SessionStore } from './session-store';

/** Test data and helpers for what depends on the session; only the tests import this file. */

export const USER: UserResponse = {
  id: 'u1',
  displayName: 'Xosé Babío',
  email: 'xose@example.com',
  avatarUrl: null,
};

export const LEAGUES: MyLeagueResponse[] = [
  { leagueId: 'l1', name: 'Os Domingueiros', role: 'ADMIN' },
  { leagueId: 'l2', name: 'Veteranos do Sar', role: 'MANAGER' },
];

/** Answers the requests of `SessionStore.load`: with this user and leagues, or a `401`. */
export function answerSession(
  http: HttpTestingController,
  user: UserResponse | null,
  leagues: MyLeagueResponse[] = LEAGUES,
): void {
  if (user === null) {
    http.expectOne('/api/me').flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });
    http.match('/api/me/leagues');
    return;
  }
  http.expectOne('/api/me').flush(user);
  http.expectOne('/api/me/leagues').flush(leagues);
}

/** Loads the session of the test's `TestBed` with this user and leagues, as the app does on start. */
export async function loadSession(
  user: UserResponse | null = USER,
  leagues: MyLeagueResponse[] = LEAGUES,
): Promise<void> {
  const loaded = TestBed.inject(SessionStore).load();
  answerSession(TestBed.inject(HttpTestingController), user, leagues);
  await loaded;
}
