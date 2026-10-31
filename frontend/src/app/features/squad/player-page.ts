import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ScoringApi, type PlayerMatchdayResponse } from '../../core/api/scoring/scoring-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { Bib } from '../../shared/bib/bib';
import { BfMoneyPipe } from '../../shared/format/money';
import { PositionChip } from '../../shared/position/position-chip';
import { Points } from '../../shared/scoring/points';
import { PointsBreakdown } from '../../shared/scoring/points-breakdown';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Badge } from '../../ui/badge/badge';
import { Disclosure } from '../../ui/disclosure/disclosure';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { Figure } from '../../ui/figure/figure';
import { List } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';

/** A player's matchdays from the most recent one: the API gives them from the oldest. */
export function latestFirst(
  matchdays: readonly PlayerMatchdayResponse[],
): PlayerMatchdayResponse[] {
  return [...matchdays].sort((a, b) => b.matchday - a.matchday);
}

/**
 * The sheet of a player (RF-11): their bib, position and value, and the points of each matchday,
 * which open to show where they come from. Retired players still have one.
 */
@Component({
  selector: 'bf-player-page',
  imports: [
    Badge,
    BfDatePipe,
    BfMoneyPipe,
    Bib,
    Disclosure,
    EmptyState,
    Figure,
    List,
    LoadError,
    Points,
    PointsBreakdown,
    PositionChip,
    Skeleton,
  ],
  templateUrl: './player-page.html',
  styleUrl: './player-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerPage {
  readonly leagueId = input<string>();
  readonly playerId = input<string>();

  protected readonly player = inject(SquadApi).player(this.leagueId, this.playerId);
  protected readonly points = inject(ScoringApi).playerPoints(this.leagueId, this.playerId);
  protected readonly matchdays = computed(() => latestFirst(this.points.value() ?? []));
}
