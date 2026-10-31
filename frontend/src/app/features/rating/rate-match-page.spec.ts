import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, waitFor } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MatchRatingsResponse, PlayerRatingResponse } from '../../core/api/rating/rating-api';
import { PLAYERS } from '../../shared/roster/players.testing';
import { Toasts } from '../../ui/toast/toasts';
import { CLOSED_MATCH } from '../../shared/roster/matches.testing';
import { RateMatchPage } from './rate-match-page';

function rating(overrides: Partial<PlayerRatingResponse>): PlayerRatingResponse {
  return {
    playerId: 'p1',
    position: 'GOALKEEPER',
    myRating: null,
    average: null,
    votes: null,
    ratingPoints: null,
    ...overrides,
  };
}

function ratings(overrides: Partial<MatchRatingsResponse> = {}): MatchRatingsResponse {
  return {
    matchId: 'm11',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    closesAt: '2099-10-28T12:00:00Z',
    status: 'OPEN',
    format: 'SPADES',
    excluded: false,
    excludedUserIds: null,
    players: [
      rating({ playerId: 'p1', position: 'GOALKEEPER', myRating: 0 }),
      rating({ playerId: 'p9', position: 'FORWARD' }),
      rating({ playerId: 'p7', position: 'FORWARD' }),
    ],
    ...overrides,
  };
}

const CLOSED_RATINGS = ratings({
  status: 'CLOSED',
  closesAt: '2026-10-28T12:00:00Z',
  players: [
    rating({ playerId: 'p1', myRating: 3, average: 2.83, votes: 6, ratingPoints: 9 }),
    rating({ playerId: 'p9', position: 'FORWARD', average: null, votes: 0, ratingPoints: 0 }),
  ],
});

describe('RateMatchPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(
    flushRatings: (request: HttpTestingController) => void = (h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(ratings());
    },
  ) {
    const result = await render(RateMatchPage, {
      inputs: { leagueId: 'l2', matchId: 'm11' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    flushRatings(http);
    http.expectOne('/api/leagues/l2/matches/m11').flush(CLOSED_MATCH);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  it('shows the matchday, the result, what each player did and the votes already given', async () => {
    await renderPage();

    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 11' })).toBeInTheDocument();
    expect(screen.getByText(/Cierra en/)).toBeInTheDocument();
    expect(screen.getByText('Ganamos 3–1 a Racing Vilariño')).toBeInTheDocument();
    expect(screen.getByText(/1 penalti parado/)).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: '0', checked: true })).toBeInTheDocument();
    expect(screen.getByText('Has valorado a 1 de 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar valoraciones' })).toBeDisabled();
  });

  it('saves every vote, a 0 included, and leaves out whoever has none', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();

    await user.click(screen.getAllByRole('radio', { name: '4' })[1] ?? screen.getByText('x'));
    expect(screen.getByText('Has valorado a 2 de 3')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Guardar valoraciones' }));

    const request = http.expectOne('/api/leagues/l2/ratings/m11');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      ratings: [
        { playerId: 'p1', value: 0 },
        { playerId: 'p9', value: 4 },
      ],
    });
    request.flush(
      ratings({
        players: [
          rating({ playerId: 'p1', myRating: 0 }),
          rating({ playerId: 'p9', position: 'FORWARD', myRating: 4 }),
          rating({ playerId: 'p7', position: 'FORWARD' }),
        ],
      }),
    );
    await fixture.whenStable();

    expect(toasts()).toContain('Valoraciones guardadas');
    expect(screen.getByRole('button', { name: 'Guardar valoraciones' })).toBeDisabled();
  });

  it('takes a vote back and warns of unsaved changes until it is the saved one again', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();
    const page = fixture.componentInstance;

    await user.click(screen.getByRole('button', { name: 'Quitar voto a Diego' }));
    expect(page.unsaved()).toBe(true);
    expect(screen.getByText('Has valorado a 0 de 3')).toBeInTheDocument();

    await user.click(screen.getAllByRole('radio', { name: '0' })[0] ?? screen.getByText('x'));
    expect(page.unsaved()).toBe(false);
  });

  it('uses the stepper when the window was frozen in marks', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(ratings({ format: 'DECIMAL' }));
    });

    expect(screen.getAllByRole('spinbutton')).toHaveLength(3);
    expect(screen.getByText(/De 0 a 10/)).toBeInTheDocument();
  });

  it('shows a notice instead of the controls when the user was excluded', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(ratings({ excluded: true }));
    });

    expect(
      screen.getByText('El administrador ha cancelado tus valoraciones de este partido.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar valoraciones' })).not.toBeInTheDocument();
  });

  it('shows the average, the votes that counted and the points once the window is closed', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(CLOSED_RATINGS);
    });

    expect(screen.queryByText(/Cierra en/)).not.toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(screen.getByText(/Media 2,83 con\s+6 votos/)).toBeInTheDocument();
    expect(screen.getByText(/Tu voto: 3/)).toBeInTheDocument();
    expect(screen.getByText('+9')).toBeInTheDocument();
    expect(screen.getByText(/Sin votos/)).toBeInTheDocument();
    expect(screen.getByText(/Sin tu voto/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver los puntos de la jornada' })).toHaveAttribute(
      'href',
      '/leagues/l2/standings/m11',
    );
  });

  it('says that the results are being calculated while the server has none', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(
        ratings({
          status: 'CLOSED',
          players: [rating({ playerId: 'p1', myRating: 3 })],
        }),
      );
    });

    expect(screen.getByText(/Calculando los resultados/)).toBeInTheDocument();
  });

  it('says that the ratings open when the match closes instead of failing', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(
        { status: 409, code: 'MATCH_NOT_CLOSED' },
        { status: 409, statusText: 'Conflict' },
      );
    });

    expect(
      screen.getByText(/Las valoraciones se abren cuando se cierra el partido/),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('explains a window that closed while voting, drops the draft and reloads', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();
    await user.click(screen.getAllByRole('radio', { name: '4' })[1] ?? screen.getByText('x'));
    await user.click(screen.getByRole('button', { name: 'Guardar valoraciones' }));

    http
      .expectOne('/api/leagues/l2/ratings/m11')
      .flush(
        { status: 409, code: 'RATING_WINDOW_CLOSED' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(toasts()).toContain('El plazo para valorar ya ha terminado.');
    expect(fixture.componentInstance.unsaved()).toBe(false);
    fixture.detectChanges();
    http.expectOne('/api/leagues/l2/ratings/m11').flush(CLOSED_RATINGS);
    http.expectOne('/api/leagues/l2/matches/m11').flush(CLOSED_MATCH);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await fixture.whenStable();

    await waitFor(() => {
      expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    });
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage((h) => {
      h.expectOne('/api/leagues/l2/ratings/m11').flush(null, {
        status: 500,
        statusText: 'Server Error',
      });
    });

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
