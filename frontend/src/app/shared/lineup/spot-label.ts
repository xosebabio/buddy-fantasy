import { formatMoney } from '../format/money';
import { POSITION_TEXTS } from '../position/position';
import type { LineupSpot } from './lineup-spots';

export const FREE_SPOT = $localize`:@@shared.lineup.freeSpot:Plaza libre`;

/**
 * The accessible name of a spot: _9 Pablo 17,2 M, Delantero_ or _Plaza libre, Defensa_. What is seen
 * (the shirt number, the name and the value) goes first and in order, as WCAG 2.5.3 asks.
 */
export function spotLabel(spot: LineupSpot): string {
  if (spot.kind === 'player') {
    const { name, position, shirtNumber, value } = spot.player;
    const shirt = shirtNumber === null ? '' : `${String(shirtNumber)} `;
    return `${shirt}${name} ${formatMoney(value)}, ${POSITION_TEXTS[position]}`;
  }
  return spot.position === null ? FREE_SPOT : `${FREE_SPOT}, ${POSITION_TEXTS[spot.position]}`;
}
