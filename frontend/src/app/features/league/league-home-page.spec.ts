import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import type { LeagueResponse } from '../../core/api/league/league-api';
import { LeagueHomePage } from './league-home-page';

describe('LeagueHomePage', () => {
  async function renderHome(league: LeagueResponse) {
    const result = await render(LeagueHomePage, {
      inputs: { leagueId: league.id },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    http.expectOne(`/api/leagues/${league.id}`).flush(league);
    await result.fixture.whenStable();
    return result;
  }

  const league: LeagueResponse = {
    id: 'l1',
    name: 'Os Domingueiros',
    role: 'ADMIN',
    createdAt: '2026-01-11T10:00:00Z',
  };

  it('shows the league, still without a matchday, and invites its admin to bring the team', async () => {
    await renderHome(league);

    expect(screen.getByRole('heading', { level: 1, name: 'Os Domingueiros' })).toBeInTheDocument();
    expect(screen.getByText('Creada el 11 de enero de 2026')).toBeInTheDocument();
    expect(screen.getByText(/Aquí verás la jornada/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Invitar a tu equipo' })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/members',
    );
  });

  it('does not invite a manager', async () => {
    await renderHome({ ...league, id: 'l2', role: 'MANAGER' });

    expect(screen.queryByRole('link', { name: 'Invitar a tu equipo' })).not.toBeInTheDocument();
  });

  it('offers to retry when the league cannot be loaded', async () => {
    const { fixture } = await render(LeagueHomePage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/leagues/l1').flush(null, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();

    screen.getByRole('button', { name: 'Reintentar' }).click();
    TestBed.tick();
    http.expectOne('/api/leagues/l1').flush(league);
    await fixture.whenStable();

    expect(screen.getByRole('heading', { level: 1, name: 'Os Domingueiros' })).toBeInTheDocument();
  });
});
