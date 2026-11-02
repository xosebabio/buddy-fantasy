import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type {
  MatchAuditEntryResponse,
  MatchSnapshotResponse,
} from '../../../core/api/match/match-api';
import { CLOSED_MATCH, STRIKER } from '../../../shared/roster/matches.testing';
import { MEMBERS } from '../../../shared/roster/members.testing';
import { ALBERTO, DIEGO, PABLO } from '../../../shared/roster/players.testing';
import { AdminAuditPage } from './admin-audit-page';

const SNAPSHOT: MatchSnapshotResponse = {
  matchday: 11,
  opponent: 'Racing Vilariño',
  venue: 'HOME',
  kickoffAt: '2026-10-26T10:00:00Z',
  status: 'CLOSED',
  closedAt: '2026-10-26T12:00:00Z',
  result: { goalsFor: 3, goalsAgainst: 1 },
  performances: [STRIKER],
};

const AUDIT: MatchAuditEntryResponse[] = [
  {
    action: 'CORRECTED',
    actorUserId: 'u1',
    occurredAt: '2026-10-27T09:42:00Z',
    before: { ...SNAPSHOT, result: { goalsFor: 3, goalsAgainst: 2 } },
    after: SNAPSHOT,
  },
  {
    action: 'SCHEDULED',
    actorUserId: 'gone',
    occurredAt: '2026-10-12T07:30:00Z',
    before: null,
    after: { ...SNAPSHOT, status: 'SCHEDULED', closedAt: null, result: null, performances: [] },
  },
];

describe('AdminAuditPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage() {
    const result = await render(AdminAuditPage, {
      inputs: { leagueId: 'l1', matchId: 'm11' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  function answer(audit: MatchAuditEntryResponse[]): void {
    http.expectOne('/api/leagues/l1/matches/m11').flush(CLOSED_MATCH);
    http.expectOne('/api/leagues/l1/matches/m11/audit').flush(audit);
    http.expectOne((r) => r.url === '/api/leagues/l1/players').flush([DIEGO, PABLO, ALBERTO]);
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS);
  }

  it('lists the changes with who made them, and opens what each one changed', async () => {
    const result = await renderPage();
    answer(AUDIT);
    await result.fixture.whenStable();

    expect(screen.getByRole('heading', { level: 1, name: 'Auditoría' })).toBeInTheDocument();
    expect(screen.getByText('Jornada 11, contra Racing Vilariño')).toBeInTheDocument();
    const rows = screen.getAllByRole('button', { expanded: false });
    expect(rows[0]).toHaveTextContent('Corrigió el acta');
    expect(rows[0]).toHaveTextContent('Xosé Babío');
    expect(rows[1]).toHaveTextContent('Programó el partido');
    expect(rows[1]).toHaveTextContent('Antiguo miembro');

    await userEvent.click(rows[0] ?? document.body);

    expect(screen.getByText('Resultado')).toBeInTheDocument();
    expect(screen.getByText('3–2').tagName).toBe('S');
    expect(screen.getByText('3–1').tagName).toBe('B');
  });

  it('says there is nothing when the match has no changes', async () => {
    const result = await renderPage();
    answer([]);
    await result.fixture.whenStable();

    expect(screen.getByText('Aún no hay cambios en este partido.')).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    const result = await renderPage();
    http.expectOne('/api/leagues/l1/matches/m11').flush(CLOSED_MATCH);
    http
      .expectOne('/api/leagues/l1/matches/m11/audit')
      .flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne((r) => r.url === '/api/leagues/l1/players').flush([]);
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS);
    await result.fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
