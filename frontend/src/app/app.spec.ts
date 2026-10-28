import { ChangeDetectionStrategy, Component } from '@angular/core';
import { render, screen } from '@testing-library/angular';

import { App } from './app';

@Component({
  selector: 'bf-test-page',
  template: '<h1>Página de prueba</h1>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestPage {}

describe('App', () => {
  it('renders the routed page', async () => {
    const { navigate } = await render(App, { routes: [{ path: 'test', component: TestPage }] });

    await navigate('/test');

    expect(await screen.findByRole('heading', { name: 'Página de prueba' })).toBeInTheDocument();
  });
});
