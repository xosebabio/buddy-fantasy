import type { LeagueSettingsBody } from '../../core/api/league/league-api';
import type { PlayerResponse } from '../../core/api/squad/squad-api';
import type { LineupPlayerModel } from '../../shared/lineup/lineup-spots';
import { POSITIONS, type Position } from '../../shared/position/position';

/** What a lineup has to meet, from the settings of the league and the budget of the lineup (RF-24). */
export interface DraftRules {
  readonly size: number;
  /** The minimum per position; all 0 if the league does not ask for a formation. */
  readonly minimums: Record<Position, number>;
  /** In millions; `null` if the league has no budget. */
  readonly budget: number | null;
}

export function draftRules(settings: LeagueSettingsBody, budget: number | null): DraftRules {
  const on = settings.lineupFormationEnabled;
  return {
    size: settings.lineupSize,
    minimums: {
      GOALKEEPER: on ? settings.lineupMinGoalkeepers : 0,
      DEFENDER: on ? settings.lineupMinDefenders : 0,
      MIDFIELDER: on ? settings.lineupMinMidfielders : 0,
      FORWARD: on ? settings.lineupMinForwards : 0,
    },
    budget,
  };
}

/** What the local check finds in a draft: it is shown, it never stops saving (decision 10 of plan 12). */
export interface DraftCheck {
  readonly total: number;
  /** The free places, with a position where the formation still lacks it. */
  readonly freeSpots: (Position | null)[];
  /** Players over the size of the league. */
  readonly excess: number;
  /** The positions below their minimum. */
  readonly lacking: Position[];
  /** How much the total passes the budget by; 0 if it fits or there is no budget. */
  readonly overBudgetBy: number;
  /** Players that were dropped from the squad and have to be replaced. */
  readonly retired: number;
}

/** Size, formation and budget of a draft, against the current values of its players. */
export function checkDraft(
  players: readonly LineupPlayerModel[],
  rules: DraftRules,
  retiredIds: ReadonlySet<string>,
): DraftCheck {
  const total = players.reduce((sum, player) => sum + player.value, 0);
  const free = Math.max(0, rules.size - players.length);
  const lacking = POSITIONS.filter((position) => missing(players, rules, position) > 0);
  const withPosition = POSITIONS.flatMap((position) =>
    Array.from({ length: missing(players, rules, position) }, () => position),
  ).slice(0, free);
  return {
    total,
    freeSpots: [
      ...withPosition,
      ...Array.from({ length: free - withPosition.length }, (): null => null),
    ],
    excess: Math.max(0, players.length - rules.size),
    lacking,
    overBudgetBy:
      rules.budget !== null && total > rules.budget ? roundMillions(total - rules.budget) : 0,
    retired: players.filter((player) => retiredIds.has(player.id)).length,
  };
}

function missing(players: readonly LineupPlayerModel[], rules: DraftRules, position: Position) {
  const count = players.filter((player) => player.position === position).length;
  return Math.max(0, rules.minimums[position] - count);
}

/** Values have two decimals at most, so the sums are rounded to avoid `1.5000000000000036`. */
function roundMillions(millions: number): number {
  return Math.round(millions * 100) / 100;
}

/** The player of the squad as a spot of the lineup, with the value that counts now. */
export function lineupPlayer(player: PlayerResponse): LineupPlayerModel {
  return { ...player, value: player.currentValue };
}

/** Whether two drafts have the same players, in any order. */
export function sameLineup(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((id) => b.includes(id));
}
