import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { PlayerMatchdayResponse } from '../../core/api/scoring/scoring-api';
import { latestFirst, PlayerPage } from './player-page';
import { PABLO, XURXO } from '../../shared/roster/players.testing';

const POINTS: PlayerMatchdayResponse[] = [
  {
    matchId: 'm10',
    matchday: 10,
    kickoffAt: '2026-10-19T09:00:00Z',
    position: 'FORWARD',
    actions: { GOAL: 3 },
    actionPoints: 3,
    ratingPoints: 4,
    points: 7,
  },
  {
    matchId: 'm11',
    matchday: 11,
    kickoffAt: '2026-10-26T10:00:00Z',
    position: 'FORWARD',
    actions: { GOAL: 3, ASSIST: 1 },
    actionPoints: 4,
    ratingPoints: 10,
    points: 14,
  },
];

describe('PlayerPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPlayer(playerId = 'p9') {
    const result = await render(PlayerPage, {
      inputs: { leagueId: 'l2', playerId },
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  it('shows the player with their values and the matchdays from the latest one', async () => {
    const { fixture } = await renderPlayer();
    http.expectOne(`/api/leagues/l2/players/p9`).flush(PABLO);
    http.expectOne(`/api/leagues/l2/players/p9/points`).flush(POINTS);
    await fixture.whenStable();

    expect(screen.getByRole('heading', { level: 1, name: 'Pablo' })).toBeInTheDocument();
    expect(screen.getByText('Delantero')).toBeInTheDocument();
    expect(screen.getByText('Valor actual')).toBeInTheDocument();
    expect(screen.getByText('17,2')).toBeInTheDocument();
    expect(screen.getByText('15,0')).toBeInTheDocument();
    expect(screen.queryByText('Dado de baja')).not.toBeInTheDocument();
    const rows = screen.getAllByRole('button', { expanded: false });
    expect(rows[0]).toHaveTextContent('Jornada 11');
    expect(rows[0]).toHaveTextContent('+14');
    expect(rows[1]).toHaveTextContent('Jornada 10');
  });

  it('opens a matchday to show where its points come from', async () => {
    const { fixture } = await renderPlayer();
    http.expectOne(`/api/leagues/l2/players/p9`).flush(PABLO);
    http.expectOne(`/api/leagues/l2/players/p9/points`).flush(POINTS);
    await fixture.whenStable();

    await userEvent.click(screen.getByRole('button', { name: /Jornada 11/ }));

    expect(screen.getByText('Asistencia')).toBeVisible();
  });

  it('marks a retired player and says that one without points has not scored yet', async () => {
    const { fixture } = await renderPlayer('p5');
    http.expectOne(`/api/leagues/l2/players/p5`).flush(XURXO);
    http.expectOne(`/api/leagues/l2/players/p5/points`).flush([]);
    await fixture.whenStable();

    expect(screen.getByText('Dado de baja')).toBeInTheDocument();
    expect(screen.getByText('Aún no ha puntuado.')).toBeInTheDocument();
  });

  it('offers to try the points again when they fail to load', async () => {
    const { fixture } = await renderPlayer();
    http.expectOne(`/api/leagues/l2/players/p9`).flush(PABLO);
    http
      .expectOne(`/api/leagues/l2/players/p9/points`)
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});

describe('latestFirst', () => {
  it('puts the latest matchday first without changing the original', () => {
    expect(latestFirst(POINTS).map((matchday) => matchday.matchday)).toEqual([11, 10]);
    expect(POINTS[0]?.matchday).toBe(10);
  });
});
