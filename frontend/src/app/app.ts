import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { showFailedRequests } from './core/http/failed-requests-notice';
import { OfflineNotice } from './core/layout/offline-notice';
import { Toaster } from './ui/toast/toaster';

/** What every page has: the offline notice, the page and the toasts. */
@Component({
  selector: 'bf-root',
  imports: [OfflineNotice, RouterOutlet, Toaster],
  template: `
    <bf-offline-notice />
    <router-outlet />
    <bf-toaster />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  constructor() {
    showFailedRequests();
  }
}
