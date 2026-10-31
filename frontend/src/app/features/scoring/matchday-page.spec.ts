import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchdayScoresResponse } from '../../core/api/scoring/scoring-api';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../shared/roster/matches.testing';
import { MEMBERS } from '../../shared/roster/members.testing';
import { PLAYERS } from '../../shared/roster/players.testing';
import { MatchdayPage, managerRows, sheetRows } from './matchday-page';
import { playerIndex } from '../../shared/roster/roster';

const OLDER_SUMMARY = { ...CLOSED_SUMMARY, id: 'm10', matchday: 10 };

const SCORES: MatchdayScoresResponse = {
  matchId: 'm11',
  matchday: 11,
  kickoffAt: '2026-10-26T10:00:00Z',
  scoredAt: '2026-10-28T12:00:00Z',
  standings: [
    { userId: 'u2', position: 1, points: 24, overBudget: false, playerIds: ['p9', 'p7'] },
    { userId: 'gone', position: 2, points: 0, overBudget: true, playerIds: ['p1'] },
    { userId: 'u1', position: 2, points: 0, overBudget: false, playerIds: [] },
  ],
  players: [
    { id: 'p9', position: 'FORWARD', actionPoints: 4, ratingPoints: 10, points: 14 },
    { id: 'p1', position: 'GOALKEEPER', actionPoints: 3, ratingPoints: 2, points: 5 },
  ],
};

function fail(): never {
  throw new Error('Expected the element to exist');
}

describe('managerRows and sheetRows', () => {
  const squad = playerIndex(PLAYERS);

  it('crosses each lineup with the sheet, leaving the points of a player who did not play out', () => {
    const rows = managerRows(SCORES.standings, SCORES.players, squad, MEMBERS);

    expect(rows.map((row) => row.name)).toEqual(['Brais Otero', 'Antiguo miembro', 'Xosé Babío']);
    expect(rows[0]?.lineup.map((entry) => [entry.player.id, entry.sheet?.points])).toEqual([
      ['p9', 14],
      ['p7', undefined],
    ]);
  });

  it('gives each player of the sheet the position they played in', () => {
    const rows = sheetRows(
      [{ id: 'p9', position: 'MIDFIELDER', actionPoints: 4, ratingPoints: 10, points: 14 }],
      squad,
    );

    expect(rows[0]?.player.position).toBe('MIDFIELDER');
    expect(rows[0]?.player.name).toBe('Pablo');
  });
});

describe('MatchdayPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(scores: (http: HttpTestingController) => void) {
    const result = await render(MatchdayPage, {
      inputs: { leagueId: 'l2', matchId: 'm11' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l2/matches')
      .flush([CLOSED_SUMMARY, OLDER_SUMMARY, SCHEDULED_SUMMARY]);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    http.expectOne('/api/leagues/l2/members').flush(MEMBERS);
    scores(http);
    await result.fixture.whenStable();
    return result;
  }

  const answer = (h: HttpTestingController) => {
    h.expectOne('/api/leagues/l2/standings/m11').flush(SCORES);
  };

  it('opens a manager to show the points of each player of their lineup', async () => {
    await renderPage(answer);

    await userEvent.click(screen.getByRole('button', { name: /Brais Otero/ }));

    const panel = screen.getByRole('button', { name: /Brais Otero/ }).closest('li') ?? fail();
    expect(panel).toHaveTextContent('Pablo');
    expect(panel).toHaveTextContent('Acciones +4, valoración +10');
    expect(panel).toHaveTextContent('+14');
    expect(within(panel).getByRole('link', { name: 'Ver su historial' })).toHaveAttribute(
      'href',
      '/leagues/l2/managers/u2',
    );
  });

  it('marks a lineup over the budget', async () => {
    await renderPage(answer);

    expect(screen.getByText('Fuera de presupuesto')).toBeInTheDocument();
  });

  it('lists the players of the sheet, each linking to their sheet', async () => {
    await renderPage(answer);

    await userEvent.click(screen.getByRole('tab', { name: 'Jugadores' }));

    expect(screen.getByRole('link', { name: /Pablo/ })).toHaveAttribute(
      'href',
      '/leagues/l2/squad/p9',
    );
  });

  it('changes of matchday with the selector, which only lists closed matches', async () => {
    await renderPage(answer);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    const select = screen.getByRole('combobox', { name: 'Jornada' });
    expect(
      within(select)
        .getAllByRole('option')
        .map((option) => option.textContent.trim()),
    ).toEqual(['Jornada 11, contra Racing Vilariño', 'Jornada 10, contra Racing Vilariño']);
    await userEvent.selectOptions(select, 'm10');

    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l2', 'standings', 'm10']);
  });

  it('explains that the points come when the ratings close, with a way to rate while they are open', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/standings/m11').flush(
        { status: 409, code: 'MATCH_NOT_SCORED' },
        { status: 409, statusText: 'Conflict' },
      );
    });

    expect(screen.getByText(/se publican al cerrar las valoraciones/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Valorar la jornada 11' })).toHaveAttribute(
      'href',
      '/leagues/l2/ratings/m11',
    );
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/standings/m11').flush(null, {
        status: 500,
        statusText: 'Error',
      });
    });

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
