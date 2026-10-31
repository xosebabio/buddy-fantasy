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

  it('in a league, navigates between the destinations that exist', async () => {
    const { container } = await renderAt('/leagues/l1/members');

    const bar = container.querySelector('bf-nav-bar');
    expect(bar).not.toBeNull();
    const nav = within(bar instanceof HTMLElement ? bar : document.body);
    const links = nav.getAllByRole('link');
    expect(links.map((link) => link.textContent.trim())).toEqual(['Jornada', 'Liga']);
    expect(nav.getByRole('link', { name: 'Jornada' })).toHaveAttribute('href', '/leagues/l1');
    expect(nav.getByRole('link', { name: 'Jornada' })).not.toHaveAttribute('aria-current');
    expect(nav.getByRole('link', { name: 'Liga' })).toHaveAttribute('aria-current', 'page');
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
