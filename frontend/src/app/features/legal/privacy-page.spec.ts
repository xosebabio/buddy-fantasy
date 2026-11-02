import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';

import { PrivacyPage } from './privacy-page';

describe('PrivacyPage', () => {
  async function renderPage() {
    return render(PrivacyPage, { providers: [provideRouter([])] });
  }

  it('presents the policy with its sections, each one with a title to link to', async () => {
    const { container } = await renderPage();

    expect(
      screen.getByRole('heading', { level: 1, name: 'Política de privacidad' }),
    ).toBeInTheDocument();
    for (const name of [
      'Quién es el responsable',
      'Qué datos tratamos',
      'Cookies y almacenamiento local',
      'Cuánto tiempo los guardamos',
      'Tus derechos',
    ]) {
      expect(screen.getByRole('heading', { level: 2, name })).toBeInTheDocument();
    }
    expect(container.querySelector('#cookies')).toHaveTextContent('Cookies y almacenamiento local');
  });

  it('explains the cookies and the local storage in a table with its own region', async () => {
    await renderPage();

    const region = screen.getByRole('region', { name: 'Cookies y almacenamiento local' });
    const rows = within(region).getAllByRole('row');
    expect(rows).toHaveLength(6);
    expect(within(region).getByRole('cell', { name: 'SESSION' })).toBeInTheDocument();
    expect(within(region).getByRole('cell', { name: 'bf.lastLeague' })).toBeInTheDocument();
  });

  it('links to the terms of service from its foot', async () => {
    await renderPage();

    expect(screen.getByRole('link', { name: 'Condiciones de servicio' })).toHaveAttribute(
      'href',
      '/terms',
    );
  });

  it('links back to the start from the logotype', async () => {
    await renderPage();

    expect(screen.getByRole('link', { name: 'BuddyFantasy' })).toHaveAttribute('href', '/');
  });
});
