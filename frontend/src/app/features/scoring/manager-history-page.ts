import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LeagueApi } from '../../core/api/league/league-api';
import { ScoringApi } from '../../core/api/scoring/scoring-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { PlayerRow } from '../../shared/player/player-row';
import { memberIndex, memberName, playerIndex } from '../../shared/roster/roster';
import { Points } from '../../shared/scoring/points';
import { PointsBreakdown } from '../../shared/scoring/points-breakdown';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Badge } from '../../ui/badge/badge';
import { Disclosure } from '../../ui/disclosure/disclosure';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { latestFirst } from './matchdays';

/**
 * The history of a manager (RF-33): a row per scored matchday, from the latest, that opens to show
 * their players with where each one's points come from. The matchdays before their last join, and
 * the ones over the budget, say so.
 */
@Component({
  selector: 'bf-manager-history-page',
  imports: [
    Badge,
    BfDatePipe,
    Disclosure,
    EmptyState,
    List,
    LoadError,
    PlayerRow,
    Points,
    PointsBreakdown,
    Skeleton,
  ],
  templateUrl: './manager-history-page.html',
  styleUrl: './manager-history-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ManagerHistoryPage {
  readonly leagueId = input<string>();
  readonly userId = input<string>();

  protected readonly history = inject(ScoringApi).managerHistory(this.leagueId, this.userId);
  protected readonly squad = inject(SquadApi).players(this.leagueId);
  protected readonly members = inject(LeagueApi).members(this.leagueId);

  protected readonly failed = computed(
    () =>
      this.history.error() !== undefined ||
      this.squad.error() !== undefined ||
      this.members.error() !== undefined,
  );
  protected readonly name = computed(() => {
    const members = this.members.value();
    const userId = this.userId();
    return members === undefined || userId === undefined
      ? undefined
      : memberName(memberIndex(members), userId);
  });
  protected readonly players = computed(() => {
    const squad = this.squad.value();
    return squad === undefined ? undefined : playerIndex(squad);
  });
  protected readonly matchdays = computed(() => latestFirst(this.history.value() ?? []));

  protected reload(): void {
    this.history.reload();
    this.squad.reload();
    this.members.reload();
  }
}
