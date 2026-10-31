import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import type { components } from '../../core/api/schema';

export type Position = components['schemas']['LineupPlayerResponse']['position'];

/** 30 px in lists, 44 px on the pitch, 72 px on the login (ui/README.md). */
export type BibSize = 'small' | 'medium' | 'large';

const POSITION_CLASSES: Record<Position, string> = {
  GOALKEEPER: 'goalkeeper',
  DEFENDER: 'defender',
  MIDFIELDER: 'midfielder',
  FORWARD: 'forward',
};

/**
 * The bib (FE-19): how a player is drawn, in the color of their position and with their shirt
 * number. It is decorative: the name and the position go in text next to it.
 */
@Component({
  selector: 'bf-bib',
  template: '{{ number() }}',
  styleUrl: './bib.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[class]': 'classes()' },
})
export class Bib {
  readonly position = input.required<Position>();
  readonly number = input.required<number>();
  readonly size = input<BibSize>('medium');

  protected readonly classes = computed(
    () => `${this.size()} ${POSITION_CLASSES[this.position()]}`,
  );
}
