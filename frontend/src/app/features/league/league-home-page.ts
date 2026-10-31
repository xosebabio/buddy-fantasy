import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideLoaderCircle } from '@lucide/angular';

import { LineupApi, type LineupWarning } from '../../core/api/lineup/lineup-api';
import { MatchApi, type MatchSummaryResponse } from '../../core/api/match/match-api';
import { RatingApi, type RatingWindowResponse } from '../../core/api/rating/rating-api';
import { ScoringApi } from '../../core/api/scoring/scoring-api';
import { SessionStore } from '../../core/auth/session-store';
import { handling } from '../../core/http/error-interceptor';
import { toProblem } from '../../core/http/problem';
import { onVisible } from '../../core/http/visible';
import { BfMoneyPipe } from '../../shared/format/money';
import { Points } from '../../shared/scoring/points';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Button } from '../../ui/button/button';
import { Countdown } from '../../ui/countdown/countdown';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Notice } from '../../ui/notice/notice';
import { Skeleton } from '../../ui/skeleton/skeleton';

/** What each warning of the lineup says; a new one breaks the build until it has its text. */
const WARNING_TEXTS: Record<LineupWarning, string> = {
  OVER_BUDGET: $localize`:@@league.home.warning.overBudget:Con los valores actuales te pasas del presupuesto. Si se bloquea así, esta jornada no puntúa.`,
  RETIRED_PLAYERS: $localize`:@@league.home.warning.retired:Hay jugadores dados de baja. Cámbialos por otros: no cuenta como cambio.`,
  SIZE_MISMATCH: $localize`:@@league.home.warning.size:La liga ha cambiado el tamaño de las alineaciones.`,
};

/** The open window that the user still has players to rate in, the first to close. */
export function pendingWindow(
  windows: readonly RatingWindowResponse[],
): RatingWindowResponse | undefined {
  return windows
    .filter((w) => w.status === 'OPEN' && !w.excluded && w.myRatings < w.players)
    .sort((a, b) => a.closesAt.localeCompare(b.closesAt))[0];
}

/**
 * The latest scheduled match whose lineups are already locked, or about to be: those before the
 * one the lineup counts for now (all of them when there is none left to play).
 */
export function lockedMatch(
  matches: readonly MatchSummaryResponse[],
  targetMatchday: number | undefined,
): MatchSummaryResponse | undefined {
  return matches
    .filter(
      (m) =>
        m.status === 'SCHEDULED' && (targetMatchday === undefined || m.matchday < targetMatchday),
    )
    .sort((a, b) => b.matchday - a.matchday)[0];
}

/**
 * The start of a league, the matchday (RF-17, RF-25, RF-32): the next match with its lock deadline,
 * the state of the user's lineup, the rating windows still open, their last matchday and their
 * place in the standings, and the main action that fits (decision 14 of plan 12).
 */
@Component({
  selector: 'bf-league-home-page',
  imports: [
    BfDatePipe,
    BfMoneyPipe,
    Button,
    Countdown,
    EmptyState,
    List,
    ListItem,
    LoadError,
    LucideLoaderCircle,
    Notice,
    Points,
    RouterLink,
    Skeleton,
  ],
  templateUrl: './league-home-page.html',
  styleUrl: './league-home-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeagueHomePage {
  readonly leagueId = input<string>();

  private readonly session = inject(SessionStore);
  protected readonly lineup = inject(LineupApi).myLineup(this.leagueId);
  protected readonly windows = inject(RatingApi).windows(this.leagueId);
  protected readonly standings = inject(ScoringApi).standings(this.leagueId);
  protected readonly matches = inject(MatchApi).matches(this.leagueId);

  protected readonly warningTexts = WARNING_TEXTS;

  protected readonly target = computed(() => this.lineup.value()?.targetMatch ?? null);
  protected readonly targetSummary = computed(() => {
    const id = this.target()?.id;
    return this.matches.value()?.find((m) => m.id === id);
  });
  protected readonly pending = computed(() => pendingWindow(this.windows.value() ?? []));
  private readonly locked = computed(() =>
    lockedMatch(this.matches.value() ?? [], this.target()?.matchday),
  );
  protected readonly lockedId = computed(() => this.locked()?.id);
  /** The lineups of the locked match, only to know whether the job has locked them yet. */
  private readonly lockedLineups = inject(LineupApi).locked(
    this.leagueId,
    this.lockedId,
    handling(409),
  );
  protected readonly locking = computed(
    () => toProblem(this.lockedLineups.error())?.code === 'LINEUPS_NOT_LOCKED',
  );
  protected readonly lockedMatchday = computed(() => this.locked()?.matchday);

  protected readonly mine = computed(() => {
    const userId = this.session.user()?.id;
    const standings = this.standings.value();
    const mine = standings?.find((s) => s.userId === userId);
    return mine !== undefined && mine.matchdays > 0
      ? { standing: mine, managers: standings?.length ?? 0 }
      : undefined;
  });
  /** The matchday of the user's last points: the standings give the points, not which matchday. */
  private readonly history = inject(ScoringApi).managerHistory(this.leagueId, () =>
    this.mine() === undefined ? undefined : this.session.user()?.id,
  );
  protected readonly lastMatchday = computed(
    () =>
      (this.history.value() ?? [])
        .filter((m) => m.inStandings)
        .sort((a, b) => b.matchday - a.matchday)[0]?.matchday,
  );

  protected readonly failed = computed(
    () =>
      this.lineup.error() !== undefined ||
      this.windows.error() !== undefined ||
      this.standings.error() !== undefined ||
      this.matches.error() !== undefined,
  );
  protected readonly loaded = computed(
    () =>
      this.lineup.hasValue() &&
      this.windows.hasValue() &&
      this.standings.hasValue() &&
      this.matches.hasValue(),
  );
  protected readonly empty = computed(
    () =>
      this.target() === null &&
      this.locked() === undefined &&
      this.pending() === undefined &&
      this.mine() === undefined,
  );

  constructor() {
    onVisible(() => {
      this.reload();
    });
  }

  protected reload(): void {
    this.lineup.reload();
    this.windows.reload();
    this.standings.reload();
    this.matches.reload();
    this.lockedLineups.reload();
    this.history.reload();
  }
}
