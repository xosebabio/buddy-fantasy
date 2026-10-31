import { inject } from '@angular/core';
import {
  type ActivatedRouteSnapshot,
  type CanActivateFn,
  RedirectCommand,
  Router,
} from '@angular/router';

import { FORBIDDEN_ROUTE, LOGIN_ROUTE, NOT_FOUND_ROUTE } from '../http/error-interceptor';
import { ReturnUrl } from './return-url';
import { SessionStore } from './session-store';

/*
 * The guards only hide what the user cannot use: the backend authorizes every request (FE-10). They
 * wait for the session that the app asks for when it starts, so they inject everything before.
 */

/** Without a session, saves the route to come back to and goes to the login. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  const returnUrl = inject(ReturnUrl);
  await session.load();
  if (session.user() !== null) {
    return true;
  }
  returnUrl.save(state.url);
  return router.parseUrl(LOGIN_ROUTE);
};

/** The login is for those without a session: with one, `/` takes the user where they belong. */
export const guestGuard: CanActivateFn = async () => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.load();
  return session.user() === null || router.parseUrl('/');
};

/** A league that is not among the user's shows «No encontrado», keeping the URL. */
export const memberGuard: CanActivateFn = async (route, state) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.load();
  return leagueOf(route, session) !== undefined || showing(router, NOT_FOUND_ROUTE, state.url);
};

/** A page of the league's admin shows «Sin acceso» to the other members, keeping the URL. */
export const adminGuard: CanActivateFn = async (route, state) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.load();
  return leagueOf(route, session)?.role === 'ADMIN' || showing(router, FORBIDDEN_ROUTE, state.url);
};

/** The league of the route's `:leagueId`, which the router passes down to every child route. */
function leagueOf(route: ActivatedRouteSnapshot, session: SessionStore) {
  const leagueId = route.paramMap.get('leagueId');
  return leagueId === null ? undefined : session.league(leagueId);
}

/** Shows the page with the URL that was asked for in the address bar. */
function showing(router: Router, page: string, url: string): RedirectCommand {
  return new RedirectCommand(router.parseUrl(page), { browserUrl: url });
}
