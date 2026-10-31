import { expect, type Locator, type Page } from '@playwright/test';

/** The steps that more than one spec takes, with the texts a person sees. */

/** Creates a league from «Tus ligas» and returns its invitation link, shown only then. */
export async function createLeague(page: Page, name: string): Promise<string> {
  await page.goto('/leagues/new');
  await page.getByLabel('Nombre de la liga').fill(name);
  await page.getByRole('button', { name: 'Crear liga' }).click();
  await expect(page.getByRole('heading', { name: 'Invita a tu equipo' })).toBeVisible();
  return invitationLink(page);
}

/** The invitation link of the page, in its read-only field. */
export function invitationLink(page: Page): Promise<string> {
  return page.getByRole('textbox', { name: 'Enlace de invitación' }).inputValue();
}

/** A toast with the text, in the list of notices (the live announcer of the CDK repeats it). */
export function toast(page: Page, text: string): Locator {
  return page.getByRole('list', { name: 'Avisos' }).getByText(text);
}

/** Answers the confirmation sheet with its button. */
export async function confirm(page: Page, button: string): Promise<void> {
  const dialog = page.getByRole('alertdialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: button, exact: true }).click();
  await expect(dialog).toBeHidden();
}

/**
 * Waits for something that a job of the backend does (locking the lineups, scoring a matchday),
 * which runs every 2 s with the `e2e` profile: checks the page as it is and, while it fails,
 * reloads it. It does not reload before the first check, so as not to cut a navigation short.
 */
export async function reloadUntil(page: Page, check: () => Promise<void>): Promise<void> {
  let attempt = 0;
  await expect(async () => {
    if (attempt++ > 0) {
      await page.reload();
    }
    await check();
  }).toPass({ timeout: 30_000 });
}
