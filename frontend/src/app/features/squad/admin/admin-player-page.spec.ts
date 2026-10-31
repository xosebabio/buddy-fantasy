import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { ALBERTO, XURXO } from '../../../shared/roster/players.testing';
import { Toasts } from '../../../ui/toast/toasts';
import { AdminPlayerPage } from './admin-player-page';

const LIMITS = { nameMaxLength: 30, shirtNumberMin: 0, shirtNumberMax: 99 };

describe('AdminPlayerPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderPage(playerId?: string, loaded = ALBERTO) {
    const result = await render(AdminPlayerPage, {
      inputs: { leagueId: 'l1', playerId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    if (playerId !== undefined) {
      http.expectOne(`/api/leagues/l1/players/${playerId}`).flush(loaded);
    }
    http.expectOne('/api/leagues/l1/players/limits').flush(LIMITS);
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  function answer(method: string, url: string, response: object, status = 200) {
    const request = http.expectOne({ method, url });
    request.flush(response, { status, statusText: 'x' });
    const body: unknown = request.request.body;
    return body;
  }

  it('registers a player with the limits as hints and goes back to the squad', async () => {
    await renderPage();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    expect(screen.getByText('Hasta 30 caracteres')).toBeInTheDocument();
    expect(screen.getByText('Opcional, entre 0 y 99')).toBeInTheDocument();

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), ' Rubén ');
    await userEvent.type(screen.getByRole('textbox', { name: 'Dorsal' }), '9');
    await userEvent.click(screen.getByRole('radio', { name: 'Delantero' }));
    await userEvent.type(screen.getByRole('textbox', { name: 'Valor inicial (M)' }), '13,5');
    await userEvent.click(screen.getByRole('button', { name: 'Dar de alta jugador' }));

    const body = answer('POST', '/api/leagues/l1/players', ALBERTO);
    expect(body).toEqual({
      name: 'Rubén',
      shirtNumber: 9,
      position: 'FORWARD',
      initialValue: 13.5,
    });
    expect(toasts()).toEqual(['Jugador dado de alta']);
    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1', 'admin', 'squad']);
  });

  it('does not offer to retire a player that is not registered yet', async () => {
    await renderPage();

    expect(screen.queryByRole('button', { name: 'Dar de baja' })).not.toBeInTheDocument();
  });

  it('paints a repeated shirt number in its field', async () => {
    await renderPage();
    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Rubén');
    await userEvent.type(screen.getByRole('textbox', { name: 'Valor inicial (M)' }), '10');
    await userEvent.click(screen.getByRole('button', { name: 'Dar de alta jugador' }));

    answer('POST', '/api/leagues/l1/players', { code: 'SHIRT_NUMBER_TAKEN', status: 409 }, 409);

    expect(await screen.findByText('Ya hay otro jugador con ese dorsal.')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Dorsal' })).toBeInvalid();
    expect(screen.getByRole('alert')).toHaveTextContent('Revisa los campos marcados');
  });

  it('does not send a shirt number out of the limits nor a value that is not a number', async () => {
    await renderPage();
    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Rubén');
    await userEvent.type(screen.getByRole('textbox', { name: 'Dorsal' }), '100');
    await userEvent.type(screen.getByRole('textbox', { name: 'Valor inicial (M)' }), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Dar de alta jugador' }));

    expect(screen.getByText('Escribe un dorsal entre 0 y 99.')).toBeInTheDocument();
    expect(screen.getByText('Escribe un número.')).toBeInTheDocument();
  });

  it('edits a player with their data loaded', async () => {
    await renderPage('p7');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveValue('Alberto');
    expect(screen.getByRole('textbox', { name: 'Valor inicial (M)' })).toHaveValue('12');
    expect(screen.getByRole('radio', { name: 'Delantero' })).toBeChecked();

    await userEvent.clear(screen.getByRole('textbox', { name: 'Dorsal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar jugador' }));

    const body = answer('PUT', '/api/leagues/l1/players/p7', ALBERTO);
    expect(body).toEqual({
      name: 'Alberto',
      shirtNumber: null,
      position: 'FORWARD',
      initialValue: 12,
    });
    expect(toasts()).toEqual(['Jugador guardado']);
    expect(navigate).toHaveBeenCalled();
  });

  it('is unsaved only while the form differs from the player', async () => {
    const { fixture } = await renderPage('p7');
    const page = fixture.componentInstance;
    expect(page.unsaved()).toBe(false);

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), 'o');
    expect(page.unsaved()).toBe(true);

    await userEvent.type(screen.getByRole('textbox', { name: 'Nombre' }), '{Backspace}');
    expect(page.unsaved()).toBe(false);
  });

  it('asks before retiring and goes back to the squad', async () => {
    await renderPage('p7');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await userEvent.click(screen.getByRole('button', { name: 'Dar de baja' }));
    expect(screen.getByRole('alertdialog', { name: 'Dar de baja a Alberto' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    http.expectNone({ method: 'DELETE', url: '/api/leagues/l1/players/p7' });

    await userEvent.click(screen.getByRole('button', { name: 'Dar de baja' }));
    await userEvent.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Dar de baja' }),
    );

    answer('DELETE', '/api/leagues/l1/players/p7', {}, 204);
    expect(toasts()).toEqual(['Jugador dado de baja']);
    expect(navigate).toHaveBeenCalled();
  });

  it('shows a retired player read-only, without actions', async () => {
    await renderPage('p5', XURXO);

    expect(screen.getByText('Dado de baja')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Nombre' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Guardar jugador' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Dar de baja' })).not.toBeInTheDocument();
  });

  it('reloads the player and explains it when they were retired meanwhile', async () => {
    const { fixture } = await renderPage('p7');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar jugador' }));

    answer('PUT', '/api/leagues/l1/players/p7', { code: 'PLAYER_RETIRED', status: 409 }, 409);

    expect(toasts()).toEqual(['El jugador está dado de baja.']);
    const player = await vi.waitFor(() => http.expectOne('/api/leagues/l1/players/p7'));
    player.flush({ ...ALBERTO, retiredAt: '2026-10-31T10:00:00Z' });
    http.expectOne('/api/leagues/l1/players/limits').flush(LIMITS);
    await fixture.whenStable();
    expect(screen.getByRole('textbox', { name: 'Nombre' })).toBeDisabled();
  });
});
