import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import { TermsPage } from './terms-page';

describe('TermsPage', () => {
  async function renderPage() {
    return render(TermsPage, { providers: [provideRouter([])] });
  }

  it('presents the terms with the rules of leagues and roles', async () => {
    await renderPage();

    expect(
      screen.getByRole('heading', { level: 1, name: 'Condiciones de servicio' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Ligas y roles' })).toBeInTheDocument();
    expect(
      screen.getByText(/Si borras tu cuenta como administrador, la liga se cierra/),
    ).toBeInTheDocument();
  });

  it('links to the privacy policy where the terms talk about the data, and from its foot', async () => {
    await renderPage();

    const links = screen.getAllByRole('link', { name: /política de privacidad/i });
    expect(links).toHaveLength(3);
    for (const link of links) {
      expect(link).toHaveAttribute('href', '/privacy');
    }
  });
});
