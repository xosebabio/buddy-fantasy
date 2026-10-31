import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { LockedLineupsResponse } from '../../core/api/lineup/lineup-api';
import { PLAYERS, PABLO, DIEGO } from '../../shared/roster/players.testing';
import { MEMBERS } from '../../shared/roster/members.testing';
import { LockedLineupsPage } from './locked-lineups-page';

const LOCKED: LockedLineupsResponse = {
  matchId: 'm12',
  matchday: 12,
  kickoffAt: '2026-11-02T10:00:00Z',
  lockAt: '2026-11-02T08:00:00Z',
  lockedAt: '2026-11-02T08:01:00Z',
  lineups: [
    {
      userId: 'u2',
      budget: 40,
      overBudget: true,
      totalValue: 41.5,
      players: [{ id: PABLO.id, value: 17.2 }],
    },
    {
      userId: 'gone',
      budget: 40,
      overBudget: false,
      totalValue: 8,
      players: [{ id: DIEGO.id, value: 8 }],
    },
  ],
};

function fail(): never {
  throw new Error('Expected the element to exist');
}

describe('LockedLineupsPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage(locked: () => void) {
    const result = await render(LockedLineupsPage, {
      inputs: { leagueId: 'l2', matchId: 'm12' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    locked();
    http.expectOne((r) => r.url === '/api/leagues/l2/players').flush(PLAYERS);
    http.expectOne('/api/leagues/l2/members').flush(MEMBERS);
    await result.fixture.whenStable();
    return result;
  }

  it('lists a row per manager, a former member as such, and opens their players with the locked value', async () => {
    const user = userEvent.setup();
    await renderPage(() => {
      http.expectOne('/api/leagues/l2/lineups/m12').flush(LOCKED);
    });

    expect(
      screen.getByRole('heading', { level: 1, name: 'Alineaciones de la jornada 12' }),
    ).toBeInTheDocument();
    const rows = screen.getAllByRole('button', { expanded: false });
    expect(rows[0]).toHaveTextContent('Antiguo miembro');
    expect(rows[1]).toHaveTextContent('Brais Otero');
    expect(rows[1]).toHaveTextContent('41,5 M');
    expect(rows[1]).toHaveTextContent('Fuera de presupuesto');

    const row = rows[1] ?? fail();
    await user.click(row);
    const panel = document.getElementById(row.getAttribute('aria-controls') ?? '') ?? fail();
    expect(within(panel).getByText('Pablo')).toBeInTheDocument();
    expect(within(panel).getByText('17,2 M')).toBeInTheDocument();
  });

  it('says that the lineups appear when they lock instead of failing', async () => {
    await renderPage(() => {
      http
        .expectOne('/api/leagues/l2/lineups/m12')
        .flush(
          { status: 409, code: 'LINEUPS_NOT_LOCKED' },
          { status: 409, statusText: 'Conflict' },
        );
    });

    expect(screen.getByText('Las alineaciones se verán cuando se bloqueen.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('says that nobody saved a lineup when none is locked', async () => {
    await renderPage(() => {
      http.expectOne('/api/leagues/l2/lineups/m12').flush({ ...LOCKED, lineups: [] });
    });

    expect(screen.getByText(/Nadie guardó una alineación/)).toBeInTheDocument();
  });
});
