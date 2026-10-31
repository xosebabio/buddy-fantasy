import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { POSITION_CLASSES, type Position } from '../position/position';

/** 30 px in lists, 44 px on the pitch, 72 px on the login (ui/README.md). */
export type BibSize = 'small' | 'medium' | 'large';

/**
 * The bib (FE-19): how a player is drawn, in the color of their position and with their shirt
 * number, if they have one. It is decorative: the name and the position go in text next to it.
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
  readonly number = input.required<number | null>();
  readonly size = input<BibSize>('medium');

  protected readonly classes = computed(
    () => `${this.size()} ${POSITION_CLASSES[this.position()]}`,
  );
}

/** The dashed outline of a bib, without a number: a gap to fill, such as a free place in a lineup. */
@Component({
  selector: 'bf-empty-bib',
  template: `
    <svg viewBox="0 0 44 50">
      <path d="M1 5.4 13.2 1 22 8.2 30.8 1 43 5.4V49H1z" />
    </svg>
  `,
  styleUrl: './empty-bib.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[class]': 'size()' },
})
export class EmptyBib {
  readonly size = input<BibSize>('medium');
}
