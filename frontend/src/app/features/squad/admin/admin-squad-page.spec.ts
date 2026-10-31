import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { PLAYERS } from '../../../shared/roster/players.testing';
import { AdminSquadPage } from './admin-squad-page';

describe('AdminSquadPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderPage() {
    const result = await render(AdminSquadPage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  function squad(players = PLAYERS) {
    http.expectOne((request) => request.url === '/api/leagues/l1/players').flush(players);
  }

  it('lists the active players by position, each one linking to their edition', async () => {
    const { fixture } = await renderPage();
    squad();
    await fixture.whenStable();

    expect(screen.getByRole('heading', { name: 'Porteros' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Pablo/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/squad/p9',
    );
    expect(screen.getByRole('link', { name: 'Dar de alta jugador' })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/squad/new',
    );
  });

  it('keeps the retired players folded apart from the squad', async () => {
    const { fixture } = await renderPage();
    squad();
    await fixture.whenStable();

    const retired = screen.getByRole('button', { name: /Dados de baja/ });
    expect(retired).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByRole('link', { name: /Xurxo/, hidden: true })).toBeInTheDocument();

    await userEvent.click(retired);

    expect(retired).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('link', { name: /Xurxo/ })).toBeVisible();
  });

  it('has no retired section when nobody is retired, and invites to register the first player', async () => {
    const { fixture } = await renderPage();
    squad([]);
    await fixture.whenStable();

    expect(screen.queryByRole('button', { name: /Dados de baja/ })).not.toBeInTheDocument();
    expect(screen.getByText('Aún no hay jugadores. Da de alta al primero.')).toBeInTheDocument();
  });

  it('offers to retry when the squad does not load', async () => {
    const { fixture } = await renderPage();
    http
      .expectOne((request) => request.url === '/api/leagues/l1/players')
      .flush({}, { status: 500, statusText: 'x' });
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
