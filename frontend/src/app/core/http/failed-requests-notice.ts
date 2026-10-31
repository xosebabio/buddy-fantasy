import { effect, inject, untracked } from '@angular/core';

import { Toasts } from '../../ui/toast/toasts';
import { FailedRequests } from './failed-requests';

/**
 * Shows a toast with «Reintentar» while there are requests waiting in `FailedRequests` (FE-08):
 * retrying sends them again, and closing it gives them up. Called once, in the root component.
 */
export function showFailedRequests(): void {
  const failedRequests = inject(FailedRequests);
  const toasts = inject(Toasts);
  let shown = false;

  effect(() => {
    if (!failedRequests.pending() || shown) {
      return;
    }
    shown = true;
    untracked(() =>
      toasts.show({
        message: $localize`:@@core.failedRequests.message:No se pudo conectar con el servidor.`,
        tone: 'danger',
        action: {
          label: $localize`:@@core.failedRequests.retry:Reintentar`,
          run: () => {
            shown = false;
            failedRequests.retry();
          },
        },
        dismissed: () => {
          shown = false;
          failedRequests.dismiss();
        },
      }),
    );
  });
}
