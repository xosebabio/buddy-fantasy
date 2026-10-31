import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
  withRouterConfig,
} from '@angular/router';

import { routes } from './app.routes';
import { SessionStore } from './core/auth/session-store';
import { provideApiHttp } from './core/http/provide-api-http';
import { PageTitle } from './core/layout/page-title';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      // The route's parameters and data as inputs; `:leagueId` reaches every page of the league.
      withComponentInputBinding(),
      withRouterConfig({ paramsInheritanceStrategy: 'always' }),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
    ),
    { provide: TitleStrategy, useClass: PageTitle },
    provideApiHttp(),
    // Asks for the session as soon as the app starts (FE-10) without waiting for it: the guards
    // do, and meanwhile the root component is on screen to offer a retry if the backend fails.
    provideAppInitializer(() => {
      void inject(SessionStore).load();
    }),
  ],
};
