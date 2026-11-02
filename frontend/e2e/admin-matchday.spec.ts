import { type Page } from '@playwright/test';

import { apiOf, ServerClock } from './api';
import { expect, expectAccessible, logIn, newUser, test, unique } from './fixtures';
import { confirm, createLeague, reloadUntil, toast } from './pages';

/** The whole matchday of the admin (plan 13, task 14), from the interface, with the manager's side by API. */

const HOUR = 3_600_000;

/** The date and the time that the form of a match asks for, both in Madrid. */
function inMadrid(instant: Date): { date: string; time: string } {
  const parts: Record<string, string> = Object.fromEntries(
    new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Europe/Madrid',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    })
      .formatToParts(instant)
      .map(({ type, value }) => [type, value]),
  );
  const part = (type: string) => parts[type] ?? '';
  return {
    date: `${part('year')}-${part('month')}-${part('day')}`,
    time: `${part('hour')}:${part('minute')}`,
  };
}

/** Sets a stepper with the keyboard, the way a person who types the figure would; it starts empty or at any value. */
async function setStepper(page: Page, label: string, value: number): Promise<void> {
  const stepper = page.getByRole('spinbutton', { name: label, exact: true });
  await stepper.focus();
  const text = await stepper.getAttribute('aria-valuenow');
  let current = text === null ? null : Number(text);
  while (current !== value) {
    // From empty, the first step goes to 0.
    const next: number = current === null ? 0 : current + (current < value ? 1 : -1);
    await stepper.press(current === null || current < value ? 'ArrowUp' : 'ArrowDown');
    await expect(stepper).toHaveAttribute('aria-valuenow', String(next));
    current = next;
  }
  await expect(stepper).toHaveAttribute('aria-valuenow', String(value));
}

/**
 * Axe measures what the fixed bar covers: it checks the screen scrolled to its end, with the main
 * action in view, or to its start where the end would hide the row that is open.
 */
async function accessible(page: Page, edge: 'start' | 'end' = 'end'): Promise<void> {
  await page.evaluate((to) => {
    window.scrollTo(0, to === 'end' ? document.body.scrollHeight : 0);
  }, edge);
  await expectAccessible(page);
}

async function registerPlayer(
  page: Page,
  player: { name: string; shirtNumber: string; position: string; value: string },
): Promise<void> {
  await page.getByRole('link', { name: 'Dar de alta jugador' }).click();
  await expect(page.getByRole('heading', { name: 'Nuevo jugador' })).toBeVisible();
  await page.getByLabel('Nombre', { exact: true }).fill(player.name);
  await page.getByLabel('Dorsal').fill(player.shirtNumber);
  await page.getByText(player.position, { exact: true }).click();
  await page.getByLabel('Valor inicial (M)').fill(player.value);
  await page.getByRole('button', { name: 'Dar de alta jugador' }).click();
}

test('the admin plays a whole matchday from the interface: rules, squad, match, sheet, close and corrections', async ({
  openBrowser,
}) => {
  // The longest walkthrough: a league set up from scratch, two jobs to wait for and axe on every screen.
  test.setTimeout(240_000);
  const leagueName = unique('Peña del domingo');
  const manager = newUser('Brais');

  const page = await openBrowser();
  await logIn(page, newUser('Uxía'));
  const admin = apiOf(page);
  const clock = new ServerClock(admin);
  await page.clock.install({ time: await clock.now() });

  // 1. The league, from the interface, and its rules: a lineup of three without formation and a
  // forward's goal worth one more.
  const invitation = await createLeague(page, leagueName);
  await page.getByRole('link', { name: 'Ir a la liga' }).click();
  await expect(page.getByRole('button', { name: `${leagueName}, cambiar de liga` })).toBeVisible();
  const leagueId = /\/leagues\/([^/]+)/.exec(page.url())?.[1] ?? '';
  const base = `/api/leagues/${leagueId}`;

  await page.getByRole('link', { name: 'Liga', exact: true }).click();
  await page.getByRole('link', { name: /^Parámetros/ }).click();
  await expect(page.getByRole('heading', { name: 'Parámetros', exact: true })).toBeVisible();
  await expect(page.getByRole('spinbutton', { name: 'Jugadores por alineación' })).toBeVisible();
  await accessible(page);

  const size = page.getByRole('spinbutton', { name: 'Jugadores por alineación' });
  await size.focus();
  await size.press('Home');
  for (let step = 1; step < 3; step++) {
    await size.press('ArrowUp');
  }
  await expect(size).toHaveAttribute('aria-valuenow', '3');
  const formation = page.getByRole('switch', { name: 'Formación por posiciones' });
  if ((await formation.getAttribute('aria-checked')) === 'true') {
    await formation.click();
  }
  await expect(formation).toHaveAttribute('aria-checked', 'false');

  await page.getByRole('button', { name: /^Gol(?! de| en)/ }).click();
  const forwardGoal = page.getByRole('spinbutton', { name: 'Gol, Delantero', exact: true });
  await expect(forwardGoal).toBeVisible();
  const goalPoints = Number(await forwardGoal.getAttribute('aria-valuenow'));
  await forwardGoal.focus();
  await forwardGoal.press('ArrowUp');
  await expect(forwardGoal).toHaveAttribute('aria-valuenow', String(goalPoints + 1));
  // The row that opened sits under the fixed bar while it is scrolled: axe measures it from the top.
  await accessible(page, 'start');
  await page.getByRole('button', { name: 'Guardar parámetros' }).click();
  await expect(toast(page, 'Parámetros guardados')).toBeVisible();

  // 2. The invitation, from «Miembros e invitación»: the manager joins with it.
  await page.getByRole('link', { name: 'Liga', exact: true }).click();
  await page.getByRole('link', { name: /^Miembros e invitación/ }).click();
  await expect(page.getByRole('heading', { name: 'Miembros e invitación' })).toBeVisible();
  await accessible(page);
  expect(invitation).toContain('/invite/');

  const managerPage = await openBrowser();
  await managerPage.clock.install({ time: await clock.now() });
  await logIn(managerPage, manager);
  await managerPage.goto(invitation);
  await managerPage.getByRole('button', { name: 'Unirme a la liga' }).click();
  await expect(
    managerPage.getByRole('button', { name: `${leagueName}, cambiar de liga` }),
  ).toBeVisible();

  // 3. The squad: a repeated shirt number shows its error in its field, one player is edited and
  // another is retired.
  await page.getByRole('link', { name: 'Liga', exact: true }).click();
  await page.getByRole('link', { name: /^Jugadores/ }).click();
  await expect(page.getByRole('heading', { name: 'Jugadores', exact: true })).toBeVisible();
  await expect(page.getByText('Aún no hay jugadores. Da de alta al primero.')).toBeVisible();
  await accessible(page);

  const squad = [
    { name: 'Diego', shirtNumber: '1', position: 'Portero', value: '8' },
    { name: 'Marta', shirtNumber: '4', position: 'Defensa', value: '10' },
    { name: 'Lucía', shirtNumber: '8', position: 'Medio', value: '12' },
    { name: 'Rubén', shirtNumber: '9', position: 'Delantero', value: '15' },
  ];
  await page.getByRole('link', { name: 'Dar de alta jugador' }).click();
  await expect(page.getByRole('heading', { name: 'Nuevo jugador' })).toBeVisible();
  await accessible(page);
  await page.goBack();
  for (const player of squad) {
    await registerPlayer(page, player);
    await expect(toast(page, 'Jugador dado de alta').last()).toBeVisible();
    await expect(page.getByRole('link', { name: new RegExp(player.name) })).toBeVisible();
  }

  await page.getByRole('link', { name: 'Dar de alta jugador' }).click();
  await page.getByLabel('Nombre', { exact: true }).fill('Hugo');
  await page.getByLabel('Dorsal').fill('9');
  await page.getByText('Delantero', { exact: true }).click();
  await page.getByLabel('Valor inicial (M)').fill('9');
  await page.getByRole('button', { name: 'Dar de alta jugador' }).click();
  await expect(page.getByText('Ya hay otro jugador con ese dorsal.')).toBeVisible();
  await accessible(page);
  await page.getByLabel('Dorsal').fill('11');
  await page.getByRole('button', { name: 'Dar de alta jugador' }).click();
  await expect(page.getByRole('link', { name: /Hugo/ })).toBeVisible();

  await page.getByRole('link', { name: /Marta/ }).click();
  await expect(page.getByRole('heading', { name: 'Marta' })).toBeVisible();
  await page.getByLabel('Valor inicial (M)').fill('11');
  await accessible(page);
  await page.getByRole('button', { name: 'Guardar jugador' }).click();
  await expect(toast(page, 'Jugador guardado')).toBeVisible();

  await page.getByRole('link', { name: /Hugo/ }).click();
  await expect(page.getByRole('heading', { name: 'Hugo' })).toBeVisible();
  await page.getByRole('button', { name: 'Dar de baja' }).click();
  await expect(page.getByRole('alertdialog')).toBeVisible();
  await accessible(page);
  await confirm(page, 'Dar de baja');
  await expect(toast(page, 'Jugador dado de baja')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Dados de baja/ })).toBeVisible();
  await expect(page.getByRole('link', { name: /Hugo/ })).toBeHidden();

  const players = await admin.get<{ id: string; name: string }[]>(`${base}/players`);
  const idOf = (name: string) => players.find((player) => player.name === name)?.id ?? '';

  // 4. The match, a few hours from the server's time.
  const start = await clock.now();
  const kickoff = inMadrid(new Date(start.getTime() + 5 * HOUR));
  await page.getByRole('link', { name: 'Liga', exact: true }).click();
  await page.getByRole('link', { name: /^Partidos/ }).click();
  await expect(page.getByText('Aún no hay partidos. Programa el primero.')).toBeVisible();
  await accessible(page);
  await page.getByRole('link', { name: 'Programar partido' }).click();
  await expect(page.getByRole('heading', { name: 'Programar partido' })).toBeVisible();
  await page.getByLabel('Jornada').fill('1');
  await page.getByLabel('Rival').fill('CD Arrabal');
  await page.getByText('En casa', { exact: true }).click();
  await page.getByLabel('Fecha').fill(kickoff.date);
  await page.getByLabel('Hora').fill(kickoff.time);
  await accessible(page);
  await page.getByRole('button', { name: 'Programar partido' }).click();
  await expect(toast(page, 'Partido programado')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await expect(page.getByText('Se puede cerrar desde el inicio del partido.')).toBeVisible();
  await accessible(page);
  const matchUrl = page.url();
  const matchId = matchUrl.split('/').pop() ?? '';

  // 5. Both lineups, by API: the interface of the manager is already tested by matchday.spec.ts.
  await admin.put(`${base}/lineup`, {
    playerIds: [idOf('Diego'), idOf('Lucía'), idOf('Rubén')],
  });
  const managerApi = apiOf(managerPage);
  await managerApi.put(`${base}/lineup`, {
    playerIds: [idOf('Marta'), idOf('Lucía'), idOf('Rubén')],
  });

  // 6. The match starts: the home shows the sheet as pending, and the admin fills it in.
  await clock.advance('PT5H5M');
  await page.clock.setSystemTime(await clock.now());
  await page.goto(`/leagues/${leagueId}`);
  const pending = page.getByRole('link', { name: /^Acta de la jornada 1/ });
  await expect(pending).toBeVisible();
  await expect(pending).toContainText('Pendiente');
  await accessible(page);
  await pending.click();

  await expect(page.getByRole('heading', { name: 'Acta de la jornada 1' })).toBeVisible();
  await accessible(page);
  await setStepper(page, 'Goles a favor', 2);
  await setStepper(page, 'Goles en contra', 0);
  const row = (name: string) => page.getByRole('listitem').filter({ hasText: name });
  for (const name of ['Diego', 'Marta', 'Lucía', 'Rubén']) {
    await row(name).getByRole('checkbox', { name: 'Jugó' }).check();
  }
  await expect(page.getByText('4 de')).toBeVisible();
  // Hugo, retired, is no longer in the squad of the sheet.
  await expect(row('Hugo')).toHaveCount(0);
  await setStepper(page, 'Goles · Rubén', 1);
  await row('Rubén').getByRole('checkbox', { name: 'Amarilla', exact: true }).check();
  await accessible(page);
  await page.getByRole('button', { name: 'Guardar acta' }).click();
  await expect(toast(page, 'Acta guardada')).toBeVisible();

  // The goals of the players (1) do not add up to the score (2): a warning that does not stop closing.
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await expect(
    page.getByText('Los goles de los jugadores no suman los del resultado.'),
  ).toBeVisible();
  await expect(page.getByText('2–0 · Jugadores: 4')).toBeVisible();
  await accessible(page);

  await page.getByRole('button', { name: 'Cerrar partido' }).click();
  await expect(page.getByRole('alertdialog')).toBeVisible();
  await expect(page.getByRole('alertdialog')).toContainText('se abre la valoración durante');
  await accessible(page);
  await confirm(page, 'Cerrar partido');
  await expect(toast(page, 'Partido cerrado')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cerrar partido' })).toBeHidden();
  await expect(page.getByRole('link', { name: /^Valoraciones/ })).toBeVisible();

  // 7. The manager rates by API; the admin cancels their ratings and the manager sees it.
  await managerApi.put(`${base}/ratings/${matchId}`, {
    ratings: [{ playerId: idOf('Rubén'), value: 4 }],
  });
  await page.getByRole('link', { name: /^Valoraciones/ }).click();
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await accessible(page);
  await page.getByRole('button', { name: new RegExp(`${manager.displayName}$`) }).click();
  await expect(page.getByRole('alertdialog')).toContainText(manager.displayName);
  await accessible(page);
  await confirm(page, 'Cancelar valoraciones');
  await expect(toast(page, 'Valoraciones canceladas')).toBeVisible();
  await expect(page.getByText('Cancelada', { exact: true })).toBeVisible();

  await managerPage.clock.setSystemTime(await clock.now());
  await managerPage.goto(`/leagues/${leagueId}/ratings`);
  await expect(managerPage.getByText('Excluido', { exact: true })).toBeVisible();
  await expectAccessible(managerPage);

  // 8. The sheet is corrected (an assist more), and the audit shows it.
  await page.goto(`/leagues/${leagueId}/admin/matches/${matchId}/sheet`);
  await expect(page.getByText('Partido cerrado: guardar corrige el acta')).toBeVisible();
  await row('Lucía').getByRole('button').first().click();
  await setStepper(page, 'Asistencias · Lucía', 1);
  await accessible(page);
  await page.getByRole('button', { name: 'Guardar corrección' }).click();
  await expect(toast(page, 'Corrección guardada')).toBeVisible();

  await page.getByRole('link', { name: 'Auditoría' }).click();
  await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible();
  await expect(page.getByRole('button', { name: /^Corrigió el acta/ })).toBeVisible();
  await page.getByRole('button', { name: /^Corrigió el acta/ }).click();
  await expect(page.getByText(/asistencias/).first()).toBeVisible();
  await accessible(page);

  // 9. The window closes and the job scores the matchday: the points reflect the edited table and the correction.
  await clock.advance('PT25H');
  await page.clock.setSystemTime(await clock.now());
  await page.goto(`/leagues/${leagueId}/ratings/${matchId}`);
  await reloadUntil(page, () =>
    expect(page.getByRole('link', { name: 'Ver los puntos de la jornada' })).toBeVisible({
      timeout: 2_000,
    }),
  );
  await page.getByRole('link', { name: 'Ver los puntos de la jornada' }).click();
  await reloadUntil(page, () =>
    expect(page.getByRole('combobox', { name: 'Jornada' })).toBeVisible({ timeout: 2_000 }),
  );
  // Both lineups share Lucía and Rubén: Rubén's goal (4 with the edited table), his yellow card and
  // Lucía's assist, plus a clean sheet each. The ratings of the manager were cancelled, so no vote counts.
  const standing = (name: string) => page.getByRole('listitem').filter({ hasText: name }).first();
  await reloadUntil(page, () =>
    expect(standing('Uxía').getByText('+4', { exact: true })).toBeVisible({ timeout: 2_000 }),
  );
  await expect(standing(manager.displayName).getByText('+4', { exact: true })).toBeVisible();
  await accessible(page);
  await page.getByRole('link', { name: 'Clasificación', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Clasificación' })).toBeVisible();
  await accessible(page);
});
