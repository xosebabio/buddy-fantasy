import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LeagueApi } from '../../core/api/league/league-api';
import { MatchApi } from '../../core/api/match/match-api';
import { ScoringApi, type StandingResponse } from '../../core/api/scoring/scoring-api';
import { SessionStore } from '../../core/auth/session-store';
import { BfPointsPipe } from '../../shared/format/points';
import { memberIndex, memberName } from '../../shared/roster/roster';
import { Points } from '../../shared/scoring/points';
import { Avatar } from '../../ui/avatar/avatar';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { closedMatches } from './matchdays';
import { StandingsNav } from './standings-nav';

/** A standing with who it is and whether its position repeats the one above (a tie). */
export interface StandingRow {
  readonly standing: StandingResponse;
  readonly name: string;
  readonly avatarUrl: string | null;
  readonly tied: boolean;
}

/** The standings crossed with the members; a manager who left is «Antiguo miembro». */
export function standingRows(
  standings: readonly StandingResponse[],
  members: Parameters<typeof memberIndex>[0],
): StandingRow[] {
  const people = memberIndex(members);
  return standings.map((standing, index) => ({
    standing,
    name: memberName(people, standing.userId),
    avatarUrl: people.get(standing.userId)?.avatarUrl ?? null,
    tied: standings[index - 1]?.position === standing.position,
  }));
}

/**
 * The overall standings (RF-32): a row per manager with their position, total, matchdays and the
 * points of the last one; each opens the manager's history. Ties share the position.
 */
@Component({
  selector: 'bf-standings-page',
  imports: [
    Avatar,
    BfPointsPipe,
    EmptyState,
    List,
    ListItem,
    LoadError,
    Points,
    RouterLink,
    Skeleton,
    StandingsNav,
  ],
  templateUrl: './standings-page.html',
  styleUrl: './standings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandingsPage {
  readonly leagueId = input<string>();

  protected readonly session = inject(SessionStore);
  protected readonly standings = inject(ScoringApi).standings(this.leagueId);
  protected readonly members = inject(LeagueApi).members(this.leagueId);
  private readonly matches = inject(MatchApi).matches(this.leagueId);

  protected readonly failed = computed(
    () => this.standings.error() !== undefined || this.members.error() !== undefined,
  );
  protected readonly rows = computed(() => {
    const standings = this.standings.value();
    const members = this.members.value();
    return standings === undefined || members === undefined
      ? undefined
      : standingRows(standings, members);
  });
  protected readonly started = computed(
    () => this.standings.value()?.some((standing) => standing.matchdays > 0) ?? false,
  );
  /** The matchday that «Por jornada» opens: the latest closed one. */
  protected readonly latestMatchId = computed(
    () => closedMatches(this.matches.value() ?? [])[0]?.id ?? null,
  );

  protected reload(): void {
    this.standings.reload();
    this.members.reload();
  }
}
