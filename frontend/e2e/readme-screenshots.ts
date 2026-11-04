import { type Page } from '@playwright/test';

import { type Api, apiOf, type Player, type Position, ServerClock } from './api';
import { expect, logIn, newUser, test } from './fixtures';
import { reloadUntil, toast } from './pages';

/**
 * The screenshots of the root README (plan 15, task 4), in `docs/readme/`: the matchday, the lineup
 * on the pitch, the rating and the standings of a league made up for the occasion. Only the data
 * below, never that of a real league (RNF-10). It is not a test of the app and CI does not run it.
 */

const HOUR = 3_600_000;
const OUTPUT = '../docs/readme';

async function registerSquad(admin: Api, leagueId: string): Promise<Map<string, Player>> {
  const squad = new Map<string, Player>();
  const register = async (
    name: string,
    position: Position,
    shirtNumber: number,
    initialValue: number,
  ) => {
    squad.set(
      name,
      await admin.post<Player>(`/api/leagues/${leagueId}/players`, {
        name,
        position,
        shirtNumber,
        initialValue,
      }),
    );
  };
  await register('Diego', 'GOALKEEPER', 1, 8);
  await register('Marta', 'DEFENDER', 4, 10);
  await register('Iria', 'DEFENDER', 5, 9);
  await register('Lucía', 'MIDFIELDER', 8, 12);
  await register('Nuria', 'MIDFIELDER', 6, 10);
  await register('Rubén', 'FORWARD', 9, 15);
  await register('Hugo', 'FORWARD', 11, 9);
  return squad;
}

async function shoot(page: Page, name: string): Promise<void> {
  // Whichever element scrolls (the page or the shell's main area), from its top.
  await page.evaluate(() => {
    window.scrollTo(0, 0);
    document.querySelectorAll('*').forEach((element) => {
      element.scrollTop = 0;
    });
  });
  await page.screenshot({ path: `${OUTPUT}/${name}.png` });
}

test('README screenshots', async ({ openBrowser }) => {
  test.setTimeout(180_000);
  const manager = newUser('Brais');

  const adminPage = await openBrowser();
  await logIn(adminPage, newUser('Uxía'));
  const admin = apiOf(adminPage);
  const clock = new ServerClock(admin);
  const league = await admin.post<{ id: string; invitationToken: string }>('/api/leagues', {
    name: 'Peña del domingo',
  });
  const base = `/api/leagues/${league.id}`;
  const settings = await admin.get<Record<string, unknown>>(`${base}/settings`);
  await admin.put(`${base}/settings`, { ...settings, lineupSize: 5 });
  const squad = await registerSquad(admin, league.id);
  const player = (name: string): Player => {
    const found = squad.get(name);
    if (found === undefined) {
      throw new Error(`No player ${name} in the squad`);
    }
    return found;
  };

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
    playerIds: ['Diego', 'Iria', 'Nuria', 'Rubén', 'Hugo'].map((name) => player(name).id),
  });

  const page = await openBrowser();
  await page.clock.install({ time: await clock.now() });
  await logIn(page, manager);
  await page.goto(`/invite/${league.invitationToken}`);
  await page.getByRole('button', { name: 'Unirme a la liga' }).click();
  await expect(page.getByText('Contra CD Arrabal')).toBeVisible();
  // The fake clock of the page freezes the timers, so the toast would never go away by itself.
  await page.getByRole('list', { name: 'Avisos' }).getByRole('button').click();
  await expect(toast(page, 'Te has unido')).toBeHidden();
  await shoot(page, '01-matchday');

  await page.getByRole('link', { name: 'Hacer alineación' }).click();
  const dialog = page.getByRole('dialog');
  for (const name of ['Diego', 'Marta', 'Lucía', 'Rubén', 'Hugo']) {
    await page
      .getByRole('button', { name: /^Plaza libre/ })
      .first()
      .click();
    await dialog.getByRole('button', { name: new RegExp(name) }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByRole('button', { name: new RegExp(`^\\d+ ${name} `) })).toBeVisible();
  }
  await shoot(page, '02-lineup');
  await page.getByRole('button', { name: 'Guardar alineación' }).click();
  await expect(toast(page, 'Alineación guardada')).toBeVisible();

  // Past the lock, the admin records the sheet and closes the match, which opens the rating window.
  await clock.advance('PT3H5M');
  await admin.put(`${base}/matches/${first.id}/sheet`, {
    result: { goalsFor: 2, goalsAgainst: 1 },
    performances: [
      { playerId: player('Diego').id },
      { playerId: player('Marta').id, assists: 1 },
      { playerId: player('Lucía').id },
      { playerId: player('Rubén').id, goals: 2 },
      { playerId: player('Hugo').id },
    ],
  });
  await clock.advance('PT2H');
  await admin.post(`${base}/matches/${first.id}/close`);
  await page.clock.setSystemTime(await clock.now());

  await page.goto(`/leagues/${league.id}`);
  await page.getByRole('link', { name: 'Valorar la jornada 1' }).last().click();
  await expect(page.getByText('De 0 a 4 picas')).toBeVisible();
  for (const [name, spades] of [
    ['Rubén', '4'],
    ['Lucía', '3'],
    ['Marta', '3'],
    ['Diego', '2'],
  ] as const) {
    await page
      .getByRole('radiogroup', { name: `Picas para ${name}` })
      .getByText(spades, { exact: true })
      .click();
  }
  await shoot(page, '03-rating');
  await page.getByRole('button', { name: 'Guardar valoraciones' }).click();
  await expect(toast(page, 'Valoraciones guardadas')).toBeVisible();

  // Past the close of the window, the job scores the matchday.
  await clock.advance('PT25H');
  await page.clock.setSystemTime(await clock.now());
  await page.goto(`/leagues/${league.id}`);
  await page.getByRole('link', { name: 'Clasificación', exact: true }).click();
  await reloadUntil(page, () =>
    expect(page.getByRole('link', { name: new RegExp(manager.displayName) })).toBeVisible({
      timeout: 2_000,
    }),
  );
  await shoot(page, '04-standings');
});
