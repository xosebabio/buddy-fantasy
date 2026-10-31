import { HttpErrorResponse } from '@angular/common/http';
import { computed, DOCUMENT, inject, Injectable, InjectionToken, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, firstValueFrom, forkJoin, map, Observable, of, tap, throwError } from 'rxjs';

import { IdentityApi, type UserResponse } from '../api/identity/identity-api';
import { LeagueApi, type MyLeagueResponse } from '../api/league/league-api';
import { handling, LOGIN_ROUTE } from '../http/error-interceptor';
import { ReturnUrl } from './return-url';

/** Where the browser goes to log in with Google: a full navigation, out of the app (FE-10). */
export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google';

/** The browser's location, which leaves the app to log in; replaced in the tests. */
export const BROWSER_LOCATION = new InjectionToken<Pick<Location, 'assign'>>('BROWSER_LOCATION', {
  providedIn: 'root',
  factory: () => inject(DOCUMENT).location,
});

interface Session {
  readonly user: UserResponse;
  readonly leagues: readonly MyLeagueResponse[];
}

/**
 * The user with the session and their leagues with their role (FE-13), the only global state of
 * the app. `load` asks for them once, when the app starts; the guards wait for it and only hide
 * what the backend would refuse anyway. The pages that create, join or leave a league call
 * `reloadLeagues`.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly identityApi = inject(IdentityApi);
  private readonly leagueApi = inject(LeagueApi);
  private readonly router = inject(Router);
  private readonly returnUrl = inject(ReturnUrl);
  private readonly location = inject(BROWSER_LOCATION);

  private readonly session = signal<Session | null>(null);
  private loaded?: Promise<void>;

  /** The user with the session, or `null` without one. */
  readonly user = computed(() => this.session()?.user ?? null);
  readonly leagues = computed(() => this.session()?.leagues ?? []);

  /**
   * Asks the backend for the session, only the first time: `GET /api/me` decides whether there is
   * one (FE-10). Without one (`401`), or if the backend cannot answer and the user gives up
   * retrying, the app goes on without a session and the guards send to the login.
   */
  load(): Promise<void> {
    this.loaded ??= firstValueFrom(
      forkJoin([this.identityApi.me(handling(401)), this.leagueApi.myLeagues(handling(401))]).pipe(
        map(([user, leagues]) => ({ user, leagues })),
        catchError((error: unknown) =>
          error instanceof HttpErrorResponse ? of(null) : throwError(() => error),
        ),
      ),
    ).then((session) => {
      this.session.set(session);
    });
    return this.loaded;
  }

  /** The user's league with this id and their role in it, if they are a member. */
  league(leagueId: string): MyLeagueResponse | undefined {
    return this.leagues().find((league) => league.leagueId === leagueId);
  }

  /** Reads the user's leagues again, after creating, joining or leaving one. */
  reloadLeagues(): Observable<readonly MyLeagueResponse[]> {
    return this.leagueApi.myLeagues().pipe(
      tap((leagues) => {
        this.session.update((session) => session && { ...session, leagues });
      }),
    );
  }

  /**
   * Leaves the app to log in with Google, which comes back to `/` (FE-10); from there the app goes
   * to `returnUrl`, if given, such as the invitation the user was looking at.
   */
  login(returnUrl?: string): void {
    if (returnUrl !== undefined) {
      this.returnUrl.save(returnUrl);
    }
    this.location.assign(GOOGLE_LOGIN_URL);
  }

  /** Ends the session and goes to the login. */
  logout(): Observable<null> {
    return this.identityApi.logout().pipe(
      tap(() => {
        this.session.set(null);
        void this.router.navigateByUrl(LOGIN_ROUTE);
      }),
    );
  }
}
