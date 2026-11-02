import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { LeagueApi } from '../../../core/api/league/league-api';
import { MatchApi } from '../../../core/api/match/match-api';
import { RatingApi } from '../../../core/api/rating/rating-api';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { handling } from '../../../core/http/error-interceptor';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { onVisible } from '../../../core/http/visible';
import { MATCH_PHASE_TEXTS, matchPhase } from '../../../shared/match/match-phase';
import { MATCH_WARNING_TEXTS } from '../../../shared/match/match-warning';
import { VENUE_TEXTS } from '../../../shared/match/venue';
import { BfDatePipe } from '../../../shared/time/date-pipe';
import { Badge } from '../../../ui/badge/badge';
import { Button } from '../../../ui/button/button';
import { Confirm } from '../../../ui/confirm/confirm';
import { Countdown } from '../../../ui/countdown/countdown';
import { List, ListItem } from '../../../ui/list/list';
import { LoadError } from '../../../ui/load-error/load-error';
import { Notice } from '../../../ui/notice/notice';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Toasts } from '../../../ui/toast/toasts';

/**
 * A match for the admin (`admin/matches/:matchId`, RF-12 to RF-16): its state, the rows that lead to
 * its data, its sheet, its ratings and its audit, and, while it is not closed, deleting it. The
 * state uses the browser's clock, so it is a guide (FE-09): the server decides what can be done.
 */
@Component({
  selector: 'bf-admin-match-page',
  imports: [
    BfDatePipe,
    Badge,
    Button,
    Countdown,
    List,
    ListItem,
    LoadError,
    Notice,
    RouterLink,
    Skeleton,
  ],
  templateUrl: './admin-match-page.html',
  styleUrl: './admin-match-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminMatchPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly api = inject(MatchApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly match = this.api.match(this.leagueId, this.matchId);
  /** The rating window exists once the match is closed; before, the server answers `409`. */
  protected readonly ratings = inject(RatingApi).ratings(
    this.leagueId,
    () => (this.current()?.status === 'CLOSED' ? this.matchId() : undefined),
    handling(409),
  );
  /** For the hours of the rating window that closing opens; the page works without it. */
  protected readonly settings = inject(LeagueApi).settings(this.leagueId);
  protected readonly deleting = signal(false);
  protected readonly closing = signal(false);

  protected readonly venueTexts = VENUE_TEXTS;
  protected readonly phaseTexts = MATCH_PHASE_TEXTS;
  protected readonly warningTexts = MATCH_WARNING_TEXTS;

  protected readonly current = computed(() =>
    this.match.hasValue() ? this.match.value() : undefined,
  );
  protected readonly phase = computed(() => {
    const match = this.current();
    return match === undefined ? undefined : matchPhase(match, new Date());
  });
  protected readonly window = computed(() =>
    this.ratings.hasValue() ? this.ratings.value() : undefined,
  );
  protected readonly players = computed(() => this.current()?.performances?.length ?? 0);

  constructor() {
    onVisible(() => {
      this.reload();
    });
  }

  protected reload(): void {
    this.match.reload();
    this.ratings.reload();
    this.settings.reload();
  }

  protected close(): void {
    const leagueId = this.leagueId();
    const match = this.current();
    if (leagueId === undefined || match === undefined || this.closing()) {
      return;
    }
    const hours = this.settings.hasValue() ? this.settings.value().ratingWindowHours : undefined;
    this.confirm
      .ask({
        title: $localize`:@@match.admin.match.closeTitle:¿Cerrar el partido?`,
        message:
          hours === undefined
            ? $localize`:@@match.admin.match.closeMessageShort:Se bloquean las alineaciones que no lo estén, se publica el acta y se abre la valoración.`
            : $localize`:@@match.admin.match.closeMessage:Se bloquean las alineaciones que no lo estén, se publica el acta y se abre la valoración durante ${hours}:hours: h.`,
        confirm: $localize`:@@match.admin.match.closeConfirm:Cerrar partido`,
      })
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.closing.set(true);
        this.api.close(leagueId, match.id).subscribe({
          next: () => {
            this.closing.set(false);
            this.toasts.show({ message: $localize`:@@match.admin.match.closed:Partido cerrado` });
            this.reload();
          },
          error: (error: unknown) => {
            this.closing.set(false);
            const code = toProblem(error)?.code;
            if (code === 'MATCH_NOT_STARTED' || code === 'MATCH_SHEET_INCOMPLETE') {
              this.toasts.show({ message: ERROR_TEXTS[code] });
              this.reload();
            }
          },
        });
      });
  }

  protected remove(): void {
    const leagueId = this.leagueId();
    const match = this.current();
    if (leagueId === undefined || match === undefined || this.deleting()) {
      return;
    }
    this.confirm
      .ask({
        title: $localize`:@@match.admin.match.deleteTitle:¿Borrar el partido?`,
        message: $localize`:@@match.admin.match.deleteMessage:Se borra el partido de la jornada ${match.matchday}:matchday:. Las alineaciones guardadas pasan al siguiente.`,
        confirm: $localize`:@@match.admin.match.deleteConfirm:Borrar partido`,
        tone: 'danger',
      })
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.deleting.set(true);
        this.api.delete(leagueId, match.id).subscribe({
          next: () => {
            this.toasts.show({ message: $localize`:@@match.admin.match.deleted:Partido borrado` });
            void this.router.navigate(['/leagues', leagueId, 'admin', 'matches']);
          },
          error: (error: unknown) => {
            this.deleting.set(false);
            if (toProblem(error)?.code === 'MATCH_CLOSED') {
              this.toasts.show({ message: ERROR_TEXTS.MATCH_CLOSED });
              this.reload();
            }
          },
        });
      });
  }
}
