import { defineConfig, devices } from '@playwright/test';

/**
 * Generates the screenshots of the root README (plan 15) with `npm run readme:screenshots`: one
 * mobile Chromium in the light theme against the real backend with the `e2e` profile, as the E2E
 * of `playwright.config.ts` does, but by hand and never in CI.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: /readme-screenshots\.ts/,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:8081',
    ...devices['Pixel 7'],
    colorScheme: 'light',
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    reducedMotion: 'reduce',
  },
});
