import { EnvironmentInjector, inject, runInInjectionContext } from '@angular/core';
import type { CanDeactivateFn } from '@angular/router';
import { from, of, switchMap } from 'rxjs';

import type { HasUnsavedChanges } from './unsaved-changes';

/**
 * `unsavedChangesGuard` for the routes (`canDeactivate`), which loads the confirmation sheet only
 * when there is something to lose, so that the routes of the features do not add it to the initial
 * bundle (RNF-13).
 */
export const lazyUnsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (page) => {
  if (!page.unsaved()) {
    return of(true);
  }
  const injector = inject(EnvironmentInjector);
  return from(import('./unsaved-changes')).pipe(
    switchMap((m) => runInInjectionContext(injector, () => m.askToLeave())),
  );
};
