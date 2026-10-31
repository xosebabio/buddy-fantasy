import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { SessionStore } from '../../core/auth/session-store';
import { Online } from '../../core/http/online';
import { Bib, type Position } from '../../shared/bib/bib';
import { Button } from '../../ui/button/button';
import { Wordmark } from '../../ui/top-bar/wordmark';

/** The classic shirt number of each position, the four bibs that drop on the login. */
const LINEUP: readonly { position: Position; number: number }[] = [
  { position: 'GOALKEEPER', number: 1 },
  { position: 'DEFENDER', number: 4 },
  { position: 'MIDFIELDER', number: 8 },
  { position: 'FORWARD', number: 9 },
];

/**
 * The login (RF-01, FE-10): only with Google, a full navigation out of the app. The backend comes
 * back with `?error` when it fails. The route to go back to, if any, was saved by whoever sent here.
 */
@Component({
  selector: 'bf-login-page',
  imports: [Bib, Button, Wordmark],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  /** `?error`, present (and empty) when the login with Google failed. */
  readonly error = input<string>();

  protected readonly failed = computed(() => this.error() !== undefined);
  protected readonly lineup = LINEUP;
  protected readonly session = inject(SessionStore);
  protected readonly online = inject(Online);
}
