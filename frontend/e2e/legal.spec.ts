import { expect, expectAccessible, test } from './fixtures';

/** The legal pages, each one reached from the login by its link (RNF-10). */
const PAGES = [
  { link: 'política de privacidad', path: '/privacy', title: 'Política de privacidad' },
  { link: 'condiciones de servicio', path: '/terms', title: 'Condiciones de servicio' },
] as const;

for (const page of PAGES) {
  test(`a visitor without a session reads the ${page.title} from the login`, async ({
    openBrowser,
  }) => {
    const visitor = await openBrowser();
    await visitor.goto('/login');
    await visitor.getByRole('link', { name: page.link }).click();

    await expect(visitor).toHaveURL(page.path);
    await expect(visitor).toHaveTitle(`${page.title} – Buddy Fantasy`);
    await expect(visitor.getByRole('heading', { level: 1, name: page.title })).toBeVisible();
    await expectAccessible(visitor);
  });
}

test('the two legal pages link to each other from their foot', async ({ openBrowser }) => {
  const visitor = await openBrowser();
  await visitor.goto('/privacy');

  await visitor.locator('footer').getByRole('link', { name: 'Condiciones de servicio' }).click();
  await expect(visitor).toHaveURL('/terms');
  await expect(
    visitor.getByRole('heading', { level: 1, name: 'Condiciones de servicio' }),
  ).toBeVisible();

  await visitor.locator('footer').getByRole('link', { name: 'Política de privacidad' }).click();
  await expect(visitor).toHaveURL('/privacy');
  await expect(
    visitor.getByRole('heading', { level: 1, name: 'Política de privacidad' }),
  ).toBeVisible();
});
