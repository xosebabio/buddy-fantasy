import { DestroyRef, DOCUMENT, inject, type Signal } from '@angular/core';
import type { CanDeactivateFn } from '@angular/router';
import { type Observable, of } from 'rxjs';

import { Confirm } from '../../ui/confirm/confirm';

/** A page with something the user edits and has not saved: the lineup and the ratings (FE-30). */
export interface HasUnsavedChanges {
  /** Whether the draft differs from what is saved; false right after saving. */
  readonly unsaved: Signal<boolean>;
}

/**
 * Asks whether to leave a page with unsaved changes, in the confirmation sheet and with «Cancelar»
 * as the safe answer. Call it in an injection context.
 */
export function askToLeave(): Observable<boolean> {
  return inject(Confirm).ask({
    title: $localize`:@@core.unsavedChanges.title:¿Salir sin guardar?`,
    message: $localize`:@@core.unsavedChanges.message:Tienes cambios sin guardar. Si sales, los perderás.`,
    confirm: $localize`:@@core.unsavedChanges.confirm:Salir sin guardar`,
    tone: 'danger',
  });
}

/** Asks before leaving a page with unsaved changes (`canDeactivate`); leaves if there is nothing to lose. */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (page) =>
  page.unsaved() ? askToLeave() : of(true);

/**
 * Makes the browser ask before closing or reloading the tab while `unsaved()` is true. Call it in an
 * injection context, such as the constructor of the page; it stops with it.
 */
export function warnBeforeUnload(unsaved: () => boolean): void {
  const window = inject(DOCUMENT).defaultView;
  if (window === null) {
    return;
  }
  const listener = (event: BeforeUnloadEvent): void => {
    if (unsaved()) {
      event.preventDefault();
    }
  };
  window.addEventListener('beforeunload', listener);
  inject(DestroyRef).onDestroy(() => {
    window.removeEventListener('beforeunload', listener);
  });
}
