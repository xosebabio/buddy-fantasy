import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { List, ListItem } from '../../ui/list/list';
import { EmptyBib } from '../bib/bib';
import { PlayerRow } from '../player/player-row';
import { POSITION_TEXTS, type Position } from '../position/position';
import { type LineupPlayerModel, type LineupSpot, listSpots, spotKey } from './lineup-spots';
import { FREE_SPOT } from './spot-label';

/**
 * The lineup as a list (FE-19), the alternative to `bf-pitch` with the same inputs and outputs: a
 * compact row per player, from the goalkeeper forwards, with the bib, the name, the position and
 * the value, and a row per free place. Every row is a button that emits `spotSelected`; the
 * `selected` one is marked in lavender.
 */
@Component({
  selector: 'bf-lineup-list',
  imports: [EmptyBib, List, ListItem, PlayerRow],
  template: `
    <ul bfList [attr.aria-label]="label()">
      @for (spot of spots(); track key(spot)) {
        <li [class.selected]="key(spot) === selected()">
          <button bfListItem type="button" size="compact" (click)="spotSelected.emit(spot)">
            <!-- One block per slot: the projection into a slot needs a single node in its block. -->
            @if (spot.kind === 'free') {
              <bf-empty-bib bfListItemLeading size="small" />
            }
            @if (spot.kind === 'player') {
              <bf-player-row [player]="spot.player" [value]="spot.player.value" />
            } @else {
              {{ freeSpot }}
            }
            @if (spot.kind === 'free' && spot.position !== null) {
              <span bfListItemMeta>{{ positionTexts[spot.position] }}</span>
            }
          </button>
        </li>
      }
    </ul>
  `,
  styleUrl: './lineup-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LineupList {
  /** What the list is, such as _Tu alineación_. */
  readonly label = input.required<string>();
  readonly players = input.required<readonly LineupPlayerModel[]>();
  readonly freeSpots = input<readonly (Position | null)[]>([]);
  /** The `spotKey` of the spot being changed. */
  readonly selected = input<string | null>(null);
  readonly spotSelected = output<LineupSpot>();

  protected readonly spots = computed(() => listSpots(this.players(), this.freeSpots()));
  protected readonly freeSpot = FREE_SPOT;
  protected readonly positionTexts: Record<Position, string> = POSITION_TEXTS;
  protected readonly key = spotKey;
}
