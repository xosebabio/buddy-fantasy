import { DestroyRef, DOCUMENT, inject } from '@angular/core';

/**
 * Calls `callback` each time the user comes back to the tab (FE-09): the pages with deadlines use it
 * to reload what the server may have changed while they were away, instead of polling. Call it in
 * an injection context, such as the constructor of the page; it stops with it.
 */
export function onVisible(callback: () => void): void {
  const document = inject(DOCUMENT);
  const listener = (): void => {
    if (document.visibilityState === 'visible') {
      callback();
    }
  };
  document.addEventListener('visibilitychange', listener);
  inject(DestroyRef).onDestroy(() => {
    document.removeEventListener('visibilitychange', listener);
  });
}
