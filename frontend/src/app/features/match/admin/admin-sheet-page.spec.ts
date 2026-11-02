import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchLimitsResponse, MatchResponse } from '../../../core/api/match/match-api';
import { CLOSED_MATCH, SCHEDULED_MATCH } from '../../../shared/roster/matches.testing';
import { PLAYERS } from '../../../shared/roster/players.testing';
import { Toasts } from '../../../ui/toast/toasts';
import { AdminSheetPage } from './admin-sheet-page';

const LIMITS: MatchLimitsResponse = {
  opponentMaxLength: 40,
  matchdayMin: 1,
  goalsMax: 30,
  statCountMax: 20,
  sheetPlayersMax: 2,
};

function nth(elements: HTMLElement[], index: number): HTMLElement {
  const element = elements[index];
  if (element === undefined) {
    throw new Error(`No element at ${String(index)}`);
  }
  return element;
}

describe('AdminSheetPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderPage(match: MatchResponse) {
    const result = await render(AdminSheetPage, {
      inputs: { leagueId: 'l1', matchId: match.id },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/leagues/l1/matches/${match.id}`).flush(match);
    http.expectOne('/api/leagues/l1/matches/limits').flush(LIMITS);
    http.expectOne('/api/leagues/l1/players?includeRetired=true').flush(PLAYERS);
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  it('lists the squad by position with a box for who played and the count', async () => {
    await renderPage(SCHEDULED_MATCH);

    expect(screen.getByRole('heading', { level: 1, name: 'Acta de la jornada 12' })).toBeVisible();
    expect(screen.getByText('0 de 2 jugadores')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Porteros' })).toBeInTheDocument();
    expect(screen.getAllByRole('checkbox', { name: 'Jugó' })).toHaveLength(3);
    expect(screen.getByRole('button', { name: 'Guardar acta' })).toBeEnabled();
  });

  it('opens the counters of who is marked and records the sheet', async () => {
    await renderPage(SCHEDULED_MATCH);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await userEvent.click(nth(screen.getAllByRole('checkbox', { name: 'Jugó' }), 0));
    expect(screen.getByRole('spinbutton', { name: 'Goles · Diego' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Amarilla' }));
    // The first step from the empty mark goes to 0.
    await userEvent.click(nth(screen.getAllByRole('button', { name: 'Subir' }), 0));
    await userEvent.click(nth(screen.getAllByRole('button', { name: 'Subir' }), 0));
    await userEvent.click(nth(screen.getAllByRole('button', { name: 'Subir' }), 1));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar acta' }));

    const request = http.expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m12/sheet' });
    expect(request.request.body).toEqual({
      result: { goalsFor: 1, goalsAgainst: 0 },
      performances: [expect.objectContaining({ playerId: 'p1', yellowCard: true })],
    });
    request.flush({ warnings: [] });
    expect(toasts()).toEqual(['Acta guardada']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'matches', 'm12']);
  });

  it('disables the unchecked boxes when the sheet is full', async () => {
    await renderPage(SCHEDULED_MATCH);

    const boxes = screen.getAllByRole('checkbox', { name: 'Jugó' });
    await userEvent.click(nth(boxes, 0));
    await userEvent.click(nth(boxes, 1));

    expect(screen.getByText('2 de 2 jugadores')).toBeInTheDocument();
    expect(nth(screen.getAllByRole('checkbox', { name: 'Jugó' }), 2)).toBeDisabled();
  });

  it('asks for the missing mark instead of sending only one', async () => {
    await renderPage(SCHEDULED_MATCH);

    await userEvent.click(nth(screen.getAllByRole('button', { name: 'Subir' }), 0));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar acta' }));

    expect(screen.getByText('Falta el marcador en contra.')).toBeInTheDocument();
    http.expectNone({ method: 'PUT', url: '/api/leagues/l1/matches/m12/sheet' });
  });

  it('disables the other cards when the double yellow is checked', async () => {
    await renderPage(SCHEDULED_MATCH);

    await userEvent.click(nth(screen.getAllByRole('checkbox', { name: 'Jugó' }), 0));
    await userEvent.click(screen.getByRole('checkbox', { name: 'Doble amarilla' }));

    expect(screen.getByRole('checkbox', { name: 'Amarilla' })).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'Roja directa' })).toBeDisabled();
  });

  it('paints INVALID_GOALS in the result and the other codes in the notice', async () => {
    await renderPage(SCHEDULED_MATCH);

    await userEvent.click(screen.getByRole('button', { name: 'Guardar acta' }));
    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m12/sheet' })
      .flush({ status: 400, code: 'INVALID_GOALS' }, { status: 400, statusText: 'Bad Request' });
    expect(await screen.findByText('El número de goles no es válido.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar acta' }));
    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m12/sheet' })
      .flush({ status: 400, code: 'INVALID_CARDS' }, { status: 400, statusText: 'Bad Request' });
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Un jugador con doble amarilla no puede tener también la amarilla ni la roja directa.',
    );
  });

  it('corrects a closed match and names who leaves the sheet before saving', async () => {
    await renderPage(CLOSED_MATCH);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(screen.getByText(/guardar corrige el acta y recalcula los puntos/)).toBeInTheDocument();
    await userEvent.click(nth(screen.getAllByRole('checkbox', { name: 'Jugó' }), 0));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar corrección' }));
    expect(await screen.findByText('Diego sale del acta y pierde sus valoraciones.')).toBeVisible();
    http.expectNone({ method: 'PUT', url: '/api/leagues/l1/matches/m11/sheet' });
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Guardar corrección' }),
    );

    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m11/sheet' })
      .flush({ warnings: [] });
    expect(toasts()).toEqual(['Corrección guardada']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'matches', 'm11']);
  });

  it('offers to try again when it fails to load', async () => {
    const result = await render(AdminSheetPage, {
      inputs: { leagueId: 'l1', matchId: 'm12' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l1/matches/m12')
      .flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/leagues/l1/matches/limits').flush(LIMITS);
    http.expectOne('/api/leagues/l1/players?includeRetired=true').flush(PLAYERS);
    await result.fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
