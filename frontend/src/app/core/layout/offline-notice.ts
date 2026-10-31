import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LucideWifiOff } from '@lucide/angular';

import { Online } from '../http/online';

/**
 * The notice at the top of every page while the browser has no network (FE-31); the pages disable
 * what sends something. It is a status, so screen readers announce it when it appears.
 */
@Component({
  selector: 'bf-offline-notice',
  imports: [LucideWifiOff],
  template: `
    <div role="status">
      @if (!online.online()) {
        <p class="notice">
          <svg lucideWifiOff class="icon" [size]="20"></svg>
          <span i18n="@@core.offline.notice">
            Sin conexión. Podrás enviar cambios cuando vuelva la red.
          </span>
        </p>
      }
    </div>
  `,
  styleUrl: './offline-notice.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfflineNotice {
  protected readonly online = inject(Online);
}
