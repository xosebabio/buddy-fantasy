import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { LucideCloudAlert } from '@lucide/angular';

import { Button } from '../button/button';

/**
 * What a screen shows when what it reads could not be loaded (FE-14), with the button to try again.
 * It is an alert, so screen readers announce it when it appears.
 */
@Component({
  selector: 'bf-load-error',
  imports: [Button, LucideCloudAlert],
  template: `
    <svg lucideCloudAlert class="icon" [size]="24"></svg>
    <p class="message" i18n="@@ui.loadError.message">
      No se ha podido cargar. Comprueba la conexión y vuelve a intentarlo.
    </p>
    <button bfButton type="button" (click)="retry.emit()" i18n="@@ui.loadError.retry">
      Reintentar
    </button>
  `,
  styleUrl: './load-error.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'alert' },
})
export class LoadError {
  readonly retry = output();
}
