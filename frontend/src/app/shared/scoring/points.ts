import { booleanAttribute, ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BfPointsPipe } from '../format/points';

/** `list` for a row or a breakdown, `hero` for the figure that leads a screen (paso 7). */
export type PointsSize = 'list' | 'hero';

/**
 * Points (FE-19), in condensed with their sign: in `brand-text` if they add, in `danger-text` if
 * they subtract and neutral at 0. `unsigned` leaves the plus out, as the totals of the standings.
 */
@Component({
  selector: 'bf-points',
  imports: [BfPointsPipe],
  template: '{{ value() | bfPoints: !unsigned() }}',
  styleUrl: './points.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'size()',
    '[class.positive]': '!unsigned() && value() > 0',
    '[class.negative]': 'value() < 0',
  },
})
export class Points {
  readonly value = input.required<number>();
  readonly size = input<PointsSize>('list');
  readonly unsigned = input(false, { transform: booleanAttribute });
}
