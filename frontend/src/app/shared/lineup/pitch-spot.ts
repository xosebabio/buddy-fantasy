import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Bib, EmptyBib } from '../bib/bib';
import { BfMoneyPipe } from '../format/money';
import type { LineupSpot } from './lineup-spots';
import { FREE_SPOT } from './spot-label';

/**
 * A spot of `bf-pitch`: the bib, or the outline of a free place, with the name and the value. The
 * pitch names the button, since the name may be cut on screen. The spaces between its texts are in
 * the DOM (`&ngsp;`) because the grid does not draw them: axe reads the visible text with them and
 * checks that the name contains it (WCAG 2.5.3).
 */
@Component({
  selector: 'button[bfPitchSpot]',
  imports: [Bib, BfMoneyPipe, EmptyBib],
  template: `
    @let current = spot();
    @if (current.kind === 'player') {
      <bf-bib [position]="current.player.position" [number]="current.player.shirtNumber" />
      &ngsp;<span class="name">{{ current.player.name }}</span
      >&ngsp;<span class="value">{{ current.player.value | bfMoney }}</span>
    } @else {
      <bf-empty-bib />
      <span class="name">{{ freeSpot }}</span>
    }
  `,
  styleUrl: './pitch-spot.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    type: 'button',
    '[class.free]': 'spot().kind === "free"',
    '[class.selected]': 'selected()',
  },
})
export class PitchSpot {
  readonly spot = input.required<LineupSpot>({ alias: 'bfPitchSpot' });
  readonly selected = input(false);

  protected readonly freeSpot = FREE_SPOT;
}
