import { formatMoney } from '../format/money';
import { POSITION_TEXTS } from '../position/position';
import type { LineupSpot } from './lineup-spots';

export const FREE_SPOT = $localize`:@@shared.lineup.freeSpot:Plaza libre`;

/** The accessible name of a spot: _Pablo, Delantero, 17,2 M_ or _Plaza libre, Defensa_. */
export function spotLabel(spot: LineupSpot): string {
  if (spot.kind === 'player') {
    const { name, position, value } = spot.player;
    return `${name}, ${POSITION_TEXTS[position]}, ${formatMoney(value)}`;
  }
  return spot.position === null ? FREE_SPOT : `${FREE_SPOT}, ${POSITION_TEXTS[spot.position]}`;
}
