import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import { loadSession } from '../../core/auth/session.testing';
import { LeagueIndexPage } from './league-index-page';

describe('LeagueIndexPage', () => {
  async function renderIndex(leagueId: string) {
    const result = await render(LeagueIndexPage, {
      inputs: { leagueId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession();
    await result.fixture.whenStable();
    return result;
  }

  it('links to the squad, the calendar, the rules and the members of the league', async () => {
    await renderIndex('l2');

    expect(screen.getByRole('heading', { level: 1, name: 'Liga' })).toBeInTheDocument();
    expect(screen.getByText('Veteranos do Sar')).toBeInTheDocument();
    const list = screen.getByRole('list');
    expect(within(list).getByRole('link', { name: /^Plantilla/ })).toHaveAttribute(
      'href',
      '/leagues/l2/squad',
    );
    expect(within(list).getByRole('link', { name: /^Calendario/ })).toHaveAttribute(
      'href',
      '/leagues/l2/matches',
    );
    expect(within(list).getByRole('link', { name: /^Reglas/ })).toHaveAttribute(
      'href',
      '/leagues/l2/rules',
    );
    expect(within(list).getByRole('link', { name: /^Miembros/ })).toHaveAttribute(
      'href',
      '/leagues/l2/members',
    );
  });

  it('does not show the administration to a manager', async () => {
    await renderIndex('l2');

    expect(screen.queryByRole('heading', { name: 'Administrar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Miembros e invitación/ })).not.toBeInTheDocument();
  });

  it('shows the admin the pages that manage the league', async () => {
    await renderIndex('l1');

    const admin = screen.getByRole('region', { name: 'Administrar' });
    expect(within(admin).getByRole('link', { name: /Miembros e invitación/ })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/members',
    );
  });
});
