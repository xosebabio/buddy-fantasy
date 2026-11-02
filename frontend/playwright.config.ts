import { defineConfig, devices } from '@playwright/test';

/**
 * E2E (FE-34): on mobile, in Chromium and WebKit, in the light and the dark theme, against the
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
const setups = browsers.flatMap(({ name, device }) =>
  themes.map((colorScheme) => ({ name, device, colorScheme })),
);
const projectName = ({ name, colorScheme }: (typeof setups)[number]) => `${name}-${colorScheme}`;
/** The specs that advance the server's clock (`POST /api/e2e/clock`). */
const CLOCK_SPECS = [
  { name: 'matchday', match: /(?<!admin-)matchday\.spec\.ts/ },
  { name: 'admin-matchday', match: /admin-matchday\.spec\.ts/ },
] as const;
const CLOCK_SPEC_FILES = CLOCK_SPECS.map(({ match }) => match);
// Every project that moves the clock, in the order they run: each waits for the previous one.
const clockProjects = CLOCK_SPECS.flatMap((spec) =>
  setups.map((setup) => ({ spec, setup, name: `${projectName(setup)}-${spec.name}` })),
);

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  // Four browsers at once and lazy routes make 5 s too tight on a slow machine.
  expect: { timeout: 10_000 },
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
  projects: [
    ...setups.map((setup) => ({
      name: projectName(setup),
      testIgnore: CLOCK_SPEC_FILES,
      use: { ...setup.device, colorScheme: setup.colorScheme },
    })),
    // The server's clock is the whole backend's and only moves forward, so the specs that move it
    // never run at once: each of these projects waits for the previous one.
    ...clockProjects.map(({ spec, setup, name }, index) => ({
      name,
      testMatch: spec.match,
      use: { ...setup.device, colorScheme: setup.colorScheme },
      dependencies: index > 0 ? [clockProjects[index - 1]?.name ?? ''] : [],
    })),
  ],
});
