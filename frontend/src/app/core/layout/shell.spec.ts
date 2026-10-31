import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  provideRouter,
  Router,
  RouterOutlet,
  withComponentInputBinding,
  withRouterConfig,
} from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import type { UserResponse } from '../api/identity/identity-api';
import { LastLeague } from '../auth/last-league';
import { answerSession, USER } from '../auth/session.testing';
import { SessionStore } from '../auth/session-store';
import { Shell } from './shell';

@Component({
  selector: 'bf-test-page',
  template: '<h1>Página</h1>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class Page {}

const routes = [
  {
    path: 'leagues/:leagueId',
    component: Shell,
    children: [
      { path: '', component: Page },
      { path: 'lineup', component: Page },
      { path: 'lineups/:matchId', component: Page },
      { path: 'ratings', component: Page },
      { path: 'standings', component: Page },
      { path: 'league', component: Page },
      { path: 'squad/:playerId', component: Page },
      { path: 'members', component: Page },
    ],
  },
  { path: '', component: Shell, children: [{ path: 'leagues', component: Page }] },
];

describe('Shell', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  async function renderAt(url: string, user: UserResponse | null = USER) {
    const result = await render('<router-outlet />', {
      imports: [RouterOutlet],
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
    const loaded = TestBed.inject(SessionStore).load();
    answerSession(TestBed.inject(HttpTestingController), user);
    await loaded;
    await TestBed.inject(Router).navigateByUrl(url);
    await result.fixture.whenStable();
    return result;
  }

  it('shows the page in the main region', async () => {
    await renderAt('/leagues/l1');

    expect(
      within(screen.getByRole('main')).getByRole('heading', { name: 'Página' }),
    ).toBeInTheDocument();
  });

  it('in a league, has its switcher and the profile in the top bar', async () => {
    await renderAt('/leagues/l1');

    const bar = screen.getByRole('banner');
    expect(
      within(bar).getByRole('button', { name: 'Os Domingueiros, cambiar de liga' }),
    ).toBeInTheDocument();
    expect(within(bar).getByRole('link', { name: 'Tu perfil' })).toHaveAttribute(
      'href',
      '/profile',
    );
  });

  async function navBar(url: string) {
    const { container } = await renderAt(url);
    const bar = container.querySelector('bf-nav-bar');
    expect(bar).not.toBeNull();
    return within(bar instanceof HTMLElement ? bar : document.body);
  }

  it('in a league, has the five destinations', async () => {
    const nav = await navBar('/leagues/l1');

    expect(nav.getAllByRole('link').map((link) => link.textContent.trim())).toEqual([
      'Jornada',
      'Alineación',
      'Valorar',
      'Clasificación',
      'Liga',
    ]);
    expect(nav.getAllByRole('link').map((link) => link.getAttribute('href'))).toEqual([
      '/leagues/l1',
      '/leagues/l1/lineup',
      '/leagues/l1/ratings',
      '/leagues/l1/standings',
      '/leagues/l1/league',
    ]);
  });

  it.each([
    ['/leagues/l1', 'Jornada'],
    ['/leagues/l1/lineup', 'Alineación'],
    ['/leagues/l1/lineups/m1', 'Alineación'],
    ['/leagues/l1/ratings', 'Valorar'],
    ['/leagues/l1/standings', 'Clasificación'],
    ['/leagues/l1/league', 'Liga'],
    ['/leagues/l1/squad/p1', 'Liga'],
    ['/leagues/l1/members', 'Liga'],
  ])('at %s, marks only «%s» as the current destination', async (url, label) => {
    const nav = await navBar(url);

    expect(nav.getAllByRole('link', { current: 'page' })).toHaveLength(1);
    expect(nav.getByRole('link', { name: label })).toHaveAttribute('aria-current', 'page');
  });

  it('moves the current destination when navigating inside the league', async () => {
    const nav = await navBar('/leagues/l1/squad/p1');
    expect(nav.getByRole('link', { name: 'Liga' })).toHaveAttribute('aria-current', 'page');

    await TestBed.inject(Router).navigateByUrl('/leagues/l1/lineup');

    expect(await nav.findByRole('link', { current: 'page' })).toHaveTextContent('Alineación');
  });

  it('in a league, has the side navigation with the profile for large screens', async () => {
    const { container } = await renderAt('/leagues/l1');

    // Hidden below lg, and jsdom does not apply media queries.
    const side = container.querySelector('bf-side-nav');
    expect(side).not.toBeNull();
    const nav = within(side instanceof HTMLElement ? side : document.body);
    expect(nav.getByRole('link', { name: 'Jornada', hidden: true })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(nav.getByRole('link', { name: 'Xosé Babío', hidden: true })).toHaveAttribute(
      'href',
      '/profile',
    );
  });

  it('remembers the league as the last one used', async () => {
    await renderAt('/leagues/l2');

    expect(TestBed.inject(LastLeague).get()).toBe('l2');
  });

  it('outside a league, has the logotype leading to the start and no navigation', async () => {
    const { container } = await renderAt('/leagues');

    expect(screen.getByRole('link', { name: 'BuddyFantasy' })).toHaveAttribute('href', '/');
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(container.querySelector('bf-side-nav')).toBeNull();
    expect(TestBed.inject(LastLeague).get()).toBeNull();
  });

  it('without a session, has no profile', async () => {
    await renderAt('/leagues', null);

    expect(screen.queryByRole('link', { name: 'Tu perfil' })).not.toBeInTheDocument();
  });
});
