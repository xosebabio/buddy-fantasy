import { DestroyRef, DOCUMENT, inject, Injectable, signal } from '@angular/core';

/**
 * Whether the browser has a network (FE-31). There is no offline mode: without one, the shell shows
 * a notice and the pages disable the buttons that send something, with
 * `[disabled]="!online.online()"`.
 */
@Injectable({ providedIn: 'root' })
export class Online {
  private readonly connected = signal(true);

  readonly online = this.connected.asReadonly();

  constructor() {
    const window = inject(DOCUMENT).defaultView;
    if (window === null) {
      return;
    }
    this.connected.set(window.navigator.onLine);
    const update = (): void => {
      this.connected.set(window.navigator.onLine);
    };
    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    inject(DestroyRef).onDestroy(() => {
      window.removeEventListener('online', update);
      window.removeEventListener('offline', update);
    });
  }
}
