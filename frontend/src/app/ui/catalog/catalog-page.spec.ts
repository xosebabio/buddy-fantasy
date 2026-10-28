import { render, screen } from '@testing-library/angular';

import { CatalogPage } from './catalog-page';

describe('CatalogPage', () => {
  it('shows the components in both themes', async () => {
    await render(CatalogPage, { routes: [] });

    expect(screen.getByRole('heading', { name: 'Catálogo de componentes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Claro' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Oscuro' })).toBeInTheDocument();
  }, 15_000);
});
