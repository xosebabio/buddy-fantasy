import { inject } from '@angular/core';
import { type RedirectFunction, Router } from '@angular/router';

import { LOGIN_ROUTE } from '../http/error-interceptor';
import { LastLeague } from './last-league';
import { ReturnUrl } from './return-url';
import { SessionStore } from './session-store';

export const LEAGUES_ROUTE = '/leagues';

/**
 * Where `/` leads (FE-03, FE-10): the login without a session; with one, the route saved before
 * the login (the backend always comes back to `/`), or else the last league used if the user is
 * still in it, or else their leagues.
 */
export const homeRedirect: RedirectFunction = async () => {
  const session = inject(SessionStore);
  const router = inject(Router);
  const returnUrl = inject(ReturnUrl);
  const lastLeague = inject(LastLeague).get();
  await session.load();
  if (session.user() === null) {
    return router.parseUrl(LOGIN_ROUTE);
  }
  const saved = returnUrl.consume();
  if (saved !== null) {
    return router.parseUrl(saved);
  }
  if (lastLeague !== null && session.league(lastLeague) !== undefined) {
    return router.createUrlTree([LEAGUES_ROUTE, lastLeague]);
  }
  return router.parseUrl(LEAGUES_ROUTE);
};
