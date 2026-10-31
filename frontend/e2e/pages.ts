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
