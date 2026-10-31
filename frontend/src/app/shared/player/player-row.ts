import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';

import type { PlayerResponse } from '../../core/api/squad/squad-api';
import { Bib } from '../bib/bib';
import { BfMoneyPipe } from '../format/money';
import { POSITION_TEXTS } from '../position/position';

/**
 * What a row shows of a player. The position can be the one they played with, as lineups and
 * sheets give it, rather than the current one.
 */
export type PlayerRowModel = Pick<PlayerResponse, 'name' | 'position' | 'shirtNumber'>;

/**
 * The player card of FE-19 as a row, without a photo (decision 5 of plan 12): the small bib, the
 * name and, below, the position in text. `value` adds the value in millions at the end, and
 * `[bfPlayerRowMeta]` (what they did, _Dado de baja_) and `[bfPlayerRowTrailing]` (a state, such as
 * _En la alineación_) are optional. It is the content of a row: it goes inside a `bfListItem` (an
 * `<li>`, or a link or button when the whole row acts) or a `bfDisclosure`.
 */
@Component({
  selector: 'bf-player-row',
  imports: [Bib, BfMoneyPipe],
  template: `
    <bf-bib size="small" [position]="player().position" [number]="player().shirtNumber" />
    <span class="body">
      <span class="name">{{ player().name }}</span>
      <span class="meta">
        @if (showPosition()) {
          <span>{{ positionText() }}</span>
        }
        <ng-content select="[bfPlayerRowMeta]" />
      </span>
    </span>
    <ng-content select="[bfPlayerRowTrailing]" />
    @let millions = value();
    @if (millions !== null) {
      <span class="value">{{ millions | bfMoney }}</span>
    }
  `,
  styleUrl: './player-row.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerRow {
  readonly player = input.required<PlayerRowModel>();
  /** The value in millions; nothing without it. */
  readonly value = input<number | null>(null);
  /** Off where the position is already said, such as in a group of candidates by position. */
  readonly showPosition = input(true, { transform: booleanAttribute });

  protected readonly positionText = computed(() => POSITION_TEXTS[this.player().position]);
}
