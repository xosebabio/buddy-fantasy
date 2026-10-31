import type { PlayerResponse } from '../../core/api/squad/squad-api';
import { POSITIONS, type Position } from '../../shared/position/position';

export interface Candidate {
  readonly player: PlayerResponse;
  /** The player being changed, who is already in the lineup. */
  readonly current: boolean;
  /** Whether it fits in what is left of the budget; the server decides on saving. */
  readonly fits: boolean;
}

export interface CandidateGroup {
  readonly position: Position;
  readonly candidates: Candidate[];
}

/**
 * The players that can take a place (decision 7 of plan 12): the active ones of the squad that are
 * not in the lineup, in groups by position with the one of the place first, plus the player being
 * changed. `left` is what remains of the budget without them, `null` if there is none.
 */
export function candidateGroups(
  squad: readonly PlayerResponse[],
  lineupIds: readonly string[],
  changing: PlayerResponse | undefined,
  position: Position | null,
  left: number | null,
): CandidateGroup[] {
  const first = changing?.position ?? position;
  const order = first === null ? POSITIONS : [first, ...POSITIONS.filter((p) => p !== first)];
  return order
    .map((group) => ({
      position: group,
      candidates: squad
        .filter(
          (player) =>
            player.position === group &&
            player.retiredAt === null &&
            (player.id === changing?.id || !lineupIds.includes(player.id)),
        )
        .map((player) => ({
          player,
          current: player.id === changing?.id,
          fits: left === null || player.currentValue <= left + EPSILON,
        })),
    }))
    .filter((group) => group.candidates.length > 0);
}

/** Absorbs the floating point error of comparing sums of values with two decimals. */
const EPSILON = 0.001;
