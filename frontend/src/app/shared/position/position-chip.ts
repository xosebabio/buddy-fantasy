import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { POSITION_CLASSES, POSITION_TEXTS, type Position } from './position';

/** A position in text on its color (FE-19), such as in the player card. */
@Component({
  selector: 'bf-position-chip',
  template: '{{ text() }}',
  styleUrl: './position-chip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'colorClass()' },
})
export class PositionChip {
  readonly position = input.required<Position>();

  protected readonly text = computed(() => POSITION_TEXTS[this.position()]);
  protected readonly colorClass = computed(() => POSITION_CLASSES[this.position()]);
}
