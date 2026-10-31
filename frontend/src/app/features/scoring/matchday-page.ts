import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { LeagueApi } from '../../core/api/league/league-api';
import { MatchApi } from '../../core/api/match/match-api';
import {
  type MatchdayPlayerResponse,
  type MatchdayStandingResponse,
  ScoringApi,
} from '../../core/api/scoring/scoring-api';
import { type PlayerResponse, SquadApi } from '../../core/api/squad/squad-api';
import { handling } from '../../core/http/error-interceptor';
import { toProblem } from '../../core/http/problem';
import { BfPointsPipe } from '../../shared/format/points';
import { PlayerRow, type PlayerRowModel } from '../../shared/player/player-row';
import { memberIndex, memberName, playerIndex } from '../../shared/roster/roster';
import { Points } from '../../shared/scoring/points';
import { Avatar } from '../../ui/avatar/avatar';
import { Badge } from '../../ui/badge/badge';
import { Button } from '../../ui/button/button';
import { Disclosure } from '../../ui/disclosure/disclosure';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { Field, FieldSelect } from '../../ui/field/field';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Tab, TabPanel, Tabs } from '../../ui/tabs/tabs';
import { closedMatches } from './matchdays';
import { StandingsNav } from './standings-nav';

/** A player of a manager's lineup with what they scored in the matchday (0 if they did not play). */
export interface LineupEntry {
  readonly player: PlayerResponse;
  readonly sheet: MatchdayPlayerResponse | undefined;
}

/** A manager's row of the matchday, with their locked lineup crossed with the squad and the sheet. */
export interface ManagerRow {
  readonly standing: MatchdayStandingResponse;
  readonly name: string;
  readonly avatarUrl: string | null;
  readonly lineup: LineupEntry[];
}

/** A player of the sheet, with the squad's name and number and the position they played in. */
export interface SheetRow {
  readonly player: PlayerRowModel & { readonly id: string };
  readonly sheet: MatchdayPlayerResponse;
}

export function managerRows(
  standings: readonly MatchdayStandingResponse[],
  sheet: readonly MatchdayPlayerResponse[],
  squad: ReadonlyMap<string, PlayerResponse>,
  members: Parameters<typeof memberIndex>[0],
): ManagerRow[] {
  const people = memberIndex(members);
  const scored = new Map(sheet.map((player) => [player.id, player]));
  return standings.map((standing) => ({
    standing,
    name: memberName(people, standing.userId),
    avatarUrl: people.get(standing.userId)?.avatarUrl ?? null,
    lineup: standing.playerIds.flatMap((id) => {
      const player = squad.get(id);
      return player === undefined ? [] : [{ player, sheet: scored.get(id) }];
    }),
  }));
}

export function sheetRows(
  sheet: readonly MatchdayPlayerResponse[],
  squad: ReadonlyMap<string, PlayerResponse>,
): SheetRow[] {
  return sheet.flatMap((entry) => {
    const player = squad.get(entry.id);
    return player === undefined
      ? []
      : [{ player: { ...player, position: entry.position }, sheet: entry }];
  });
}

/**
 * The points of a matchday (RF-30 to RF-32): the managers, each opening their locked lineup with
 * the points of every player, and the players of the sheet. Until the rating window closes the
 * server answers `MATCH_NOT_CLOSED` or `MATCH_NOT_SCORED`, which are states of the page.
 */
@Component({
  selector: 'bf-matchday-page',
  imports: [
    Avatar,
    Badge,
    BfPointsPipe,
    Button,
    Disclosure,
    EmptyState,
    Field,
    FieldSelect,
    List,
    ListItem,
    LoadError,
    PlayerRow,
    Points,
    RouterLink,
    Skeleton,
    StandingsNav,
    Tab,
    TabPanel,
    Tabs,
  ],
  templateUrl: './matchday-page.html',
  styleUrl: './matchday-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchdayPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly router = inject(Router);

  protected readonly tab = signal('managers');

  protected readonly scores = inject(ScoringApi).matchday(
    this.leagueId,
    this.matchId,
    handling(409),
  );
  protected readonly squad = inject(SquadApi).players(this.leagueId);
  protected readonly members = inject(LeagueApi).members(this.leagueId);
  private readonly matches = inject(MatchApi).matches(this.leagueId);

  private readonly errorCode = computed(() => toProblem(this.scores.error())?.code);
  protected readonly notScored = computed(
    () => this.errorCode() === 'MATCH_NOT_CLOSED' || this.errorCode() === 'MATCH_NOT_SCORED',
  );
  /** Only a closed match can still have its ratings open: that one can be rated. */
  protected readonly rateable = computed(() => this.errorCode() === 'MATCH_NOT_SCORED');
  protected readonly failed = computed(
    () =>
      (this.scores.error() !== undefined && !this.notScored()) ||
      this.squad.error() !== undefined ||
      this.members.error() !== undefined,
  );

  protected readonly closed = computed(() => closedMatches(this.matches.value() ?? []));
  protected readonly matchday = computed(
    () => this.matches.value()?.find((match) => match.id === this.matchId())?.matchday,
  );

  protected readonly managers = computed(() => {
    const scores = this.scores.hasValue() ? this.scores.value() : undefined;
    const squad = this.squad.value();
    const members = this.members.value();
    return scores === undefined || squad === undefined || members === undefined
      ? undefined
      : managerRows(scores.standings, scores.players, playerIndex(squad), members);
  });
  protected readonly players = computed(() => {
    const scores = this.scores.hasValue() ? this.scores.value() : undefined;
    const squad = this.squad.value();
    return scores === undefined || squad === undefined
      ? undefined
      : sheetRows(scores.players, playerIndex(squad));
  });

  protected choose(matchId: string): void {
    void this.router.navigate(['/leagues', this.leagueId(), 'standings', matchId]);
  }

  protected reload(): void {
    this.scores.reload();
    this.squad.reload();
    this.members.reload();
  }
}
