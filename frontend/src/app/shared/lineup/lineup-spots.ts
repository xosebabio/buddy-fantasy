import type { PlayerResponse } from '../../core/api/squad/squad-api';
import { POSITIONS, type Position } from '../position/position';

/** A player of the lineup, with the value that counts for the budget. */
export type LineupPlayerModel = Pick<PlayerResponse, 'id' | 'name' | 'position' | 'shirtNumber'> & {
  readonly value: number;
};

/**
 * What can be touched in a lineup: a player, or a free place (the n-th of `freeSpots`). A free place
 * has a position when the formation still lacks it, and goes in its row; otherwise it is `null`.
 */
export type LineupSpot =
  | { readonly kind: 'player'; readonly player: LineupPlayerModel }
  | { readonly kind: 'free'; readonly index: number; readonly position: Position | null };

/** At most five bibs fit side by side at 360 px; a longer row is split (risk of §12 of plan 12). */
const ROW_MAX = 5;

/** What identifies a spot, for `selected` and to track it. */
export function spotKey(spot: LineupSpot): string {
  return spot.kind === 'player' ? spot.player.id : `free-${String(spot.index)}`;
}

/** The free places with their index, which a page gets back when one is touched. */
export function freeSpots(positions: readonly (Position | null)[]): LineupSpot[] {
  return positions.map((position, index) => ({ kind: 'free', index, position }));
}

/** The spots in list order: from the goalkeeper forwards, then the free places without a position. */
export function listSpots(
  players: readonly LineupPlayerModel[],
  free: readonly (Position | null)[],
): LineupSpot[] {
  const spots = freeSpots(free);
  return [
    ...POSITIONS.flatMap((position) => positionSpots(players, spots, position)),
    ...spots.filter((spot) => spot.kind === 'free' && spot.position === null),
  ];
}

export interface PitchLayout {
  /** From the forwards, at the top, to the own goal, at the bottom; at most five spots a row. */
  readonly rows: readonly (readonly LineupSpot[])[];
  /** The free places without a position, at the foot of the pitch. */
  readonly foot: readonly LineupSpot[];
}

/** Where each spot goes on the pitch: a row for each position that has any, split if too long. */
export function pitchLayout(
  players: readonly LineupPlayerModel[],
  free: readonly (Position | null)[],
): PitchLayout {
  const spots = freeSpots(free);
  return {
    rows: [...POSITIONS]
      .reverse()
      .flatMap((position) => splitRow(positionSpots(players, spots, position))),
    foot: spots.filter((spot) => spot.kind === 'free' && spot.position === null),
  };
}

function positionSpots(
  players: readonly LineupPlayerModel[],
  spots: readonly LineupSpot[],
  position: Position,
): LineupSpot[] {
  return [
    ...players
      .filter((player) => player.position === position)
      .map((player): LineupSpot => ({ kind: 'player', player })),
    ...spots.filter((spot) => spot.kind === 'free' && spot.position === position),
  ];
}

/** A row in as few rows of at most `ROW_MAX` as it takes, as even as they can be: 7 is 4 and 3. */
function splitRow(spots: readonly LineupSpot[]): LineupSpot[][] {
  const count = Math.ceil(spots.length / ROW_MAX);
  const rows: LineupSpot[][] = [];
  let start = 0;
  for (let row = 0; row < count; row++) {
    const size = Math.ceil((spots.length - start) / (count - row));
    rows.push(spots.slice(start, start + size));
    start += size;
  }
  return rows;
}
