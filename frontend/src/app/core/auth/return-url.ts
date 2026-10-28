import { Injectable } from '@angular/core';

const KEY = 'bf.returnUrl';

/**
 * The route to go back to after the login (FE-10). It lives in `sessionStorage` because the login
 * leaves the app, and the backend always comes back to `/`.
 */
@Injectable({ providedIn: 'root' })
export class ReturnUrl {
  save(url: string): void {
    if (isAppRoute(url)) {
      sessionStorage.setItem(KEY, url);
    }
  }

  /** The saved route, once: reading it forgets it. */
  consume(): string | null {
    const url = sessionStorage.getItem(KEY);
    sessionStorage.removeItem(KEY);
    return url !== null && isAppRoute(url) ? url : null;
  }
}

/** A route of this app, never another origin (`//host` or `/\host`) to send the user to. */
function isAppRoute(url: string): boolean {
  return url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/\\');
}
