import { expect, expectAccessible, logIn, newUser, test, unique } from './fixtures';
import { confirm, invitationLink, toast } from './pages';

test('the admin creates a league, invites a member, sees them and removes them', async ({
  openBrowser,
}) => {
  const leagueName = unique('Peña del domingo');
  const member = newUser('Brais');

  const admin = await openBrowser();
  await logIn(admin, newUser('Uxía'));
  await admin.goto('/');
  await expect(admin).toHaveURL('/leagues');
  await expect(admin.getByText('Aún no juegas en ninguna liga')).toBeVisible();
  await expectAccessible(admin);

  await admin.getByRole('link', { name: 'Crear liga' }).click();
  await expect(admin.getByRole('heading', { name: 'Crear liga' })).toBeVisible();
  await expectAccessible(admin);

  await admin.getByLabel('Nombre de la liga').fill(leagueName);
  await admin.getByRole('button', { name: 'Crear liga' }).click();
  await expect(admin.getByRole('heading', { name: 'Invita a tu equipo' })).toBeVisible();
  await expectAccessible(admin);

  await admin.getByRole('link', { name: 'Ir a la liga' }).click();
  await expect(admin.getByRole('heading', { name: leagueName })).toBeVisible();
  const leagueUrl = admin.url();
  await expectAccessible(admin);

  // The link of the creation is not shown again: the admin generates a new one to share it.
  await admin.getByRole('link', { name: 'Invitar a tu equipo' }).click();
  await expect(admin.getByRole('heading', { name: 'Miembros e invitación' })).toBeVisible();
  await admin.getByRole('button', { name: 'Generar enlace nuevo' }).click();
  await expect(admin.getByRole('alertdialog')).toBeVisible();
  await expectAccessible(admin);
  await confirm(admin, 'Generar enlace');
  const link = await invitationLink(admin);

  const brais = await openBrowser();
  await logIn(brais, member);
  await brais.goto(link);
  await brais.getByRole('button', { name: 'Unirme a la liga' }).click();
  await expect(brais.getByRole('heading', { name: leagueName })).toBeVisible();
  await brais.getByRole('link', { name: 'Liga' }).click();
  await expect(brais.getByRole('heading', { name: 'Miembros' })).toBeVisible();
  await expect(brais.getByText(`${member.displayName} (tú)`)).toBeVisible();
  await expectAccessible(brais);

  const removeMember = admin.getByRole('button', { name: `Expulsar a ${member.displayName}` });
  await admin.reload();
  await expect(removeMember).toBeVisible();
  await expectAccessible(admin);
  await removeMember.click();
  await confirm(admin, 'Expulsar');
  await expect(toast(admin, `${member.displayName} ya no está en la liga`)).toBeVisible();
  await expect(removeMember).toHaveCount(0);

  await brais.goto('/leagues');
  await expect(brais.getByText('Aún no juegas en ninguna liga')).toBeVisible();
  await brais.goto(leagueUrl);
  await expect(brais.getByRole('heading', { name: 'No encontrado' })).toBeVisible();
  await expectAccessible(brais);

  await admin.getByRole('link', { name: 'Tu perfil' }).click();
  await expect(admin.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
  await expectAccessible(admin);
  await admin.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(admin).toHaveURL('/login');
});
