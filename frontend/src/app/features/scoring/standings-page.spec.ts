import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import type { StandingResponse } from '../../core/api/scoring/scoring-api';
import { loadSession } from '../../core/auth/session.testing';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../shared/roster/matches.testing';
import { MEMBERS } from '../../shared/roster/members.testing';
import { StandingsPage, standingRows } from './standings-page';

function standing(overrides: Partial<StandingResponse>): StandingResponse {
  return {
    userId: 'u1',
    position: 1,
    points: 342,
    matchdays: 11,
    lastMatchdayPoints: 41,
    ...overrides,
  };
}

describe('standingRows', () => {
  it('names the managers, marks a repeated position as a tie and calls a user who left a former member', () => {
    const rows = standingRows(
      [
        standing({ userId: 'u2', position: 1 }),
        standing({ userId: 'u1', position: 1 }),
        standing({ userId: 'gone', position: 3 }),
      ],
      MEMBERS,
    );

    expect(rows.map((row) => row.name)).toEqual(['Brais Otero', 'Xosé Babío', 'Antiguo miembro']);
    expect(rows.map((row) => row.tied)).toEqual([false, true, false]);
  });
});

describe('StandingsPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(standings: StandingResponse[] | null) {
    const result = await render(StandingsPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    await loadSession();
    http.expectOne('/api/leagues/l2/matches').flush([CLOSED_SUMMARY, SCHEDULED_SUMMARY]);
    http.expectOne('/api/leagues/l2/members').flush(MEMBERS);
    if (standings === null) {
      http.expectOne('/api/leagues/l2/standings').flush(null, { status: 500, statusText: 'Error' });
    } else {
      http.expectOne('/api/leagues/l2/standings').flush(standings);
    }
    await result.fixture.whenStable();
    return result;
  }

  it('lists the managers with their total, matchdays and last matchday, and marks the user', async () => {
    await renderPage([
      standing({ userId: 'u2', position: 1, points: 342, matchdays: 11, lastMatchdayPoints: 41 }),
      standing({ userId: 'u1', position: 2, points: 318, matchdays: 1, lastMatchdayPoints: -3 }),
    ]);

    const links = screen.getAllByRole('link', { name: /Brais|Xosé/ });
    expect(links[0]).toHaveAttribute('href', '/leagues/l2/managers/u2');
    expect(links[0]).toHaveTextContent('342');
    expect(links[0]).toHaveTextContent('11 jornadas, +41 en la última');
    expect(links[1]).toHaveTextContent('1 jornada, −3 en la última');
    expect(links[1]).toHaveAttribute('aria-current', 'true');
    expect(links[0]).not.toHaveAttribute('aria-current');
  });

  it('opens the latest closed matchday from «Por jornada»', async () => {
    await renderPage([standing({})]);

    expect(screen.getByRole('link', { name: 'Por jornada' })).toHaveAttribute(
      'href',
      '/leagues/l2/standings/m11',
    );
  });

  it('says that the standings start with the first scored matchday', async () => {
    await renderPage([standing({ matchdays: 0, points: 0, lastMatchdayPoints: 0 })]);

    expect(screen.getByText(/empieza cuando se puntúe la primera jornada/)).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage(null);

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
