import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { loadSession } from '../../core/auth/session.testing';
import { SessionStore } from '../../core/auth/session-store';
import { Theme } from '../../core/theme/theme';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  async function renderProfile() {
    const result = await render(ProfilePage, {
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession();
    await result.fixture.whenStable();
    return result;
  }

  it('shows the name and email of the Google account', async () => {
    await renderProfile();

    expect(screen.getByRole('heading', { level: 1, name: 'Xosé Babío' })).toBeInTheDocument();
    expect(screen.getByText('xose@example.com')).toBeInTheDocument();
  });

  it('changes the theme', async () => {
    await renderProfile();
    expect(screen.getByRole('radio', { name: 'Automático' })).toBeChecked();

    await userEvent.click(screen.getByText('Oscuro'));

    expect(TestBed.inject(Theme).preference()).toBe('dark');
    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked();
  });

  it('logs out and goes to the login', async () => {
    await renderProfile();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    const request = TestBed.inject(HttpTestingController).expectOne('/api/logout');
    expect(request.request.method).toBe('POST');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(TestBed.inject(SessionStore).user()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
