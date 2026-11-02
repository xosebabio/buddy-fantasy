import { Location } from '@angular/common';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { screen } from '@testing-library/angular';

import { appConfig } from './app.config';
// The lazy pages, loaded with the test file: their first dynamic import is too slow for a test.
import './core/layout/shell';
import './core/pages/error-page';
import './features/league/create-league-page';
import './features/league/members-page';
import './features/legal/privacy-page';
import './features/legal/terms-page';
import { answerSession, USER } from './core/auth/session.testing';
import { SessionStore } from './core/auth/session-store';
import { routes } from './app.routes';

describe('routes', () => {
  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    const loaded = TestBed.inject(SessionStore).load();
    answerSession(TestBed.inject(HttpTestingController), USER);
    await loaded;
  });

  it('shows «No encontrado» in the shell for a route that does not exist', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/nothing/here');

    expect(screen.getByRole('heading', { level: 1, name: 'No encontrado' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tu perfil' })).toBeInTheDocument();
    expect(document.title).toBe('No encontrado – Buddy Fantasy');
  });

  it('takes leagues/new for the page that creates a league, not for a league', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/leagues/new');

    expect(screen.getByRole('heading', { level: 1, name: 'Crear liga' })).toBeInTheDocument();
    expect(document.title).toBe('Crear liga – Buddy Fantasy');
  });

  it('shows a page of a league in its shell', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/leagues/l2/members');

    expect(screen.getByRole('heading', { level: 1, name: 'Miembros' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Veteranos do Sar, cambiar de liga' }),
    ).toBeInTheDocument();
  });

  it('shows «No encontrado» for a league that is not the user’s, keeping its URL', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/leagues/other');

    expect(screen.getByRole('heading', { level: 1, name: 'No encontrado' })).toBeInTheDocument();
    expect(TestBed.inject(Location).path()).toBe('/leagues/other');
  });

  it('shows «Sin acceso» to a manager in an admin page, keeping its URL', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/leagues/l2/admin/members');

    expect(screen.getByRole('heading', { level: 1, name: 'Sin acceso' })).toBeInTheDocument();
    expect(TestBed.inject(Location).path()).toBe('/leagues/l2/admin/members');
  });

  it('shows the privacy policy outside the shell, with its title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/privacy');

    expect(
      screen.getByRole('heading', { level: 1, name: 'Política de privacidad' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Tu perfil' })).not.toBeInTheDocument();
    expect(document.title).toBe('Política de privacidad – Buddy Fantasy');
  });

  it('shows the terms of service outside the shell, with its title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/terms');

    expect(
      screen.getByRole('heading', { level: 1, name: 'Condiciones de servicio' }),
    ).toBeInTheDocument();
    expect(document.title).toBe('Condiciones de servicio – Buddy Fantasy');
  });

  it('leaves the legal pages without any guard, so that anyone can read them', () => {
    const legal = routes.filter((route) => ['privacy', 'terms'].includes(route.path ?? ''));

    expect(legal.map((route) => route.path)).toEqual(['privacy', 'terms']);
    for (const route of legal) {
      expect(route.canActivate).toBeUndefined();
      expect(route.canMatch).toBeUndefined();
    }
  });
});
