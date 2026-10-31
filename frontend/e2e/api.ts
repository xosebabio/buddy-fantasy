import { expect, type Page } from '@playwright/test';

/**
 * What a test prepares through the API instead of the interface, as the admin or as a manager:
 * the league, its squad and its matches, which other specs and plans already cover as screens.
 * Every call goes with the session of the page's browser and, like Angular, its CSRF header.
 */
export interface Api {
  get<T>(path: string): Promise<T>;
  post<T = undefined>(path: string, data?: unknown): Promise<T>;
  put<T = undefined>(path: string, data?: unknown): Promise<T>;
}

/** The body is what the test asks for: it prepares data, it does not check the contract (the unit tests do). */
// eslint-disable-next-line @typescript-eslint/no-unnecessary-type-parameters
function parse<T>(text: string): T {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-return
  return JSON.parse(text === '' ? 'null' : text);
}

export function apiOf(page: Page): Api {
  async function send<T>(method: 'POST' | 'PUT', path: string, data: unknown): Promise<T> {
    const cookies = await page.context().cookies();
    const xsrf = cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
    const response = await page.request.fetch(path, {
      method,
      data,
      headers: { 'X-XSRF-TOKEN': xsrf },
    });
    expect(response.ok(), `${method} ${path}: ${await response.text()}`).toBe(true);
    return parse<T>(await response.text());
  }
  return {
    async get<T>(path: string): Promise<T> {
      const response = await page.request.get(path);
      expect(response.ok(), `GET ${path}: ${await response.text()}`).toBe(true);
      return parse<T>(await response.text());
    },
    post: (path, data) => send('POST', path, data),
    put: (path, data) => send('PUT', path, data),
  };
}

export type Position = 'GOALKEEPER' | 'DEFENDER' | 'MIDFIELDER' | 'FORWARD';

export interface Player {
  readonly id: string;
  readonly name: string;
}

/** The server's clock of the `e2e` profile, which only moves forward. */
export class ServerClock {
  constructor(private readonly api: Api) {}

  async now(): Promise<Date> {
    return new Date((await this.api.get<{ now: string }>('/api/e2e/clock')).now);
  }

  /** Moves it forward by an ISO-8601 duration and returns the new time. */
  async advance(duration: string): Promise<Date> {
    const { now } = await this.api.post<{ now: string }>('/api/e2e/clock', { advance: duration });
    return new Date(now);
  }
}

/** Gives the browser the server's time, so that its countdowns agree with it. */
export async function syncBrowserClock(page: Page, clock: ServerClock): Promise<void> {
  await page.clock.setSystemTime(await clock.now());
}
