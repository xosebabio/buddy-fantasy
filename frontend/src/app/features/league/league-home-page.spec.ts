import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import type { MyLineupResponse } from '../../core/api/lineup/lineup-api';
import type { MatchSummaryResponse } from '../../core/api/match/match-api';
import type { RatingWindowResponse } from '../../core/api/rating/rating-api';
import type { StandingResponse } from '../../core/api/scoring/scoring-api';
import { loadSession } from '../../core/auth/session.testing';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../shared/roster/matches.testing';
import { LeagueHomePage, lockedMatch, pendingWindow } from './league-home-page';

const NEXT = {
  id: 'm12',
  matchday: 12,
  kickoffAt: '2099-11-02T10:00:00Z',
  lockAt: '2099-11-02T08:00:00Z',
};

function lineup(overrides: Partial<MyLineupResponse> = {}): MyLineupResponse {
  return {
    budget: 100,
    changesRemaining: 1,
    changesUsed: 1,
    maxChanges: 2,
    players: [],
    savedAt: '2026-10-30T10:00:00Z',
    targetMatch: NEXT,
    totalValue: 96.2,
    warnings: [],
    ...overrides,
  };
}

function window(overrides: Partial<RatingWindowResponse> = {}): RatingWindowResponse {
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

function standing(overrides: Partial<StandingResponse> = {}): StandingResponse {
  return {
    userId: 'u1',
    position: 2,
    points: 318,
    matchdays: 11,
    lastMatchdayPoints: 38,
    ...overrides,
  };
}

describe('pendingWindow', () => {
  it('takes the open window to close first that has players left to rate', () => {
    const pending = pendingWindow([
      window({ matchId: 'done', myRatings: 9 }),
      window({ matchId: 'excluded', excluded: true }),
      window({ matchId: 'closed', status: 'CLOSED' }),
      window({ matchId: 'late', closesAt: '2099-11-05T12:00:00Z' }),
      window({ matchId: 'soon' }),
    ]);

    expect(pending?.matchId).toBe('soon');
  });
});

describe('lockedMatch', () => {
  const m = (matchday: number, status: MatchSummaryResponse['status']): MatchSummaryResponse => ({
    ...SCHEDULED_SUMMARY,
    id: `m${String(matchday)}`,
    matchday,
    status,
  });

  it('takes the latest scheduled match before the target, or the latest one when none is left', () => {
    const matches = [m(10, 'CLOSED'), m(11, 'SCHEDULED'), m(12, 'SCHEDULED'), m(13, 'SCHEDULED')];

    expect(lockedMatch(matches, 13)?.id).toBe('m12');
    expect(lockedMatch(matches, 11)).toBeUndefined();
    expect(lockedMatch(matches, undefined)?.id).toBe('m13');
  });
});

describe('LeagueHomePage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  interface Data {
    lineup?: MyLineupResponse;
    windows?: RatingWindowResponse[];
    standings?: StandingResponse[];
    matches?: MatchSummaryResponse[];
  }

  async function renderHome(data: Data | null, followUp: () => void = () => undefined) {
    const result = await render(LeagueHomePage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    await loadSession();
    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    const reply = (path: string, body: object) => {
      const request = http.expectOne(`/api/leagues/l2/${path}`);
      if (data === null) {
        request.flush(null, { status: 500, statusText: 'Error' });
      } else {
        request.flush(body);
      }
    };
    reply('lineup', data?.lineup ?? lineup());
    reply('ratings', data?.windows ?? []);
    reply('standings', data?.standings ?? []);
    reply('matches', data?.matches ?? [CLOSED_SUMMARY, SCHEDULED_SUMMARY]);
    // The reads that depend on those start in a later round of effects.
    for (let round = 0; round < 3; round++) {
      TestBed.tick();
      await new Promise((resolve) => setTimeout(resolve));
    }
    followUp();
    await result.fixture.whenStable();
    return result;
  }

  it('leads with the next match, the lineup and the place in the standings, and offers to edit the lineup', async () => {
    await renderHome({ standings: [standing(), standing({ userId: 'u2', position: 1 })] }, () => {
      http.expectOne('/api/leagues/l2/managers/u1/history').flush([
        {
          matchId: 'm11',
          matchday: 11,
          kickoffAt: '',
          points: 38,
          inStandings: true,
          overBudget: false,
          players: [],
        },
      ]);
    });

    expect(screen.getByText('En la jornada 11')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Jornada 12' })).toBeInTheDocument();
    expect(screen.getByText(/Contra CD Arrabal/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Tu alineación/ })).toHaveTextContent('1 de 2 cambios');
    expect(screen.getByRole('link', { name: /Tu alineación/ })).toHaveTextContent('96,2');
    expect(screen.getByText('+38')).toBeInTheDocument();
    expect(screen.getByText(/2\.º/)).toHaveTextContent('de 2');
    expect(screen.getByRole('link', { name: 'Editar alineación' })).toHaveAttribute(
      'href',
      '/leagues/l2/lineup',
    );
  });

  it('asks to rate the matchday when a window is open with players left, and it becomes the main action', async () => {
    await renderHome({ windows: [window()], matches: [CLOSED_SUMMARY, SCHEDULED_SUMMARY] });

    expect(screen.getByRole('link', { name: /Valora la jornada 11/ })).toHaveTextContent('3 de 9');
    expect(screen.getByRole('link', { name: 'Valorar la jornada 11' })).toHaveAttribute(
      'href',
      '/leagues/l2/ratings/m11',
    );
    expect(screen.queryByRole('link', { name: 'Editar alineación' })).not.toBeInTheDocument();
  });

  it('asks to make a lineup when the user never saved one', async () => {
    await renderHome({ lineup: lineup({ savedAt: null, players: [] }), matches: [] });

    expect(screen.getByRole('link', { name: /Aún no tienes alineación/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Hacer alineación' })).toBeInTheDocument();
  });

  it('shows the warnings of the lineup', async () => {
    await renderHome({ lineup: lineup({ warnings: ['OVER_BUDGET'] }), matches: [] });

    expect(screen.getByText(/te pasas del presupuesto/)).toBeInTheDocument();
  });

  it('says the lineups are locking until the server has them, and then links to them', async () => {
    await renderHome(
      {
        matches: [SCHEDULED_SUMMARY, { ...SCHEDULED_SUMMARY, id: 'm13', matchday: 13 }],
        lineup: lineup({ targetMatch: { ...NEXT, id: 'm13', matchday: 13 } }),
      },
      () => {
        http
          .expectOne('/api/leagues/l2/lineups/m12')
          .flush(
            { status: 409, code: 'LINEUPS_NOT_LOCKED' },
            { status: 409, statusText: 'Conflict' },
          );
      },
    );

    expect(screen.getByText('Bloqueándose…')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Alineaciones de la jornada 12/ })).toHaveAttribute(
      'href',
      '/leagues/l2/lineups/m12',
    );
  });

  it('shows an empty state without matches or points, and no action', async () => {
    await renderHome({ lineup: lineup({ targetMatch: null }), matches: [] });

    expect(screen.getByText(/Aún no hay partidos/)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /alineación/i })).not.toBeInTheDocument();
  });

  it('offers to retry when it cannot be loaded', async () => {
    await renderHome(null);

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
