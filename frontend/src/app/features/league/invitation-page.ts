import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { map, switchMap } from 'rxjs';

import { LeagueApi } from '../../core/api/league/league-api';
import { LEAGUES_ROUTE } from '../../core/auth/home-redirect';
import { SessionStore } from '../../core/auth/session-store';
import { handling } from '../../core/http/error-interceptor';
import { Online } from '../../core/http/online';
import { Button } from '../../ui/button/button';
import { Card } from '../../ui/card/card';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Toasts } from '../../ui/toast/toasts';

/**
 * An invitation link (RF-06): the league it leads to and the button to join it. A link that is not
 * valid (revoked, replaced or mistyped) is a `404` that the page explains itself. The backend does
 * not duplicate a membership, so a member goes to the league joining again.
 */
@Component({
  selector: 'bf-invitation-page',
  imports: [Button, Card, EmptyState, LoadError, RouterLink, Skeleton],
  templateUrl: './invitation-page.html',
  styleUrl: './invitation-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationPage {
  readonly token = input<string>();

  private readonly leagueApi = inject(LeagueApi);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly leaguesRoute = LEAGUES_ROUTE;
  protected readonly preview = this.leagueApi.invitation(this.token, handling(404));
  protected readonly joining = signal(false);
  /** Whether joining found the link no longer valid. */
  private readonly revoked = signal(false);
  protected readonly invalid = computed(() => this.revoked() || isNotFound(this.preview.error()));

  protected join(): void {
    const token = this.token();
    const preview = this.preview.value();
    if (token === undefined || preview === undefined || this.joining()) {
      return;
    }
    this.joining.set(true);
    this.leagueApi
      .join(token, handling(404))
      .pipe(switchMap(({ leagueId }) => this.session.reloadLeagues().pipe(map(() => leagueId))))
      .subscribe({
        next: (leagueId) => {
          if (!preview.alreadyMember) {
            this.toasts.show({
              message: $localize`:@@league.invitation.joined:Te has unido a ${preview.leagueName}:league:`,
            });
          }
          void this.router.navigate([LEAGUES_ROUTE, leagueId]);
        },
        error: (error: unknown) => {
          this.joining.set(false);
          this.revoked.set(isNotFound(error));
        },
      });
  }
}

function isNotFound(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 404;
}
