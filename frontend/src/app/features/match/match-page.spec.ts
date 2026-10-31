import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import type { MatchResponse } from '../../core/api/match/match-api';
import { PLAYERS } from '../../shared/roster/players.testing';
import { CLOSED_MATCH, SCHEDULED_MATCH } from '../../shared/roster/matches.testing';
import { MatchPage } from './match-page';

describe('MatchPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderMatch(matchId: string) {
    const result = await render(MatchPage, {
      inputs: { leagueId: 'l2', matchId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  function answer(matchId: string, match: MatchResponse) {
    http.expectOne(`/api/leagues/l2/matches/${matchId}`).flush(match);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
  }

  it('shows the result, the sheet with what each player did and the ways to follow the matchday', async () => {
    const { fixture } = await renderMatch('m11');
    answer('m11', CLOSED_MATCH);
    await fixture.whenStable();

    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 11' })).toBeInTheDocument();
    expect(screen.getByText('Contra Racing Vilariño, En casa')).toBeInTheDocument();
    expect(screen.getByText('3–1')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Valorar' })).toHaveAttribute(
      'href',
      '/leagues/l2/ratings/m11',
    );
    expect(screen.getByRole('link', { name: 'Puntos de la jornada' })).toHaveAttribute(
      'href',
      '/leagues/l2/standings/m11',
    );
    expect(screen.getByRole('link', { name: 'Alineaciones' })).toHaveAttribute(
      'href',
      '/leagues/l2/lineups/m11',
    );
    const rows = within(screen.getByRole('region', { name: 'Acta' })).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Pablo');
    expect(rows[0]).toHaveTextContent('2 goles, 1 asistencia, tarjeta amarilla');
    expect(rows[1]).toHaveTextContent('Diego');
    expect(rows[1]).toHaveTextContent('1 penalti parado');
  });

  it('says that the sheet is published when the match closes, without links', async () => {
    const { fixture } = await renderMatch('m12');
    answer('m12', SCHEDULED_MATCH);
    await fixture.whenStable();

    expect(screen.getByText('El acta se publica al cerrar el partido.')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Valorar' })).not.toBeInTheDocument();
    expect(screen.queryByText(/^\d+–\d+$/)).not.toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    const { fixture } = await renderMatch('m11');
    http
      .expectOne('/api/leagues/l2/matches/m11')
      .flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
