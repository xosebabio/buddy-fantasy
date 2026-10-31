import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchApi, type MatchResultResponse } from '../../core/api/match/match-api';
import { RatingApi } from '../../core/api/rating/rating-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { handling } from '../../core/http/error-interceptor';
import { ERROR_TEXTS } from '../../core/http/error-texts';
import { Online } from '../../core/http/online';
import { toProblem } from '../../core/http/problem';
import { onVisible } from '../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../core/layout/unsaved-changes';
import { performanceSummary } from '../../shared/performance/performance';
import { PlayerRow } from '../../shared/player/player-row';
import { RatingInput } from '../../shared/rating/rating-input';
import { playerIndex } from '../../shared/roster/roster';
import { Points } from '../../shared/scoring/points';
import { Button } from '../../ui/button/button';
import { Countdown } from '../../ui/countdown/countdown';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Notice } from '../../ui/notice/notice';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Toasts } from '../../ui/toast/toasts';
import { sameVotes, savedVotes, votedCount, votesRequest, type Votes } from './votes';

/** How long the page waits before it asks again for results that the server is still calculating. */
const RESULTS_RETRY_MS = 10_000;

/** The result of the match in the voice of the team: _Ganamos 3–1 a CD Arrabal_. */
function resultText(result: MatchResultResponse, opponent: string): string {
  const score = `${String(result.goalsFor)}–${String(result.goalsAgainst)}`;
  if (result.goalsFor > result.goalsAgainst) {
    return $localize`:@@rating.rate.won:Ganamos ${score}:score: a ${opponent}:opponent:`;
  }
  return result.goalsFor === result.goalsAgainst
    ? $localize`:@@rating.rate.drew:Empatamos ${score}:score: con ${opponent}:opponent:`
    : $localize`:@@rating.rate.lost:Perdimos ${score}:score: contra ${opponent}:opponent:`;
}

/**
 * Rates the players of a match (RF-18 to RF-23): a local draft with the user's votes in the format
 * frozen when the window opened, saved whole. Once the window closes, each row shows the user's
 * vote, the average and the points; an open window shows nothing of anyone else (RF-21).
 */
@Component({
  selector: 'bf-rate-match-page',
  imports: [
    Button,
    Countdown,
    EmptyState,
    List,
    LoadError,
    Notice,
    PlayerRow,
    Points,
    RatingInput,
    RouterLink,
    Skeleton,
  ],
  templateUrl: './rate-match-page.html',
  styleUrl: './rate-match-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RateMatchPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly ratingApi = inject(RatingApi);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly ratings = this.ratingApi.ratings(this.leagueId, this.matchId, handling(409));
  protected readonly match = inject(MatchApi).match(this.leagueId, this.matchId);
  protected readonly squad = inject(SquadApi).players(this.leagueId);
  protected readonly saving = signal(false);

  /** The votes being edited; `null` while the draft is what the server has. */
  private readonly edited = signal<Votes | null>(null);

  protected readonly notClosed = computed(
    () => toProblem(this.ratings.error())?.code === 'MATCH_NOT_CLOSED',
  );
  protected readonly failed = computed(
    () =>
      (this.ratings.error() !== undefined && !this.notClosed()) ||
      this.match.error() !== undefined ||
      this.squad.error() !== undefined,
  );

  /** The ratings, or `undefined` while loading or in error, where reading the resource would throw. */
  protected readonly current = computed(() =>
    this.ratings.hasValue() ? this.ratings.value() : undefined,
  );
  private readonly saved = computed(() => savedVotes(this.current()?.players ?? []));
  private readonly open = computed(() => this.current()?.status === 'OPEN');
  protected readonly votes = computed(() =>
    this.open() ? (this.edited() ?? this.saved()) : this.saved(),
  );
  protected readonly voted = computed(() => votedCount(this.votes()));
  readonly unsaved = computed(() => {
    const edited = this.edited();
    return this.open() && edited !== null && !sameVotes(edited, this.saved());
  });

  /** What the server has not calculated yet, once the window has closed. */
  protected readonly calculating = computed(() => {
    const ratings = this.current();
    return (
      ratings?.status === 'CLOSED' &&
      ratings.players.length > 0 &&
      ratings.players.every((player) => player.votes === null)
    );
  });
  protected readonly canRate = computed(() => this.open() && this.current()?.excluded === false);

  protected readonly summary = computed(() => {
    const match = this.match.hasValue() ? this.match.value() : undefined;
    return match?.result == null ? null : resultText(match.result, match.opponent);
  });

  /** The players of the sheet, crossed with the squad and with what each did in the match. */
  protected readonly rows = computed(() => {
    const ratings = this.current();
    const squad = this.squad.hasValue() ? this.squad.value() : undefined;
    const match = this.match.hasValue() ? this.match.value() : undefined;
    if (ratings === undefined || squad === undefined || match === undefined) {
      return undefined;
    }
    const index = playerIndex(squad);
    const performances = new Map((match.performances ?? []).map((p) => [p.playerId, p]));
    return ratings.players.flatMap((rating) => {
      const player = index.get(rating.playerId);
      const performance = performances.get(rating.playerId);
      return player === undefined
        ? []
        : [
            {
              rating,
              player: { ...player, position: rating.position },
              did: performance === undefined ? '' : performanceSummary(performance).join(', '),
            },
          ];
    });
  });

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      this.reload();
    });
    effect((onCleanup) => {
      if (this.calculating()) {
        const timer = setTimeout(() => {
          this.ratings.reload();
        }, RESULTS_RETRY_MS);
        onCleanup(() => {
          clearTimeout(timer);
        });
      }
    });
  }

  protected reload(): void {
    this.ratings.reload();
    this.match.reload();
    this.squad.reload();
  }

  protected vote(playerId: string, value: number | null): void {
    this.edited.set(new Map(this.votes()).set(playerId, value));
  }

  protected save(): void {
    const leagueId = this.leagueId();
    const matchId = this.matchId();
    const edited = this.edited();
    if (leagueId === undefined || matchId === undefined || edited === null || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.ratingApi.save(leagueId, matchId, votesRequest(edited)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.edited.set(null);
        this.ratings.set(saved);
        this.toasts.show({ message: $localize`:@@rating.rate.saved:Valoraciones guardadas` });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const code = toProblem(error)?.code;
        if (code) {
          // What was on screen no longer holds (the window closed, or the user was excluded).
          this.toasts.show({ message: ERROR_TEXTS[code], tone: 'danger' });
          this.edited.set(null);
          this.reload();
        }
      },
    });
  }
}
