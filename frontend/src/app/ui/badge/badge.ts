import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** `accent` for a role (_Admin_), `warning` for a deadline about to expire, `brand` for good news. */
export type BadgeTone = 'neutral' | 'brand' | 'accent' | 'warning';

/**
 * A short tag next to what it qualifies (FE-18). An icon goes before the text at 16 px
 * (`<svg lucideClock [size]="16"></svg>`); the color is never the only clue, the text says it.
 */
@Component({
  selector: 'bf-badge',
  template: '<ng-content />',
  styleUrl: './badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'tone()' },
})
export class Badge {
  readonly tone = input<BadgeTone>('neutral');
}
