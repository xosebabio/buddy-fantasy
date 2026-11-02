import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchRatingsResponse } from '../../core/api/rating/rating-api';
import { loadSession } from '../../core/auth/session.testing';
import { MEMBERS } from '../../shared/roster/members.testing';
import { Toasts } from '../../ui/toast/toasts';
import { AdminRatingsPage } from './admin-ratings-page';

function ratings(overrides: Partial<MatchRatingsResponse> = {}): MatchRatingsResponse {
  return {
    matchId: 'm11',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    closesAt: '2099-10-28T12:00:00Z',
    status: 'OPEN',
    format: 'SPADES',
    excluded: false,
    excludedUserIds: [],
    players: [],
    ...overrides,
  };
}

describe('AdminRatingsPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(answer: (http: HttpTestingController) => void) {
    const result = await render(AdminRatingsPage, {
      inputs: { leagueId: 'l1', matchId: 'm11' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    await loadSession();
    answer(http);
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS);
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  async function confirmCancel(): Promise<void> {
    await screen.findByRole('alertdialog');
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', {
        name: 'Cancelar valoraciones',
      }),
    );
  }

  it('lists every member with a button to cancel, and marks the cancelled ones', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(ratings({ excludedUserIds: ['u2'] }));
    });

    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 11' })).toBeInTheDocument();
    expect(screen.getByText('Xosé Babío')).toBeInTheDocument();
    expect(screen.getByText('(tú)')).toBeInTheDocument();
    expect(screen.getByText('Usuario sin nombre')).toBeInTheDocument();
    expect(screen.getByText('Cancelada')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Cancelar valoraciones de Xosé Babío' }),
    ).toBeEnabled();
    expect(
      screen.queryByRole('button', { name: 'Cancelar valoraciones de Brais Otero' }),
    ).toBeNull();
  });

  it('asks for confirmation naming the member, cancels and reloads', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(ratings());
    });

    await userEvent.click(
      screen.getByRole('button', { name: 'Cancelar valoraciones de Brais Otero' }),
    );
    expect(
      await screen.findByText(
        'Se borran las valoraciones de Brais Otero en la jornada 11 y no podrá volver a valorarla. No se puede deshacer.',
      ),
    ).toBeInTheDocument();
    await confirmCancel();
    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/ratings/m11/exclusions/u2' })
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(toasts()).toEqual(['Valoraciones canceladas']);
    const reloaded = await vi.waitFor(() => http.expectOne('/api/leagues/l1/ratings/m11'));
    reloaded.flush(ratings({ excludedUserIds: ['u2'] }));
    await vi.waitFor(() => {
      expect(screen.getByText('Cancelada')).toBeInTheDocument();
    });
  });

  it('does nothing when the admin does not confirm', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(ratings());
    });

    await userEvent.click(
      screen.getByRole('button', { name: 'Cancelar valoraciones de Brais Otero' }),
    );
    await screen.findByRole('alertdialog');
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }),
    );

    http.expectNone((r) => r.method === 'PUT');
    expect(toasts()).toEqual([]);
  });

  it('explains a closed window and reloads', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(ratings());
    });

    await userEvent.click(
      screen.getByRole('button', { name: 'Cancelar valoraciones de Brais Otero' }),
    );
    await confirmCancel();
    http
      .expectOne({ method: 'PUT', url: '/api/leagues/l1/ratings/m11/exclusions/u2' })
      .flush(
        { status: 409, code: 'RATING_WINDOW_CLOSED' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(toasts()).toEqual(['El plazo para valorar ya ha terminado.']);
    const reloaded = await vi.waitFor(() => http.expectOne('/api/leagues/l1/ratings/m11'));
    reloaded.flush(ratings({ status: 'CLOSED', excludedUserIds: [] }));
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS);
  });

  it('is read-only with the window closed and shows only the cancelled ones', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(
        ratings({ status: 'CLOSED', excludedUserIds: ['u2'] }),
      );
    });

    expect(screen.getByText('Brais Otero')).toBeInTheDocument();
    expect(screen.getByText('Cancelada')).toBeInTheDocument();
    expect(screen.queryByText('Xosé Babío')).toBeNull();
    expect(screen.queryByRole('button', { name: /Cancelar valoraciones/ })).toBeNull();
  });

  it('says nobody was cancelled when the window is closed and the list is empty', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(
        ratings({ status: 'CLOSED', excludedUserIds: [] }),
      );
    });

    expect(screen.getByText('No se ha cancelado ninguna valoración.')).toBeInTheDocument();
  });

  it('says the rating opens when the match is closed', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(
        { status: 409, code: 'MATCH_NOT_CLOSED' },
        { status: 409, statusText: 'Conflict' },
      );
    });

    expect(screen.getByText('La valoración se abre al cerrar el partido.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cancelar valoraciones/ })).toBeNull();
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l1/ratings/m11').flush(null, {
        status: 500,
        statusText: 'Server Error',
      });
    });

    expect(screen.getByRole('button', { name: /Reintentar/ })).toBeInTheDocument();
  });
});
