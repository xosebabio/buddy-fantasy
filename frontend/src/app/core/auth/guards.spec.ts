import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding, withRouterConfig } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import type { UserResponse } from '../api/identity/identity-api';
import { adminGuard, authGuard, guestGuard, memberGuard } from './guards';
import { homeRedirect } from './home-redirect';
import { LastLeague } from './last-league';
import { ReturnUrl } from './return-url';
import { answerSession, USER } from './session.testing';
import { SessionStore } from './session-store';

/** Shows the name in its route's data, to tell which page the router reached. */
@Component({
  selector: 'bf-test-page',
  template: '<h1>{{ name() }}</h1>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class Page {
  readonly name = input.required<string>();
}

function page(name: string) {
  return { component: Page, data: { name } };
}

const routes = [
  { path: '', pathMatch: 'full' as const, redirectTo: homeRedirect },
  { path: 'login', canActivate: [guestGuard], ...page('login') },
  { path: 'leagues', canActivate: [authGuard], ...page('leagues') },
  {
    path: 'leagues/:leagueId',
    canActivate: [authGuard, memberGuard],
    children: [
      { path: '', ...page('league') },
      { path: 'admin', canActivate: [adminGuard], ...page('admin') },
    ],
  },
  { path: 'invite/:token', ...page('invitation') },
  { path: 'forbidden', ...page('forbidden') },
  { path: 'not-found', ...page('not-found') },
];

describe('guards', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(
          routes,
          withComponentInputBinding(),
          withRouterConfig({ paramsInheritanceStrategy: 'always' }),
        ),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  /** Navigates with this session, and gives back the page shown and the URL in the browser. */
  async function visit(url: string, user: UserResponse | null = USER) {
    const loaded = TestBed.inject(SessionStore).load();
    answerSession(http, user);
    await loaded;
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return {
      page: harness.routeNativeElement?.querySelector('h1')?.textContent,
      url: TestBed.inject(Location).path(),
    };
  }

  describe('authGuard', () => {
    it('lets a user with a session in', async () => {
      expect(await visit('/leagues')).toEqual({ page: 'leagues', url: '/leagues' });
    });

    it('sends to the login without a session, saving the route', async () => {
      expect(await visit('/leagues/l1?tab=2', null)).toEqual({ page: 'login', url: '/login' });
      expect(TestBed.inject(ReturnUrl).consume()).toBe('/leagues/l1?tab=2');
    });
  });

  describe('guestGuard', () => {
    it('shows the login without a session', async () => {
      expect(await visit('/login', null)).toEqual({ page: 'login', url: '/login' });
    });

    it('takes a user with a session to the start', async () => {
      expect(await visit('/login')).toEqual({ page: 'leagues', url: '/leagues' });
    });
  });

  describe('memberGuard', () => {
    it('lets a member of the league in', async () => {
      expect(await visit('/leagues/l2')).toEqual({ page: 'league', url: '/leagues/l2' });
    });

    it('shows «No encontrado» for a league that is not the user’s, keeping the URL', async () => {
      expect(await visit('/leagues/other')).toEqual({ page: 'not-found', url: '/leagues/other' });
    });
  });

  describe('adminGuard', () => {
    it('lets the admin of the league in', async () => {
      expect(await visit('/leagues/l1/admin')).toEqual({ page: 'admin', url: '/leagues/l1/admin' });
    });

    it('shows «Sin acceso» to a manager, keeping the URL', async () => {
      expect(await visit('/leagues/l2/admin')).toEqual({
        page: 'forbidden',
        url: '/leagues/l2/admin',
      });
    });
  });

  describe('homeRedirect', () => {
    it('goes to the login without a session', async () => {
      expect(await visit('/', null)).toEqual({ page: 'login', url: '/login' });
    });

    it('goes back to the route saved before the login, once', async () => {
      TestBed.inject(ReturnUrl).save('/invite/tok?x=1');

      expect(await visit('/')).toEqual({ page: 'invitation', url: '/invite/tok?x=1' });
      expect(TestBed.inject(ReturnUrl).consume()).toBeNull();
    });

    it('goes to the last league used while the user is still in it', async () => {
      TestBed.inject(LastLeague).save('l2');

      expect(await visit('/')).toEqual({ page: 'league', url: '/leagues/l2' });
    });

    it('goes to the user’s leagues when the last one is no longer theirs', async () => {
      TestBed.inject(LastLeague).save('gone');

      expect(await visit('/')).toEqual({ page: 'leagues', url: '/leagues' });
    });

    it('goes to the user’s leagues without a last one', async () => {
      expect(await visit('/')).toEqual({ page: 'leagues', url: '/leagues' });
    });
  });
});
