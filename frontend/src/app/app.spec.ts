import { render, screen } from '@testing-library/angular';

import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  it('renders the routed page', async () => {
    const { navigate } = await render(App, { routes });

    await navigate('/_ui');

    expect(
      await screen.findByRole('heading', { name: 'Catálogo de componentes' }),
    ).toBeInTheDocument();
  });
});
