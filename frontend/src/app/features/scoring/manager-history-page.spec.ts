import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { ManagerMatchdayResponse } from '../../core/api/scoring/scoring-api';
import { MEMBERS } from '../../shared/roster/members.testing';
import { PLAYERS } from '../../shared/roster/players.testing';
import { ManagerHistoryPage } from './manager-history-page';

function matchday(overrides: Partial<ManagerMatchdayResponse>): ManagerMatchdayResponse {
  return {
    matchId: 'm11',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    inStandings: true,
    overBudget: false,
    points: 14,
    players: [
      {
        id: 'p9',
        position: 'FORWARD',
        played: true,
        actions: { GOAL: 3, ASSIST: 1 },
        actionPoints: 4,
        ratingPoints: 10,
        points: 14,
      },
      {
        id: 'p7',
        position: null,
        played: false,
        actions: {},
        actionPoints: 0,
        ratingPoints: 0,
        points: 0,
      },
    ],
    ...overrides,
  };
}

describe('ManagerHistoryPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(history: ManagerMatchdayResponse[] | null, userId = 'u2') {
    const result = await render(ManagerHistoryPage, {
      inputs: { leagueId: 'l2', userId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    http.expectOne('/api/leagues/l2/members').flush(MEMBERS);
    if (history === null) {
      http
        .expectOne(`/api/leagues/l2/managers/${userId}/history`)
        .flush(null, { status: 500, statusText: 'Error' });
    } else {
      http.expectOne(`/api/leagues/l2/managers/${userId}/history`).flush(history);
    }
    await result.fixture.whenStable();
    return result;
  }

  it('lists the matchdays from the latest, named after the manager', async () => {
    await renderPage([matchday({ matchId: 'm10', matchday: 10 }), matchday({})]);

    expect(screen.getByRole('heading', { name: 'Brais Otero' })).toBeInTheDocument();
    const rows = screen.getAllByRole('button', { expanded: false });
    expect(rows[0]).toHaveTextContent('Jornada 11');
    expect(rows[1]).toHaveTextContent('Jornada 10');
  });

  it('opens a matchday to show where the points of each player come from', async () => {
    await renderPage([matchday({})]);

    await userEvent.click(screen.getByRole('button', { name: /Jornada 11/ }));
    await userEvent.click(screen.getByRole('button', { name: /Pablo/ }));

    const terms = screen.getAllByRole('term').map((term) => term.textContent);
    expect(terms).toEqual(['Gol', 'Asistencia', 'Valoración', 'Total']);
    expect(screen.getByText('No jugó')).toBeInTheDocument();
  });

  it('marks the matchdays before the last join and the ones over the budget', async () => {
    await renderPage([matchday({ inStandings: false, overBudget: true })]);

    expect(screen.getByText('Antes de su última alta')).toBeInTheDocument();
    expect(screen.getByText('Fuera de presupuesto')).toBeInTheDocument();
  });

  it('names a user who left the league a former member', async () => {
    await renderPage([matchday({})], 'gone');

    expect(screen.getByRole('heading', { name: 'Antiguo miembro' })).toBeInTheDocument();
  });

  it('says that there are no scored matchdays yet', async () => {
    await renderPage([]);

    expect(screen.getByText('Aún no hay jornadas puntuadas.')).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage(null);

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
