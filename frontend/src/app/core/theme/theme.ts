import { DOCUMENT, inject, Injectable, signal } from '@angular/core';

/** `system` follows `prefers-color-scheme`; the others fix the theme (FE-26). */
export type ThemePreference = 'system' | 'light' | 'dark';

/** Also read by the script of `index.html`, which applies the choice before the first paint. */
const KEY = 'bf.theme';

/**
 * The theme the user chose, kept in `localStorage` and applied as `data-theme` on `<html>`, where
 * ui/styles/_themes.scss switches the tokens.
 */
@Injectable({ providedIn: 'root' })
export class Theme {
  private readonly root = inject(DOCUMENT).documentElement;
  private readonly chosen = signal(stored());

  readonly preference = this.chosen.asReadonly();

  set(preference: ThemePreference): void {
    this.chosen.set(preference);
    if (preference === 'system') {
      localStorage.removeItem(KEY);
      this.root.removeAttribute('data-theme');
    } else {
      localStorage.setItem(KEY, preference);
      this.root.setAttribute('data-theme', preference);
    }
  }
}

function stored(): ThemePreference {
  const value = localStorage.getItem(KEY);
  return value === 'light' || value === 'dark' ? value : 'system';
}
