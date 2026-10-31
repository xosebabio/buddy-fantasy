import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from './matches.testing';
import { MatchesPage } from './matches-page';

describe('MatchesPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderMatches() {
    const result = await render(MatchesPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  it('shows a skeleton while it loads', async () => {
    await renderMatches();

    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    http.expectOne('/api/leagues/l2/matches').flush([]);
  });

  it('lists the matchdays in order with venue, date and, if closed, the result', async () => {
    const { fixture } = await renderMatches();
    http.expectOne('/api/leagues/l2/matches').flush([SCHEDULED_SUMMARY, CLOSED_SUMMARY]);
    await fixture.whenStable();

    const rows = screen.getAllByRole('link');
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent('Jornada 11, contra Racing Vilariño');
    expect(rows[0]).toHaveTextContent('En casa');
    expect(rows[0]).toHaveTextContent('3–1');
    expect(rows[0]).toHaveAttribute('href', '/leagues/l2/matches/m11');
    expect(rows[1]).toHaveTextContent('Jornada 12, contra CD Arrabal');
    expect(rows[1]).toHaveTextContent('Fuera');
    expect(rows[1]).not.toHaveTextContent('–');
  });

  it('invites to wait for the next match when there are none', async () => {
    const { fixture } = await renderMatches();
    http.expectOne('/api/leagues/l2/matches').flush([]);
    await fixture.whenStable();

    expect(screen.getByText(/Aún no hay partidos/)).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    const { fixture } = await renderMatches();
    http
      .expectOne('/api/leagues/l2/matches')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
