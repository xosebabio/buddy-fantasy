import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import { loadSession, USER } from '../../core/auth/session.testing';
import { LeaguesPage } from './leagues-page';

describe('LeaguesPage', () => {
  async function renderLeagues(leagues?: Parameters<typeof loadSession>[1]) {
    const result = await render(LeaguesPage, {
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession(USER, leagues);
    await result.fixture.whenStable();
    return result;
  }

  it('lists the user’s leagues, marking those they administer', async () => {
    await renderLeagues();

    const list = screen.getByRole('list');
    const admin = within(list).getByRole('link', { name: /Os Domingueiros/ });
    const manager = within(list).getByRole('link', { name: /Veteranos do Sar/ });
    expect(admin).toHaveAttribute('href', '/leagues/l1');
    expect(admin).toHaveTextContent('Admin');
    expect(manager).toHaveAttribute('href', '/leagues/l2');
    expect(manager).toHaveTextContent('Manager');
    expect(screen.getByRole('link', { name: 'Crear liga' })).toHaveAttribute(
      'href',
      '/leagues/new',
    );
    expect(
      screen.getByText('¿Te han invitado? Abre el enlace que te pasaron.'),
    ).toBeInTheDocument();
  });

  it('invites to create a league or ask for a link without any', async () => {
    await renderLeagues([]);

    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    expect(screen.getByText(/Aún no juegas en ninguna liga/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Crear liga' })).toHaveAttribute(
      'href',
      '/leagues/new',
    );
  });
});
