import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchResponse, MatchSummaryResponse } from '../../../core/api/match/match-api';
import {
  CLOSED_MATCH,
  CLOSED_SUMMARY,
  SCHEDULED_MATCH,
  SCHEDULED_SUMMARY,
} from '../../../shared/roster/matches.testing';
import { SETTINGS } from '../../../shared/roster/settings.testing';
import { Toasts } from '../../../ui/toast/toasts';
import { AdminMatchFormPage } from './admin-match-form-page';

const LIMITS = {
  opponentMaxLength: 40,
  matchdayMin: 1,
  goalsMax: 30,
  statCountMax: 20,
  sheetPlayersMax: 30,
};

describe('AdminMatchFormPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderPage(
    options: { matchId?: string; loaded?: MatchResponse; calendar?: MatchSummaryResponse[] } = {},
  ) {
    const result = await render(AdminMatchFormPage, {
      inputs: { leagueId: 'l1', matchId: options.matchId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    if (options.matchId === undefined) {
      http.expectOne('/api/leagues/l1/matches').flush(options.calendar ?? []);
    } else {
      http
        .expectOne(`/api/leagues/l1/matches/${options.matchId}`)
        .flush(options.loaded ?? SCHEDULED_MATCH);
    }
    http.expectOne('/api/leagues/l1/matches/limits').flush(LIMITS);
    http.expectOne('/api/leagues/l1/settings').flush(SETTINGS);
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  it('schedules a match proposing the next matchday and a week after the last one', async () => {
    await renderPage({ calendar: [CLOSED_SUMMARY, SCHEDULED_SUMMARY] });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(screen.getByRole('textbox', { name: 'Jornada' })).toHaveValue('13');
    expect(screen.getByLabelText('Fecha')).toHaveValue('2026-11-09');
    expect(screen.getByLabelText('Hora')).toHaveValue('11:00');
    expect(screen.getByText('Hasta 40 caracteres')).toBeInTheDocument();
    expect(
      screen.getByText('Hora de Madrid. Las alineaciones se bloquean 2 h antes.'),
    ).toBeInTheDocument();

    await userEvent.type(screen.getByRole('textbox', { name: 'Rival' }), ' Atlético Lavadores ');
    await userEvent.click(screen.getByRole('radio', { name: 'Fuera' }));
    await userEvent.click(screen.getByRole('button', { name: 'Programar partido' }));

    const request = http.expectOne({ method: 'POST', url: '/api/leagues/l1/matches' });
    expect(request.request.body).toEqual({
      matchday: 13,
      opponent: 'Atlético Lavadores',
      venue: 'AWAY',
      kickoffAt: '2026-11-09T10:00:00.000Z',
    });
    request.flush({ ...SCHEDULED_MATCH, id: 'm13' }, { status: 201, statusText: 'Created' });

    expect(toasts()).toEqual(['Partido programado']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'matches', 'm13']);
  });

  it('checks the matchday against the limit without sending anything', async () => {
    await renderPage({});

    await userEvent.type(screen.getByRole('textbox', { name: 'Jornada' }), '0');
    await userEvent.type(screen.getByRole('textbox', { name: 'Rival' }), 'Arrabal');
    await userEvent.click(screen.getByRole('button', { name: 'Programar partido' }));

    expect(screen.getByText('Escribe una jornada desde la 1.')).toBeInTheDocument();
    expect(screen.getAllByText('Rellena este campo.')).toHaveLength(2);
    expect(
      screen.getByText('No se pudo guardar el partido. Revisa los campos marcados.'),
    ).toBeInTheDocument();
  });

  it.each([
    ['MATCHDAY_TAKEN', 'Ya hay otro partido en esa jornada.', 'Jornada'],
    ['KICKOFF_TAKEN', 'Ya hay otro partido a esa hora.', 'Hora'],
    ['INVALID_OPPONENT', 'El nombre del rival está vacío o es demasiado largo.', 'Rival'],
  ])('paints %s under its field', async (code, text, label) => {
    await renderPage({ calendar: [SCHEDULED_SUMMARY] });
    await userEvent.type(screen.getByRole('textbox', { name: 'Rival' }), 'Arrabal');

    await userEvent.click(screen.getByRole('button', { name: 'Programar partido' }));
    http
      .expectOne({ method: 'POST', url: '/api/leagues/l1/matches' })
      .flush({ status: 409, code }, { status: 409, statusText: 'Conflict' });

    expect(await screen.findByText(text)).toBeInTheDocument();
    expect(screen.getByLabelText(label)).toBeInvalid();
  });

  it('edits a match, warning that changing the time postpones it', async () => {
    await renderPage({ matchId: 'm12' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    expect(screen.getByRole('textbox', { name: 'Rival' })).toHaveValue('CD Arrabal');
    expect(screen.queryByText(/se desbloquean/)).not.toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText('Hora'));
    await userEvent.type(screen.getByLabelText('Hora'), '12:30');
    expect(screen.getByText(/se desbloquean/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar partido' }));

    const request = http.expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m12' });
    expect(request.request.body).toEqual({
      matchday: 12,
      opponent: 'CD Arrabal',
      venue: 'AWAY',
      kickoffAt: '2026-11-02T11:30:00.000Z',
    });
    request.flush(SCHEDULED_MATCH);

    expect(toasts()).toEqual(['Partido guardado']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'matches', 'm12']);
  });

  it('leaves the matchday, the day and the time read-only once the match is closed', async () => {
    await renderPage({ matchId: 'm11', loaded: CLOSED_MATCH });

    expect(screen.getByRole('textbox', { name: 'Jornada' })).toBeDisabled();
    expect(screen.getByLabelText('Fecha')).toBeDisabled();
    expect(screen.getByLabelText('Hora')).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Rival' })).toBeEnabled();
    expect(screen.getAllByText(/ya no se pueden cambiar/)).toHaveLength(2);
  });

  it('explains that the match is closed and reloads when the server refuses the change', async () => {
    const { fixture } = await renderPage({ matchId: 'm12' });
    await userEvent.clear(screen.getByLabelText('Hora'));
    await userEvent.type(screen.getByLabelText('Hora'), '12:30');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar partido' }));
    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/matches/m12' })
      .flush({ status: 409, code: 'MATCH_CLOSED' }, { status: 409, statusText: 'Conflict' });

    expect(toasts()).toEqual([
      'El partido está cerrado: no se puede cambiar su jornada ni su hora, ni borrarlo.',
    ]);
    const match = await vi.waitFor(() => http.expectOne('/api/leagues/l1/matches/m12'));
    match.flush(CLOSED_MATCH);
    http.expectOne('/api/leagues/l1/matches/limits').flush(LIMITS);
    http.expectOne('/api/leagues/l1/settings').flush(SETTINGS);
    await fixture.whenStable();
    expect(screen.getByRole('textbox', { name: 'Jornada' })).toBeDisabled();
  });

  it('offers to try again when it fails to load', async () => {
    const result = await render(AdminMatchFormPage, {
      inputs: { leagueId: 'l1', matchId: 'm12' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l1/matches/m12')
      .flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/leagues/l1/matches/limits').flush(LIMITS);
    http.expectOne('/api/leagues/l1/settings').flush(SETTINGS);
    await result.fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
