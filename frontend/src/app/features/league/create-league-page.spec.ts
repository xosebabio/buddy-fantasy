import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { LEAGUES, loadSession } from '../../core/auth/session.testing';
import { SessionStore } from '../../core/auth/session-store';
import { CreateLeaguePage } from './create-league-page';

describe('CreateLeaguePage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderCreate() {
    const result = await render(CreateLeaguePage, {
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession();
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  async function create(name: string) {
    if (name !== '') {
      await userEvent.type(screen.getByLabelText('Nombre de la liga'), name);
    }
    await userEvent.click(screen.getByRole('button', { name: 'Crear liga' }));
    return http.expectOne('/api/leagues');
  }

  it('creates the league, reloads the user’s leagues and shows the invitation link', async () => {
    const { fixture } = await renderCreate();

    const request = await create('Fútbol 7 Martes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Fútbol 7 Martes' });
    request.flush({ id: 'l3', invitationToken: 'tok-3' });
    const created = { leagueId: 'l3', name: 'Fútbol 7 Martes', role: 'ADMIN' } as const;
    http.expectOne('/api/me/leagues').flush([...LEAGUES, created]);
    await fixture.whenStable();

    expect(TestBed.inject(SessionStore).league('l3')).toEqual(created);
    expect(screen.getByRole('heading', { level: 1, name: 'Invita a tu equipo' })).toHaveFocus();
    expect(screen.getByLabelText('Enlace de invitación')).toHaveValue(
      `${location.origin}/invite/tok-3`,
    );
    expect(screen.getByRole('link', { name: 'Ir a la liga' })).toHaveAttribute(
      'href',
      '/leagues/l3',
    );
    expect(await screen.findByText('Liga creada')).toBeInTheDocument();
  });

  it('shows the server’s reason for a name it rejects in its field', async () => {
    const { fixture } = await renderCreate();

    (await create('x'.repeat(200))).flush(
      { status: 400, code: 'INVALID_LEAGUE_NAME' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    const input = screen.getByLabelText('Nombre de la liga');
    expect(input).toHaveAccessibleDescription(
      expect.stringContaining('El nombre de la liga está vacío o es demasiado largo.'),
    );
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('button', { name: 'Crear liga' })).toBeEnabled();
  });

  it('asks for the name when it is missing', async () => {
    const { fixture } = await renderCreate();

    (await create('')).flush(
      { status: 400, violations: ['name: must not be blank'] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(screen.getByLabelText('Nombre de la liga')).toHaveAccessibleDescription(
      expect.stringContaining('Escribe el nombre de la liga.'),
    );
  });

  it('cannot create without a network', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);

    await renderCreate();

    expect(screen.getByRole('button', { name: 'Crear liga' })).toBeDisabled();
  });
});
