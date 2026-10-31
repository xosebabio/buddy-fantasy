import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, waitFor, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { MyLineupResponse } from '../../core/api/lineup/lineup-api';
import { ALBERTO, DIEGO, PABLO, PLAYERS } from '../../shared/roster/players.testing';
import { Toasts } from '../../ui/toast/toasts';
import { SETTINGS } from '../../shared/roster/settings.testing';
import { LineupPage } from './lineup-page';

const TARGET = {
  id: 'm12',
  matchday: 12,
  kickoffAt: '2099-11-02T10:00:00Z',
  lockAt: '2099-11-02T08:00:00Z',
};

function lineup(overrides: Partial<MyLineupResponse> = {}): MyLineupResponse {
  return {
    budget: 40,
    changesRemaining: 1,
    changesUsed: 1,
    maxChanges: 2,
    players: [
      { id: DIEGO.id, position: 'GOALKEEPER', value: DIEGO.currentValue, active: true },
      { id: PABLO.id, position: 'FORWARD', value: PABLO.currentValue, active: true },
    ],
    savedAt: '2026-10-30T10:00:00Z',
    targetMatch: TARGET,
    totalValue: 25.7,
    warnings: [],
    ...overrides,
  };
}

function fail(): never {
  throw new Error('Expected the element to exist');
}

describe('LineupPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(saved: MyLineupResponse = lineup()) {
    const result = await render(LineupPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/leagues/l2/lineup').flush(saved);
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await result.fixture.whenStable();
    return result;
  }

  it('shows a skeleton while it loads', async () => {
    await render(LineupPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);

    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    http.expectOne('/api/leagues/l2/lineup').flush(lineup());
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
  });

  it('shows the matchday, the budget, the changes and the free places', async () => {
    await renderPage();

    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 12' })).toBeInTheDocument();
    expect(screen.getByText(/Se bloquea en/)).toBeInTheDocument();
    expect(screen.getByText('25,7')).toBeInTheDocument();
    expect(screen.getByText('/ 40,0 M')).toBeInTheDocument();
    expect(screen.getByText('de 2')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /^Plaza libre/ })).toHaveLength(5);
    expect(screen.getByRole('button', { name: 'Guardar alineación' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Descartar cambios' })).not.toBeInTheDocument();
  });

  it('explains that no match is left to play', async () => {
    await renderPage(lineup({ targetMatch: null }));

    expect(screen.getByText(/No quedan partidos por jugar/)).toBeInTheDocument();
  });

  it('opens the candidates of a free place, adds one and saves the lineup it with the server response', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();

    await user.click(
      screen.getAllByRole('button', { name: /^Plaza libre, Defensa/ }).at(0) ?? fail(),
    );
    const sheet = await screen.findByRole('dialog', { name: 'Añadir un jugador' });
    expect(within(sheet).getByText('Te quedan 14,3 M')).toBeInTheDocument();
    expect(within(sheet).queryByText('Xurxo')).not.toBeInTheDocument();
    await user.click(within(sheet).getByRole('button', { name: /Alberto/ }));
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: /Alberto .*, Delantero$/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Descartar cambios' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Guardar alineación' }));
    const request = http.expectOne('/api/leagues/l2/lineup');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ playerIds: [DIEGO.id, PABLO.id, ALBERTO.id] });
    request.flush(
      lineup({
        players: [
          { id: DIEGO.id, position: 'GOALKEEPER', value: 8.5, active: true },
          { id: PABLO.id, position: 'FORWARD', value: 17.2, active: true },
          { id: ALBERTO.id, position: 'FORWARD', value: 12, active: true },
        ],
      }),
    );
    await fixture.whenStable();

    expect(
      TestBed.inject(Toasts)
        .toasts()
        .map((t) => t.message),
    ).toContain('Alineación guardada');
    expect(screen.queryByRole('button', { name: 'Descartar cambios' })).not.toBeInTheDocument();
  });

  it('removes a player from the sheet and can discard the change', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();

    await user.click(screen.getByRole('button', { name: /Pablo .*, Delantero$/ }));
    const sheet = await screen.findByRole('dialog', { name: 'Cambiar a Pablo' });
    expect(within(sheet).getByText(/En la alineación/)).toBeInTheDocument();
    await user.click(within(sheet).getByRole('button', { name: 'Quitar de la alineación' }));
    await fixture.whenStable();

    expect(screen.queryByRole('button', { name: /Pablo/ })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Descartar cambios' }));
    expect(screen.getByRole('button', { name: /Pablo .*, Delantero$/ })).toBeInTheDocument();
  });

  it('warns of a budget that is passed but still lets save', async () => {
    await renderPage(lineup({ budget: 20 }));

    expect(screen.getByText(/Te pasas 5,7 M del presupuesto/)).toBeInTheDocument();
  });

  it('shows the error of a rejected save, reloads and keeps the draft', async () => {
    const user = userEvent.setup();
    const { fixture } = await renderPage();
    await user.click(
      screen.getAllByRole('button', { name: /^Plaza libre, Defensa/ }).at(0) ?? fail(),
    );
    await user.click(await screen.findByRole('button', { name: /Alberto/ }));
    await user.click(screen.getByRole('button', { name: 'Guardar alineación' }));

    http
      .expectOne('/api/leagues/l2/lineup')
      .flush(
        { status: 409, code: 'MAX_CHANGES_EXCEEDED' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(
      TestBed.inject(Toasts)
        .toasts()
        .map((t) => t.message),
    ).toContain('Has pasado del máximo de cambios de esta jornada.');
    fixture.detectChanges();
    http.expectOne('/api/leagues/l2/lineup').flush(lineup());
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await fixture.whenStable();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Descartar cambios' })).toBeInTheDocument();
    });
  });

  it('explains that the match locked when a reload shows another one', async () => {
    const { fixture } = await renderPage();

    document.dispatchEvent(new Event('visibilitychange'));
    fixture.detectChanges();
    http
      .expectOne('/api/leagues/l2/lineup')
      .flush(lineup({ targetMatch: { ...TARGET, id: 'm13', matchday: 13 } }));
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await fixture.whenStable();

    expect(
      TestBed.inject(Toasts)
        .toasts()
        .map((t) => t.message),
    ).toContain('La jornada 12 ya se ha bloqueado. Lo que guardes cuenta para la 13.');
  });

  it('offers to try again when it fails to load', async () => {
    const { fixture } = await render(LineupPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l2/lineup')
      .flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
