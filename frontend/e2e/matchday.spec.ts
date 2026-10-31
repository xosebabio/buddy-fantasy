import { type Api, apiOf, type Player, type Position, ServerClock } from './api';
import { expect, expectAccessible, logIn, newUser, test, unique } from './fixtures';
import { reloadUntil, toast } from './pages';

/** The whole matchday of a manager (plan 12, task 13), with the admin's side prepared by API. */

const HOUR = 3_600_000;

interface Squad {
  readonly diego: Player;
  readonly marta: Player;
  readonly lucia: Player;
  readonly ruben: Player;
  readonly hugo: Player;
}

async function registerSquad(admin: Api, leagueId: string): Promise<Squad> {
  const register = (name: string, position: Position, shirtNumber: number, initialValue: number) =>
    admin.post<Player>(`/api/leagues/${leagueId}/players`, {
      name,
      position,
      shirtNumber,
      initialValue,
    });
  return {
    diego: await register('Diego', 'GOALKEEPER', 1, 8),
    marta: await register('Marta', 'DEFENDER', 4, 10),
    lucia: await register('Lucía', 'MIDFIELDER', 8, 12),
    ruben: await register('Rubén', 'FORWARD', 9, 15),
    hugo: await register('Hugo', 'FORWARD', 11, 9),
  };
}

test('a manager plays a whole matchday: lineup, lock, rating, close and standings', async ({
  openBrowser,
}) => {
  // A whole matchday in one walkthrough, with two jobs to wait for, does not fit in the default 30 s.
  test.setTimeout(120_000);
  const leagueName = unique('Peña del domingo');
  const manager = newUser('Brais');

  // The admin's side, by API: the league, its squad, two matches and the lineup of the admin.
  const adminPage = await openBrowser();
  await logIn(adminPage, newUser('Uxía'));
  const admin = apiOf(adminPage);
  const clock = new ServerClock(admin);
  const league = await admin.post<{ id: string; invitationToken: string }>('/api/leagues', {
    name: leagueName,
  });
  const base = `/api/leagues/${league.id}`;
  // A lineup of three keeps the walkthrough short: the rules are the league's, not the screen's.
  const settings = await admin.get<Record<string, unknown>>(`${base}/settings`);
  await admin.put(`${base}/settings`, { ...settings, lineupSize: 3 });
  const squad = await registerSquad(admin, league.id);

  const start = await clock.now();
  const at = (hours: number) => new Date(start.getTime() + hours * HOUR).toISOString();
  const first = await admin.post<{ id: string }>(`${base}/matches`, {
    matchday: 1,
    opponent: 'CD Arrabal',
    venue: 'HOME',
    kickoffAt: at(5),
  });
  await admin.post(`${base}/matches`, {
    matchday: 2,
    opponent: 'Atlético Lavadores',
    venue: 'AWAY',
    kickoffAt: at(72),
  });
  await admin.put(`${base}/lineup`, {
    playerIds: [squad.lucia.id, squad.hugo.id, squad.ruben.id],
  });

  // The manager joins with the invitation.
  const page = await openBrowser();
  await page.clock.install({ time: await clock.now() });
  await logIn(page, manager);
  await page.goto(`/invite/${league.invitationToken}`);
  await page.getByRole('button', { name: 'Unirme a la liga' }).click();
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await expect(page.getByText('Contra CD Arrabal')).toBeVisible();
  await expect(page.getByText('Aún no tienes alineación')).toBeVisible();
  await expectAccessible(page);

  // 1. The lineup, on the pitch, with the sheet of candidates.
  await page.getByRole('link', { name: 'Hacer alineación' }).click();
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await expectAccessible(page);
  const dialog = page.getByRole('dialog');
  for (const name of ['Diego', 'Marta', 'Rubén']) {
    await page
      .getByRole('button', { name: /^Plaza libre/ })
      .first()
      .click();
    await expect(dialog).toBeVisible();
    if (name === 'Diego') {
      await expectAccessible(page);
    }
    await dialog.getByRole('button', { name: new RegExp(name) }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByRole('button', { name: new RegExp(`^\\d+ ${name} `) })).toBeVisible();
  }
  await expectAccessible(page);
  await page.getByRole('button', { name: 'Guardar alineación' }).click();
  await expect(toast(page, 'Alineación guardada')).toBeVisible();

  await page.getByRole('link', { name: 'Jornada', exact: true }).click();
  await expect(page.getByRole('link', { name: /^Tu alineación/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Editar alineación' })).toBeVisible();

  // 2. The server's clock goes past the lock: the lineups of everyone are public.
  await clock.advance('PT3H5M');
  await page.clock.setSystemTime(await clock.now());
  await expect(async () => {
    await page.reload();
    await expect(page.getByRole('link', { name: /^Alineaciones de la jornada 1\s*$/ })).toBeVisible(
      { timeout: 2_000 },
    );
  }).toPass({ timeout: 30_000 });
  await expect(page.getByRole('heading', { name: 'Jornada 2' })).toBeVisible();
  await expectAccessible(page);
  await page.getByRole('link', { name: /^Alineaciones de la jornada 1/ }).click();
  await reloadUntil(page, () =>
    expect(page.getByRole('heading', { name: 'Alineaciones de la jornada 1' })).toBeVisible({
      timeout: 2_000,
    }),
  );
  await page.getByRole('button', { name: new RegExp(manager.displayName) }).click();
  await expect(page.getByText('Marta').first()).toBeVisible();
  await expectAccessible(page);

  // 3. The admin records the sheet and closes the match, which opens the rating window.
  await admin.put(`${base}/matches/${first.id}/sheet`, {
    result: { goalsFor: 1, goalsAgainst: 0 },
    performances: [
      { playerId: squad.diego.id },
      { playerId: squad.marta.id, assists: 1 },
      { playerId: squad.ruben.id, goals: 1 },
      { playerId: squad.lucia.id },
    ],
  });
  await clock.advance('PT2H');
  await admin.post(`${base}/matches/${first.id}/close`);
  await page.clock.setSystemTime(await clock.now());

  // 4. The manager rates with spades, changes a vote and saves.
  await page.goto(`/leagues/${league.id}`);
  await page.getByRole('link', { name: 'Valorar la jornada 1' }).last().click();
  await expect(page.getByRole('heading', { name: 'Jornada 1' })).toBeVisible();
  await expect(page.getByText('De 0 a 4 picas')).toBeVisible();
  await expectAccessible(page);
  const spades = page.getByRole('radiogroup', { name: 'Picas para Rubén' });
  await spades.getByText('3', { exact: true }).click();
  await spades.getByText('4', { exact: true }).click();
  await page.getByRole('radiogroup', { name: 'Picas para Marta' }).getByText('0').click();
  await page.getByRole('button', { name: 'Quitar voto a Marta' }).click();
  await page.getByRole('button', { name: 'Guardar valoraciones' }).click();
  await expect(toast(page, 'Valoraciones guardadas')).toBeVisible();
  await expect(page.getByText('Has valorado a 1 de 4')).toBeVisible();

  // 5. The server's clock goes past the close of the window, and the job scores the matchday.
  await clock.advance('PT25H');
  await page.clock.setSystemTime(await clock.now());

  // 6. The average, the points, the standings, the history and the sheet of a player.
  await expect(async () => {
    await page.goto(`/leagues/${league.id}/ratings/${first.id}`);
    await expect(page.getByText('Media 4,00 con 1 votos')).toBeVisible({ timeout: 2_000 });
  }).toPass({ timeout: 30_000 });
  await expect(page.getByText('Tu voto: 4')).toBeVisible();
  await expectAccessible(page);

  await page.getByRole('link', { name: 'Ver los puntos de la jornada' }).click();
  // The manager's lineup scored 18 (Rubén's goal and rating, and Marta's assist), the admin's 17.
  await reloadUntil(page, () =>
    expect(page.getByRole('combobox', { name: 'Jornada' })).toBeVisible({ timeout: 2_000 }),
  );
  const row = (name: string) => page.getByRole('listitem').filter({ hasText: name });
  await expect(
    row(manager.displayName).first().getByText('+18', { exact: true }).first(),
  ).toBeVisible();
  await expect(row('Uxía').first().getByText('+17', { exact: true }).first()).toBeVisible();
  await expectAccessible(page);

  await page.getByRole('link', { name: 'Clasificación', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Clasificación' })).toBeVisible();
  const mine = page.getByRole('link', { name: new RegExp(manager.displayName) });
  await expect(mine).toBeVisible();
  await expectAccessible(page);

  await mine.click();
  await expect(page.getByRole('heading', { name: manager.displayName })).toBeVisible();
  await page.getByRole('button', { name: /^Jornada 1/ }).click();
  await expectAccessible(page);

  await page.goto(`/leagues/${league.id}/squad/${squad.ruben.id}`);
  await expect(page.getByRole('heading', { name: 'Rubén' })).toBeVisible();
  await page.getByRole('button', { name: /^Jornada 1/ }).click();
  await expect(page.getByText('Valoración')).toBeVisible();
  await expectAccessible(page);
});
