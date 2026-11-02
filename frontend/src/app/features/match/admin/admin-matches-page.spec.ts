import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import type { MatchSummaryResponse } from '../../../core/api/match/match-api';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../../shared/roster/matches.testing';
import { AdminMatchesPage } from './admin-matches-page';

describe('AdminMatchesPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  async function renderPage(matches: MatchSummaryResponse[] | 'error') {
    const result = await render(AdminMatchesPage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    const request = http.expectOne('/api/leagues/l1/matches');
    if (matches === 'error') {
      request.flush(null, { status: 500, statusText: 'Server Error' });
    } else {
      request.flush(matches);
    }
    await result.fixture.whenStable();
    return result;
  }

  it('lists the matches, the newest matchday first, each with its state and a link to administer it', async () => {
    vi.useFakeTimers({ now: new Date('2026-11-02T11:00:00Z'), toFake: ['Date'] });
    const next: MatchSummaryResponse = {
      ...SCHEDULED_SUMMARY,
      id: 'm13',
      matchday: 13,
      kickoffAt: '2026-11-09T10:00:00Z',
    };
    await renderPage([CLOSED_SUMMARY, next, SCHEDULED_SUMMARY]);

    const rows = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Jornada 13, contra CD Arrabal');
    expect(rows[0]).toHaveTextContent('Programado');
    expect(rows[1]).toHaveTextContent('Jornada 12, contra CD Arrabal');
    expect(rows[1]).toHaveTextContent('Acta pendiente');
    expect(rows[2]).toHaveTextContent('3–1');
    expect(rows[2]).toHaveTextContent('Cerrado');
    expect(screen.getByRole('link', { name: /Jornada 12/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/matches/m12',
    );
    expect(screen.getByRole('link', { name: 'Programar partido' })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/matches/new',
    );
  });

  it('invites to schedule the first match when the calendar is empty', async () => {
    await renderPage([]);

    expect(screen.getByText('Aún no hay partidos. Programa el primero.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Programar partido' })).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    await renderPage('error');

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
