import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { LeagueSettingsBody } from '../../../core/api/league/league-api';
import { Toasts } from '../../../ui/toast/toasts';
import { AdminSettingsPage } from './admin-settings-page';

const SETTINGS: LeagueSettingsBody = {
  ratingWindowHours: 24,
  ratingFormat: 'SPADES',
  ratingSpadesFactor: 4,
  ratingSpadesBase: -2,
  ratingDecimalFactor: 1,
  ratingDecimalBase: 0,
  ratingMinVotes: 1,
  defaultRating: null,
  lineupLockHours: 2,
  lineupMaxChanges: 2,
  lineupSize: 7,
  lineupFormationEnabled: false,
  lineupMinGoalkeepers: 1,
  lineupMinDefenders: 2,
  lineupMinMidfielders: 2,
  lineupMinForwards: 1,
  lineupBudgetEnabled: true,
  lineupBudget: 100,
  marketK: 0.05,
  marketMaxChangePct: 10,
  marketMinValue: 1,
  scoringRules: {
    GOAL: { GOALKEEPER: 6, DEFENDER: 5, MIDFIELDER: 4, FORWARD: 3 },
    OWN_GOAL: { GOALKEEPER: -2, DEFENDER: -2, MIDFIELDER: -2, FORWARD: -2 },
  },
};

describe('AdminSettingsPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderPage() {
    const result = await render(AdminSettingsPage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/leagues/l1/settings').flush(SETTINGS);
    http.expectOne('/api/leagues/l1/settings/limits').flush({ lineupLockHoursMax: 48 });
    await result.fixture.whenStable();
    return result;
  }

  function toasts(): string[] {
    return TestBed.inject(Toasts)
      .toasts()
      .map((t) => t.message);
  }

  function put() {
    const request = http.expectOne({ method: 'PUT', url: '/api/leagues/l1/settings' });
    const body: unknown = request.request.body;
    return {
      body,
      flush: (response: object, status = 200) => {
        request.flush(response, { status, statusText: 'x' });
      },
    };
  }

  it('shows the settings, the limit of the lock and the table of points', async () => {
    await renderPage();

    expect(screen.getByRole('spinbutton', { name: 'Jugadores por alineación' })).toHaveTextContent(
      '7',
    );
    expect(screen.getByText('Entre 0 y 48 horas')).toBeInTheDocument();
    expect(
      screen.getByRole('spinbutton', { name: 'Horas de bloqueo antes del partido' }),
    ).toHaveTextContent('2');
    expect(screen.getByRole('switch', { name: 'Presupuesto' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Formación por posiciones' })).not.toBeChecked();
    expect(screen.getByRole('button', { name: /Gol en propia/ })).toHaveTextContent('−2−2−2−2');
  });

  it('only shows the minimums and the mark when they are switched on', async () => {
    await renderPage();
    expect(screen.queryByRole('spinbutton', { name: 'Porteros mínimos' })).not.toBeInTheDocument();
    expect(screen.queryByRole('spinbutton', { name: 'Nota sin votos' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('switch', { name: 'Formación por posiciones' }));
    await userEvent.click(screen.getByRole('switch', { name: 'Nota sin votos' }));

    expect(screen.getByRole('spinbutton', { name: 'Porteros mínimos' })).toHaveTextContent('1');
    expect(screen.getByRole('spinbutton', { name: 'Nota sin votos' })).toHaveTextContent('0');
  });

  it('shows the conversion of the chosen format and drops the mark when it changes', async () => {
    await renderPage();
    await userEvent.click(screen.getByRole('switch', { name: 'Nota sin votos' }));
    expect(screen.getByRole('textbox', { name: 'Factor' })).toHaveValue('4');

    await userEvent.click(screen.getByRole('radio', { name: 'Nota' }));

    expect(screen.getByRole('textbox', { name: 'Factor' })).toHaveValue('1');
    expect(screen.getByRole('switch', { name: 'Nota sin votos' })).not.toBeChecked();
  });

  it('opens one action of the table at a time', async () => {
    await renderPage();
    const goal = screen.getByRole('button', { name: /^Gol \d/ });
    const ownGoal = screen.getByRole('button', { name: /Gol en propia/ });

    await userEvent.click(goal);
    expect(screen.getByRole('spinbutton', { name: 'Gol, Delantero' })).toBeVisible();

    await userEvent.click(ownGoal);
    expect(goal).toHaveAttribute('aria-expanded', 'false');
    expect(
      screen.getByRole('spinbutton', { name: 'Gol en propia puerta, Delantero' }),
    ).toBeVisible();
  });

  it('saves the whole body with the change, the market untouched', async () => {
    const { fixture } = await renderPage();
    await userEvent.click(screen.getByRole('button', { name: /^Gol \d/ }));
    await userEvent.click(
      within(
        screen.getByRole('spinbutton', { name: 'Gol, Delantero' }).parentElement ?? document.body,
      ).getByRole('button', { name: 'Subir', hidden: true }),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Guardar parámetros' }));

    const request = put();
    expect(request.body).toMatchObject({
      marketK: 0.05,
      lineupSize: 7,
      scoringRules: { GOAL: { GOALKEEPER: 6, DEFENDER: 5, MIDFIELDER: 4, FORWARD: 4 } },
    });
    request.flush(SETTINGS);
    await fixture.whenStable();

    expect(toasts()).toEqual(['Parámetros guardados']);
  });

  it('knows about unsaved changes until it saves', async () => {
    const { fixture } = await renderPage();
    const page = fixture.componentInstance;
    expect(page.unsaved()).toBe(false);

    await userEvent.click(screen.getByRole('switch', { name: 'Presupuesto' }));
    expect(page.unsaved()).toBe(true);

    await userEvent.click(screen.getByRole('switch', { name: 'Presupuesto' }));
    expect(page.unsaved()).toBe(false);

    await userEvent.click(screen.getByRole('switch', { name: 'Presupuesto' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar parámetros' }));
    const request = put();
    expect(request.body).toMatchObject({ lineupBudgetEnabled: false });
    request.flush({ ...SETTINGS, lineupBudgetEnabled: false });
    await fixture.whenStable();

    expect(page.unsaved()).toBe(false);
    expect(screen.getByRole('switch', { name: 'Presupuesto' })).not.toBeChecked();
  });

  it('paints a 400 under its control and in a notice', async () => {
    const { fixture } = await renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar parámetros' }));

    put().flush({ status: 400, code: 'INVALID_LINEUP_LOCK_HOURS' }, 400);
    await fixture.whenStable();

    expect(
      screen.getByText('Las horas de bloqueo de la alineación no son válidas.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Revisa los campos marcados');
  });

  it('puts a code without a control in the notice', async () => {
    const { fixture } = await renderPage();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar parámetros' }));

    put().flush({ status: 400, code: 'SCORING_TABLE_INCOMPLETE' }, 400);
    await fixture.whenStable();

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Faltan los puntos de alguna acción o posición.',
    );
  });

  it('does not send a budget that is not a number', async () => {
    await renderPage();
    const amount = screen.getByRole('textbox', { name: 'Importe (M)' });
    await userEvent.clear(amount);
    await userEvent.type(amount, 'mucho');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar parámetros' }));

    http.expectNone({ method: 'PUT', url: '/api/leagues/l1/settings' });
    expect(screen.getByText('Escribe un número.')).toBeInTheDocument();
  });

  it('offers to retry when the settings do not load', async () => {
    const result = await render(AdminSettingsPage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/api/leagues/l1/settings')
      .error(new ProgressEvent('error'), { status: 500, statusText: 'Server error' });
    http.expectOne('/api/leagues/l1/settings/limits').flush({ lineupLockHoursMax: 48 });
    await result.fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
