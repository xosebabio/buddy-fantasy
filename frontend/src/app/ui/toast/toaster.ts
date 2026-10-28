import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LucideCircleAlert, LucideX } from '@lucide/angular';

import { Button } from '../button/button';
import { IconButton } from '../button/icon-button';
import { Toasts } from './toasts';

/**
 * Shows the toasts of `Toasts` at the bottom of the screen, above the safe area (FE-18). It goes
 * once in the shell; `Toasts` announces them, so this list is not a live region.
 */
@Component({
  selector: 'bf-toaster',
  imports: [Button, IconButton, LucideCircleAlert, LucideX],
  template: `
    @if (toasts.toasts().length > 0) {
      <ul class="toasts" aria-label="Avisos" i18n-aria-label="@@ui.toaster.label">
        @for (toast of toasts.toasts(); track toast.id) {
          <li class="toast" [class.danger]="toast.tone === 'danger'">
            @if (toast.tone === 'danger') {
              <svg lucideCircleAlert class="icon" [size]="20"></svg>
            }
            <p class="message">{{ toast.message }}</p>
            @if (toast.action; as action) {
              <button bfButton type="button" variant="quiet" (click)="toasts.act(toast.id)">
                {{ action.label }}
              </button>
            }
            <button
              bfIconButton
              type="button"
              label="Cerrar aviso"
              i18n-label="@@ui.toaster.close"
              (click)="toasts.dismiss(toast.id)"
            >
              <svg lucideX [size]="20"></svg>
            </button>
          </li>
        }
      </ul>
    }
  `,
  styleUrl: './toaster.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Toaster {
  protected readonly toasts = inject(Toasts);
}
