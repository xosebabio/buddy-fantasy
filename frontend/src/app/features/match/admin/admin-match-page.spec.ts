import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchResponse } from '../../../core/api/match/match-api';
import { CLOSED_MATCH, SCHEDULED_MATCH } from '../../../shared/roster/matches.testing';
import { Toasts } from '../../../ui/toast/toasts';
import { AdminMatchPage } from './admin-match-page';

const WINDOW = {
  matchId: 'm11',
  matchday: 11,
  status: 'OPEN',
  kickoffAt: '2026-10-26T10:00:00Z',
  closesAt: '2099-10-28T12:00:00Z',
  format: 'SPADES',
  players: [],
  excluded: false,
  excludedUserIds: [],
};

describe('AdminMatchPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderPage(matchId: string, match: MatchResponse) {
    const result = await render(AdminMatchPage, {
      inputs: { leagueId: 'l1', matchId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/leagues/l1/matches/${matchId}`).flush(match);
    if (match.status === 'CLOSED') {
      const ratings = await vi.waitFor(() => http.expectOne(`/api/leagues/l1/ratings/${matchId}`));
      ratings.flush(WINDOW);
    }
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  it('shows a match that is not closed with its rows and the way to delete it', async () => {
    vi.useFakeTimers({ now: new Date('2026-10-30T10:00:00Z'), toFake: ['Date'] });
    try {
      await renderPage('m12', { ...SCHEDULED_MATCH, warnings: ['GOALS_MISMATCH'] });
    } finally {
      vi.useRealTimers();
    }

    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 12' })).toBeInTheDocument();
    expect(screen.getByText('Programado')).toBeInTheDocument();
    expect(screen.getByText('Contra CD Arrabal, Fuera')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Datos del partido/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/matches/m12/edit',
    );
    expect(screen.getByRole('link', { name: /Acta/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/matches/m12/sheet',
    );
    expect(screen.getByRole('link', { name: /Auditoría/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/matches/m12/audit',
    );
    expect(screen.queryByRole('link', { name: /Valoraciones/ })).not.toBeInTheDocument();
    expect(
      screen.getByText('Los goles de los jugadores no suman los del resultado.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Borrar partido' })).toBeEnabled();
  });

  it('marks a match that started and is not closed as pending the sheet', async () => {
    vi.useFakeTimers({ now: new Date('2026-11-02T11:00:00Z'), toFake: ['Date'] });
    try {
      await renderPage('m12', SCHEDULED_MATCH);
    } finally {
      vi.useRealTimers();
    }

    expect(screen.getByText('Acta pendiente')).toBeInTheDocument();
  });

  it('shows a closed match with its result, its open window and no way to delete it', async () => {
    await renderPage('m11', CLOSED_MATCH);

    expect(screen.getByText('Cerrado')).toBeInTheDocument();
    expect(screen.getByText('La jornada y la hora ya no se pueden cambiar.')).toBeInTheDocument();
    expect(screen.getByText(/3–1/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Valoraciones/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/ratings/m11',
    );
    expect(screen.getByText(/Abierta, cierra en/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Borrar partido' })).not.toBeInTheDocument();
  });

  it('asks before deleting, deletes and goes back to the calendar', async () => {
    await renderPage('m12', SCHEDULED_MATCH);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await userEvent.click(screen.getByRole('button', { name: 'Borrar partido' }));
    expect(
      await screen.findByText(
        'Se borra el partido de la jornada 12. Las alineaciones guardadas pasan al siguiente.',
      ),
    ).toBeInTheDocument();
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Borrar partido' }),
    );
    http.expectOne({ method: 'DELETE', url: '/api/leagues/l1/matches/m12' }).flush(null, {
      status: 204,
      statusText: 'No Content',
    });

    expect(toasts()).toEqual(['Partido borrado']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'matches']);
  });

  it('does not delete when the user cancels', async () => {
    await renderPage('m12', SCHEDULED_MATCH);

    await userEvent.click(screen.getByRole('button', { name: 'Borrar partido' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));

    http.expectNone({ method: 'DELETE', url: '/api/leagues/l1/matches/m12' });
  });

  it('explains that the match is closed and reloads when the server refuses to delete it', async () => {
    await renderPage('m12', SCHEDULED_MATCH);

    await userEvent.click(screen.getByRole('button', { name: 'Borrar partido' }));
    await screen.findByRole('alertdialog');
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Borrar partido' }),
    );
    http
      .expectOne({ method: 'DELETE', url: '/api/leagues/l1/matches/m12' })
      .flush({ status: 409, code: 'MATCH_CLOSED' }, { status: 409, statusText: 'Conflict' });

    expect(toasts()).toEqual([
      'El partido está cerrado: no se puede cambiar su jornada ni su hora, ni borrarlo.',
    ]);
    const reloaded = await vi.waitFor(() => http.expectOne('/api/leagues/l1/matches/m12'));
    reloaded.flush(CLOSED_MATCH);
    const ratings = await vi.waitFor(() => http.expectOne('/api/leagues/l1/ratings/m12'));
    ratings.flush(WINDOW);
  });

  it('offers to try again when it fails to load', async () => {
    const result = await render(AdminMatchPage, {
      inputs: { leagueId: 'l1', matchId: 'm12' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l1/matches/m12')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await result.fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
