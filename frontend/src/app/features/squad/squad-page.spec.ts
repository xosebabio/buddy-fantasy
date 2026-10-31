import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import { PLAYERS } from '../../shared/roster/players.testing';
import { SquadPage } from './squad-page';

describe('SquadPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderSquad() {
    const result = await render(SquadPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  function answer(players = PLAYERS) {
    http.expectOne((request) => request.url === '/api/leagues/l2/players').flush(players);
  }

  it('shows a skeleton while it loads', async () => {
    await renderSquad();

    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    answer();
  });

  it('asks for the retired players too and lists the active ones by position, with their value', async () => {
    const { fixture } = await renderSquad();
    const request = http.expectOne((r) => r.url === '/api/leagues/l2/players');
    expect(request.request.params.get('includeRetired')).toBe('true');
    request.flush(PLAYERS);
    await fixture.whenStable();

    expect(screen.getByRole('heading', { level: 2, name: 'Porteros' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Delanteros' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Defensas' })).not.toBeInTheDocument();
    const rows = within(screen.getByRole('region', { name: 'Delanteros' })).getAllByRole('link');
    expect(rows[0]).toHaveTextContent('Alberto');
    expect(rows[1]).toHaveTextContent('Pablo');
    expect(rows[1]).toHaveTextContent('17,2 M');
    expect(rows[1]).toHaveAttribute('href', '/leagues/l2/squad/p9');
    expect(screen.queryByText('Xurxo')).not.toBeInTheDocument();
  });

  it('invites to wait for the admin when there are no players', async () => {
    const { fixture } = await renderSquad();
    answer([]);
    await fixture.whenStable();

    expect(screen.getByText(/Aún no hay jugadores/)).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    const { fixture } = await renderSquad();
    http
      .expectOne((r) => r.url === '/api/leagues/l2/players')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
