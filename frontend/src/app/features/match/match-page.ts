import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchApi } from '../../core/api/match/match-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { PlayerRow } from '../../shared/player/player-row';
import { playerIndex } from '../../shared/roster/roster';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { performanceSummary } from '../../shared/performance/performance';
import { VENUE_TEXTS } from './venue';

/**
 * A match with its result and, once it is closed, its sheet (RF-13, RF-14) and the way to rate it,
 * to see its points and to see the lineups. The sheet crosses its player ids with the whole squad.
 */
@Component({
  selector: 'bf-match-page',
  imports: [BfDatePipe, EmptyState, List, ListItem, LoadError, PlayerRow, RouterLink, Skeleton],
  templateUrl: './match-page.html',
  styleUrl: './match-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  protected readonly match = inject(MatchApi).match(this.leagueId, this.matchId);
  protected readonly players = inject(SquadApi).players(this.leagueId);
  protected readonly venueTexts = VENUE_TEXTS;

  /** The sheet with each performance next to its player, which the squad always has. */
  protected readonly sheet = computed(() => {
    const index = playerIndex(this.players.value() ?? []);
    return (this.match.value()?.performances ?? []).flatMap((performance) => {
      const player = index.get(performance.playerId);
      return player === undefined
        ? []
        : [
            {
              performance,
              player: { ...player, position: performance.position },
              summary: performanceSummary(performance).join(', '),
            },
          ];
    });
  });
}
