import { defineConfig, devices } from '@playwright/test';

/**
 * Smoke E2E (FE-34): on mobile, in Chromium and WebKit, in the light and the dark theme, against the
 * real backend with the `e2e` profile. By default against the production build served by Caddy
 * (`--profile caddy` of deploy/docker-compose.dev.yml), as in production; `E2E_BASE_URL` points it
 * elsewhere, such as `ng serve` (http://localhost:4200).
 */
const baseURL = process.env['E2E_BASE_URL'] ?? 'http://localhost:8081';
const ci = process.env['CI'] !== undefined;

const browsers = [
  { name: 'chromium', device: devices['Pixel 7'] },
  { name: 'webkit', device: devices['iPhone 14'] },
] as const;
const themes = ['light', 'dark'] as const;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  reporter: ci ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    // Without displacements (RNF-12), so that axe never measures the contrast of a moving element.
    reducedMotion: 'reduce',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: browsers.flatMap(({ name, device }) =>
    themes.map((colorScheme) => ({
      name: `${name}-${colorScheme}`,
      use: { ...device, colorScheme },
    })),
  ),
});
