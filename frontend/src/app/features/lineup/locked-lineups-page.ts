import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LeagueApi } from '../../core/api/league/league-api';
import { LineupApi, type LockedLineupResponse } from '../../core/api/lineup/lineup-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { handling } from '../../core/http/error-interceptor';
import { toProblem } from '../../core/http/problem';
import { BfMoneyPipe } from '../../shared/format/money';
import { PlayerRow } from '../../shared/player/player-row';
import { memberIndex, memberName, playerIndex } from '../../shared/roster/roster';
import { Avatar } from '../../ui/avatar/avatar';
import { Badge } from '../../ui/badge/badge';
import { Disclosure } from '../../ui/disclosure/disclosure';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';

/**
 * The lineups of every manager locked for a match (RF-27): a row per manager that opens to show
 * their players with the value at locking, the total and whether it went over the budget. Before
 * locking the server answers `LINEUPS_NOT_LOCKED`, which is a state of the page, not an error.
 */
@Component({
  selector: 'bf-locked-lineups-page',
  imports: [
    Avatar,
    Badge,
    BfMoneyPipe,
    Disclosure,
    EmptyState,
    List,
    LoadError,
    PlayerRow,
    Skeleton,
  ],
  templateUrl: './locked-lineups-page.html',
  styleUrl: './locked-lineups-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LockedLineupsPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  protected readonly locked = inject(LineupApi).locked(this.leagueId, this.matchId, handling(409));
  protected readonly squad = inject(SquadApi).players(this.leagueId);
  protected readonly members = inject(LeagueApi).members(this.leagueId);

  protected readonly notLocked = computed(
    () => toProblem(this.locked.error())?.code === 'LINEUPS_NOT_LOCKED',
  );
  protected readonly failed = computed(
    () =>
      (this.locked.error() !== undefined && !this.notLocked()) ||
      this.squad.error() !== undefined ||
      this.members.error() !== undefined,
  );

  /** The managers by name, each with their players crossed with the whole squad. */
  protected readonly rows = computed(() => {
    const locked = this.locked.value();
    const squad = this.squad.value();
    const members = this.members.value();
    if (locked === undefined || squad === undefined || members === undefined) {
      return undefined;
    }
    const players = playerIndex(squad);
    const people = memberIndex(members);
    return locked.lineups
      .map((lineup) => lockedRow(lineup, players, people))
      .sort((a, b) => a.name.localeCompare(b.name, 'es'));
  });

  protected reload(): void {
    this.locked.reload();
    this.squad.reload();
    this.members.reload();
  }
}

function lockedRow(
  lineup: LockedLineupResponse,
  players: ReturnType<typeof playerIndex>,
  people: ReturnType<typeof memberIndex>,
) {
  return {
    lineup,
    name: memberName(people, lineup.userId),
    avatarUrl: people.get(lineup.userId)?.avatarUrl ?? null,
    players: lineup.players.flatMap((locked) => {
      const player = players.get(locked.id);
      return player === undefined ? [] : [{ player, value: locked.value }];
    }),
  };
}
