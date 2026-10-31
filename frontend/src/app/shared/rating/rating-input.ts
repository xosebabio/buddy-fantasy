import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  model,
} from '@angular/core';

import type { RatingFormat } from '../../core/api/rating/rating-api';
import { Stepper } from '../../ui/stepper/stepper';

/**
 * The values of each format are fixed by the contract (`PlayerRatingRequest`), not by the league,
 * so they are presentation constants. The first step of an empty mark goes to the middle.
 */
const SPADES = [0, 1, 2, 3, 4] as const;
const DECIMAL_MAX = 10;
const DECIMAL_START = 5;

const NO_VOTE = $localize`:@@shared.ratingInput.noVote:Sin voto`;

let nextId = 0;

/**
 * A user's vote for a player (FE-19), in the format of the rating window, which the page takes from
 * `MatchRatingsResponse` (decision 6 of plan 12): `SPADES`, five native radio buttons from 0 to 4 in
 * a row; `DECIMAL`, a `bf-stepper` from 0 to 10 whose first step goes to 5. It starts without a vote
 * (`null`), and _Quitar_ takes the vote back to it: a 0 is a vote, never the same as not voting.
 */
@Component({
  selector: 'bf-rating-input',
  imports: [Stepper],
  template: `
    @if (format() === 'SPADES') {
      <div class="spades" role="radiogroup" [attr.aria-label]="label()">
        @for (option of spades; track option) {
          <label class="spade">
            <input
              type="radio"
              [name]="name"
              [value]="option"
              [checked]="value() === option"
              [disabled]="disabled()"
              (change)="value.set(option)"
            />
            {{ option }}
          </label>
        }
      </div>
    } @else {
      <bf-stepper
        [label]="label()"
        [min]="0"
        [max]="decimalMax"
        [start]="decimalStart"
        [empty]="noVote"
        [disabled]="disabled()"
        [(value)]="value"
      />
    }
    @if (value() !== null) {
      <button
        type="button"
        class="clear"
        [attr.aria-label]="clearLabel()"
        [disabled]="disabled()"
        (click)="clear()"
        i18n="@@shared.ratingInput.clear"
      >
        Quitar
      </button>
    } @else if (format() === 'SPADES') {
      <span class="none">{{ noVote }}</span>
    }
  `,
  styleUrl: './rating-input.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingInput {
  readonly format = input.required<RatingFormat>();
  /** Who is rated, for the accessible names: _Picas para Diego_, _Quitar voto a Diego_. */
  readonly playerName = input.required<string>();
  readonly value = model<number | null>(null);
  readonly disabled = input(false, { transform: booleanAttribute });

  protected readonly spades = SPADES;
  protected readonly decimalMax = DECIMAL_MAX;
  protected readonly decimalStart = DECIMAL_START;
  protected readonly noVote = NO_VOTE;
  protected readonly name = `bf-rating-input-${String(nextId++)}`;

  protected readonly label = computed(() => {
    const player = this.playerName();
    return this.format() === 'SPADES'
      ? $localize`:@@shared.ratingInput.spadesLabel:Picas para ${player}:player:`
      : $localize`:@@shared.ratingInput.decimalLabel:Nota de ${player}:player:`;
  });
  protected readonly clearLabel = computed(
    () => $localize`:@@shared.ratingInput.clearLabel:Quitar voto a ${this.playerName()}:player:`,
  );

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  /** Back to no vote, with the focus on the control, since the button goes away. */
  protected clear(): void {
    this.value.set(null);
    this.host.querySelector<HTMLElement>('input, [role="spinbutton"]')?.focus();
  }
}
