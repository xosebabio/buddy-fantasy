import { randomUUID } from 'node:crypto';

import AxeBuilder from '@axe-core/playwright';
import { type BrowserContext, expect, type Page, test as base } from '@playwright/test';

/** An account of the test login (`POST /api/e2e/login`), which only the `e2e` profile has. */
export interface E2eUser {
  readonly email: string;
  readonly displayName: string;
}

/** A new account for each test, so that they never share data even against the same database. */
export function newUser(displayName: string): E2eUser {
  return { email: `${randomUUID()}@e2e.buddyfantasy.test`, displayName };
}

/** A name that no other test or run has used. */
export function unique(name: string): string {
  return `${name} ${randomUUID().slice(0, 8)}`;
}

const CSP_VIOLATION = 'CSP violation:';

interface Fixtures {
  /**
   * Opens a browser of its own, as each person in a test has their own device. The test fails if
   * any of them breaks the CSP (that of the `<meta>` of autoCsp and that of Caddy's header) or
   * throws an uncaught error.
   */
  openBrowser: () => Promise<Page>;
}

export const test = base.extend<Fixtures>({
  openBrowser: async ({ browser }, use) => {
    const contexts: BrowserContext[] = [];
    const problems: string[] = [];
    await use(async () => {
      const context = await browser.newContext();
      contexts.push(context);
      // Chromium and WebKit word the violations differently in the console; the event is the same.
      await context.addInitScript((prefix) => {
        document.addEventListener('securitypolicyviolation', (event) => {
          console.error(`${prefix} ${event.effectiveDirective} ${event.blockedURI}`);
        });
      }, CSP_VIOLATION);
      const page = await context.newPage();
      page.on('console', (message) => {
        if (message.type() === 'error' && message.text().startsWith(CSP_VIOLATION)) {
          problems.push(message.text());
        }
      });
      page.on('pageerror', (error) => problems.push(error.message));
      return page;
    });
    await Promise.all(contexts.map((context) => context.close()));
    expect(problems, 'CSP violations and uncaught errors').toEqual([]);
  },
});

export { expect };

/**
 * Logs in without Google, with the session cookie in the page's browser. Like Angular, it sends
 * back the `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header; the backend gives it on any request,
 * even on the `401` of `GET /api/me` without a session, which goes through Caddy and `ng serve`.
 */
export async function logIn(page: Page, user: E2eUser): Promise<void> {
  await page.request.get('/api/me');
  const cookies = await page.context().cookies();
  const xsrf = cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
  const response = await page.request.post('/api/e2e/login', {
    data: user,
    headers: { 'X-XSRF-TOKEN': xsrf },
  });
  expect(response.status(), 'test login (is the backend running with the e2e profile?)').toBe(204);
}

/** The screen as it is passes axe without violations of WCAG 2.2 AA (RNF-12). */
export async function expectAccessible(page: Page): Promise<void> {
  const { violations } = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze();
  const found = violations.map(
    ({ id, help, nodes }) =>
      `${id}: ${help} (${nodes.map(({ target }) => target.join(' ')).join(', ')})`,
  );
  expect(found, `accessibility of ${page.url()}`).toEqual([]);
}
