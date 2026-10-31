import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { BROWSER_LOCATION, GOOGLE_LOGIN_URL } from '../../core/auth/session-store';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  const location = { assign: vi.fn() };

  beforeEach(() => {
    location.assign.mockReset();
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  async function renderLogin(error?: string) {
    return render(LoginPage, {
      inputs: error === undefined ? {} : { error },
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BROWSER_LOCATION, useValue: location },
      ],
    });
  }

  it('presents the app with its four bibs, hidden from screen readers', async () => {
    const { container } = await renderLogin();

    expect(
      screen.getByRole('heading', { level: 1, name: 'El fantasy de tu equipo' }),
    ).toBeInTheDocument();
    const bibs = container.querySelectorAll('bf-bib');
    expect([...bibs].map((bib) => bib.textContent)).toEqual(['1', '4', '8', '9']);
    expect([...bibs].every((bib) => bib.getAttribute('aria-hidden') === 'true')).toBe(true);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('logs in with Google', async () => {
    await renderLogin();

    await userEvent.click(screen.getByRole('button', { name: 'Entrar con Google' }));

    expect(location.assign).toHaveBeenCalledWith(GOOGLE_LOGIN_URL);
  });

  it('says that the login failed when Google comes back with an error', async () => {
    await renderLogin('');

    expect(screen.getByRole('alert')).toHaveTextContent(
      'No se pudo entrar con Google. Vuelve a intentarlo.',
    );
  });

  it('cannot log in without a network', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);

    await renderLogin();

    expect(screen.getByRole('button', { name: 'Entrar con Google' })).toBeDisabled();
  });
});
