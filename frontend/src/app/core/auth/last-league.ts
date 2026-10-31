import { Injectable } from '@angular/core';

const KEY = 'bf.lastLeague';

/** The league the user was in last, where `/` takes them back (FE-03). */
@Injectable({ providedIn: 'root' })
export class LastLeague {
  save(leagueId: string): void {
    localStorage.setItem(KEY, leagueId);
  }

  get(): string | null {
    return localStorage.getItem(KEY);
  }
}
