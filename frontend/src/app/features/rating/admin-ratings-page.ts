import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

import { LeagueApi, type MemberResponse } from '../../core/api/league/league-api';
import { RatingApi } from '../../core/api/rating/rating-api';
import { SessionStore } from '../../core/auth/session-store';
import { handling } from '../../core/http/error-interceptor';
import { ERROR_TEXTS } from '../../core/http/error-texts';
import { Online } from '../../core/http/online';
import { toProblem } from '../../core/http/problem';
import { onVisible } from '../../core/http/visible';
import { displayName } from '../../shared/roster/roster';
import { Avatar } from '../../ui/avatar/avatar';
import { Badge } from '../../ui/badge/badge';
import { Button } from '../../ui/button/button';
import { Confirm } from '../../ui/confirm/confirm';
import { Countdown } from '../../ui/countdown/countdown';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Toasts } from '../../ui/toast/toasts';

/**
 * Cancels the ratings of a manager in a match (`admin/ratings/:matchId`, RF-41). It lists the
 * members and marks those already cancelled; it does not say who has voted (RF-21, FE-11), because
 * the only thing it reads is `excludedUserIds`. With the window closed the list is read-only and
 * shows only the cancelled ones.
 */
@Component({
  selector: 'bf-admin-ratings-page',
  imports: [Avatar, Badge, Button, Countdown, EmptyState, List, ListItem, LoadError, Skeleton],
  templateUrl: './admin-ratings-page.html',
  styleUrl: './admin-ratings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminRatingsPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly api = inject(RatingApi);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);
  private readonly session = inject(SessionStore);

  protected readonly online = inject(Online);
  protected readonly ratings = this.api.ratings(this.leagueId, this.matchId, handling(409));
  private readonly members = inject(LeagueApi).members(this.leagueId);
  protected readonly displayName = displayName;
  /** The user whose ratings are being cancelled. */
  protected readonly cancelling = signal<string | null>(null);

  /** `MATCH_NOT_CLOSED` is a state of the view, not a failure (decision 16). */
  protected readonly notClosed = computed(
    () => toProblem(this.ratings.error())?.code === 'MATCH_NOT_CLOSED',
  );
  protected readonly failed = computed(
    () =>
      (this.ratings.error() !== undefined && !this.notClosed()) ||
      this.members.error() !== undefined,
  );
  protected readonly current = computed(() =>
    this.ratings.hasValue() ? this.ratings.value() : undefined,
  );
  protected readonly isOpen = computed(() => this.current()?.status === 'OPEN');
  protected readonly currentUserId = computed(() => this.session.user()?.id ?? null);

  /** The members that can be listed: all while the window is open, the cancelled once it is closed. */
  protected readonly rows = computed(() => {
    const ratings = this.current();
    if (ratings === undefined || !this.members.hasValue()) {
      return undefined;
    }
    const excluded = new Set(ratings.excludedUserIds ?? []);
    return this.members
      .value()
      .map((member) => ({ member, excluded: excluded.has(member.userId) }))
      .filter((row) => ratings.status === 'OPEN' || row.excluded);
  });

  constructor() {
    onVisible(() => {
      this.reload();
    });
  }

  protected reload(): void {
    this.ratings.reload();
    this.members.reload();
  }

  protected cancelLabel(member: MemberResponse): string {
    const name = displayName(member);
    return $localize`:@@rating.admin.ratings.cancelLabel:Cancelar valoraciones de ${name}:name:`;
  }

  protected cancel(member: MemberResponse): void {
    const leagueId = this.leagueId();
    const ratings = this.current();
    if (leagueId === undefined || ratings === undefined || this.cancelling() !== null) {
      return;
    }
    const name = displayName(member);
    const matchday = ratings.matchday;
    this.confirm
      .ask({
        title: $localize`:@@rating.admin.ratings.confirmTitle:¿Cancelar valoraciones?`,
        message: $localize`:@@rating.admin.ratings.confirmMessage:Se borran las valoraciones de ${name}:name: en la jornada ${matchday}:matchday: y no podrá volver a valorarla. No se puede deshacer.`,
        confirm: $localize`:@@rating.admin.ratings.confirm:Cancelar valoraciones`,
        tone: 'danger',
      })
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.cancelling.set(member.userId);
        this.api.exclude(leagueId, ratings.matchId, member.userId).subscribe({
          next: () => {
            this.cancelling.set(null);
            this.toasts.show({
              message: $localize`:@@rating.admin.ratings.cancelled:Valoraciones canceladas`,
            });
            this.ratings.reload();
          },
          error: (error: unknown) => {
            this.cancelling.set(null);
            const code = toProblem(error)?.code;
            if (code === 'RATING_WINDOW_CLOSED' || code === 'MATCH_NOT_CLOSED') {
              this.toasts.show({ message: ERROR_TEXTS[code], tone: 'danger' });
              this.reload();
            }
          },
        });
      });
  }
}
