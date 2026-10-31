import { expect, expectAccessible, logIn, newUser, test, unique } from './fixtures';
import { createLeague, toast } from './pages';

test('a user without a session opens an invitation and is back on it after the login', async ({
  openBrowser,
}) => {
  const leagueName = unique('Peña del domingo');
  const admin = await openBrowser();
  await logIn(admin, newUser('Uxía'));
  const link = await createLeague(admin, leagueName);

  const guest = await openBrowser();
  await guest.goto(link);
  await expect(guest).toHaveURL('/login');
  await expect(guest.getByRole('button', { name: 'Entrar con Google' })).toBeVisible();
  await expectAccessible(guest);

  // Google's login always comes back to /, which takes the user to the route they asked for.
  await logIn(guest, newUser('Brais'));
  await guest.goto('/');
  await expect(guest).toHaveURL(link);
  await expect(guest.getByText(leagueName)).toBeVisible();
  await expectAccessible(guest);

  await guest.getByRole('button', { name: 'Unirme a la liga' }).click();
  await expect(guest.getByRole('button', { name: `${leagueName}, cambiar de liga` })).toBeVisible();
  await expect(toast(guest, `Te has unido a ${leagueName}`)).toBeVisible();
  await expectAccessible(guest);
});
