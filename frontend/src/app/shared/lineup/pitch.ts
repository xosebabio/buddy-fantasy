import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import type { Position } from '../position/position';
import { type LineupPlayerModel, type LineupSpot, pitchLayout, spotKey } from './lineup-spots';
import { PitchSpot } from './pitch-spot';
import { spotLabel } from './spot-label';

/** A row of five bibs needs narrower spots to fit at 360 px. */
const DENSE_FROM = 5;

/**
 * The lineup on the pitch (FE-19): striped grass with a row per position, the own goal at the
 * bottom, and each player as a 44 px bib with their name and value. The free places with a position
 * go in its row and the others at the foot, outside the lines. Choosing is by touch, never by
 * dragging: every spot is a button, named with the name, the position and the value (the name may be
 * cut on screen), that emits `spotSelected`; `selected` rings it in lavender. `bf-lineup-list` is the
 * same as a list.
 */
@Component({
  selector: 'bf-pitch',
  imports: [PitchSpot],
  template: `
    <div class="field">
      <div class="lines" aria-hidden="true">
        <i class="halfway"></i><i class="circle"></i><i class="box"></i><i class="goal-box"></i>
      </div>
      <div class="rows">
        @for (row of layout().rows; track $index) {
          <div class="row">
            @for (spot of row; track key(spot)) {
              <button
                [bfPitchSpot]="spot"
                [selected]="key(spot) === selected()"
                [attr.aria-label]="spotLabel(spot)"
                (click)="spotSelected.emit(spot)"
              ></button>
            }
          </div>
        }
      </div>
    </div>
    @if (layout().foot.length > 0) {
      <div class="foot">
        @for (spot of layout().foot; track key(spot)) {
          <button
            [bfPitchSpot]="spot"
            [selected]="key(spot) === selected()"
            [attr.aria-label]="spotLabel(spot)"
            (click)="spotSelected.emit(spot)"
          ></button>
        }
      </div>
    }
  `,
  styleUrl: './pitch.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'group', '[attr.aria-label]': 'label()', '[class.dense]': 'dense()' },
})
export class Pitch {
  /** What the group is, such as _Tu alineación_. */
  readonly label = input.required<string>();
  readonly players = input.required<readonly LineupPlayerModel[]>();
  readonly freeSpots = input<readonly (Position | null)[]>([]);
  /** The `spotKey` of the spot being changed. */
  readonly selected = input<string | null>(null);
  readonly spotSelected = output<LineupSpot>();

  protected readonly layout = computed(() => pitchLayout(this.players(), this.freeSpots()));
  protected readonly dense = computed(() =>
    this.layout().rows.some((row) => row.length >= DENSE_FROM),
  );
  protected readonly key = spotKey;
  protected readonly spotLabel = spotLabel;
}
