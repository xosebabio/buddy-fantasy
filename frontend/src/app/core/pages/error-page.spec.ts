import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';

import { ErrorPage } from './error-page';

describe('ErrorPage', () => {
  it.each([
    ['forbidden', 'Sin acceso', /No puedes ver esta página/],
    ['not-found', 'No encontrado', /Esta página no existe/],
  ] as const)('explains a %s and leads back to the start', async (kind, title, message) => {
    await render(ErrorPage, { inputs: { kind }, providers: [provideRouter([])] });

    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
    expect(screen.getByText(message)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir al inicio' })).toHaveAttribute('href', '/');
  });
});
