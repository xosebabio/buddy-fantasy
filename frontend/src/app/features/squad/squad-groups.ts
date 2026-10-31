import type { PlayerResponse } from '../../core/api/squad/squad-api';
import { POSITIONS, type Position } from '../../shared/position/position';

export interface SquadGroup {
  readonly position: Position;
  readonly players: PlayerResponse[];
}

/**
 * The active players of the squad grouped by position, from the own goal forwards, and each group by
 * name. The retired ones are left out (decision 1 of plan 12): the API gives the whole squad so that
 * other screens can cross their ids, but the list is of who plays. A position without players has
 * no group.
 */
export function squadGroups(players: readonly PlayerResponse[]): SquadGroup[] {
  const active = players.filter((player) => player.retiredAt === null);
  return POSITIONS.map((position) => ({
    position,
    players: active
      .filter((player) => player.position === position)
      .sort((a, b) => a.name.localeCompare(b.name, 'es')),
  })).filter((group) => group.players.length > 0);
}
