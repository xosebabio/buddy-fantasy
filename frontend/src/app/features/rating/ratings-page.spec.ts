import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import type { RatingWindowResponse } from '../../core/api/rating/rating-api';
import { RatingsPage, sortWindows } from './ratings-page';

function window(overrides: Partial<RatingWindowResponse>): RatingWindowResponse {
  return {
    matchId: 'm11',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    closesAt: '2099-10-28T12:00:00Z',
    status: 'OPEN',
    players: 9,
    myRatings: 3,
    excluded: false,
    ...overrides,
  };
}

describe('sortWindows', () => {
  it('puts the open windows first, the soonest to close at the top, and then the closed, the latest first', () => {
    const sorted = sortWindows([
      window({ matchId: 'a', matchday: 8, status: 'CLOSED' }),
      window({ matchId: 'b', matchday: 10, closesAt: '2099-11-02T12:00:00Z' }),
      window({ matchId: 'c', matchday: 9, status: 'CLOSED' }),
      window({ matchId: 'd', matchday: 11, closesAt: '2099-10-28T12:00:00Z' }),
    ]);

    expect(sorted.map((w) => w.matchId)).toEqual(['d', 'b', 'c', 'a']);
  });
});

describe('RatingsPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(windows: RatingWindowResponse[] | null) {
    const result = await render(RatingsPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    if (windows === null) {
      expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
      http.expectOne('/api/leagues/l2/ratings').flush(null, { status: 500, statusText: 'Error' });
    } else {
      http.expectOne('/api/leagues/l2/ratings').flush(windows);
    }
    await result.fixture.whenStable();
    return result;
  }

  it('lists the open window with its countdown and how many players are rated, and links to it', async () => {
    await renderPage([window({}), window({ matchId: 'm10', matchday: 10, status: 'CLOSED' })]);

    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveAttribute('href', '/leagues/l2/ratings/m11');
    expect(links[0]).toHaveTextContent('Jornada 11');
    expect(links[0]).toHaveTextContent(/Cierra en/);
    expect(links[0]).toHaveTextContent('3 de 9');
    expect(links[1]).toHaveTextContent('Jornada 10');
    expect(links[1]).toHaveTextContent('Cerrada');
  });

  it('marks the window whose ratings the admin cancelled', async () => {
    await renderPage([window({ excluded: true })]);

    expect(screen.getByText('Excluido')).toBeInTheDocument();
    expect(screen.queryByText('3 de 9')).not.toBeInTheDocument();
  });

  it('says that there is nothing to rate yet', async () => {
    await renderPage([]);

    expect(screen.getByText(/Aún no hay partidos para valorar/)).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage(null);

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
