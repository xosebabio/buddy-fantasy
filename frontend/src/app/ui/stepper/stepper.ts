import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  model,
} from '@angular/core';
import { LucideMinus, LucidePlus } from '@lucide/angular';

/**
 * A whole number with − and + (FE-18), such as a mark from 0 to 10, with the figure large in the
 * middle. `min`, `max` and `step` come from the page; a `null` bound (the default) is no limit, for
 * what has none, such as a score in the points table, which can be negative (shown with the
 * typographic minus, in red). With `empty`, the value can be `null`, shown as that text, and the
 * first step from it goes to `start` (`min`, or 0 without it).
 *
 * The figure is a `spinbutton` named by `label`: the up and down arrows change it by `step`, and
 * Home and End take it to `min` and `max` if they are set. As in the ARIA pattern, the buttons are for touch and the
 * pointer and stay out of the tab order.
 */
@Component({
  selector: 'bf-stepper',
  imports: [LucideMinus, LucidePlus],
  template: `
    <button
      type="button"
      class="step"
      tabindex="-1"
      aria-label="Bajar"
      i18n-aria-label="@@ui.stepper.decrease"
      [disabled]="disabled() || !canDecrease()"
      (click)="stepBy(-1)"
    >
      <svg lucideMinus [size]="20"></svg>
    </button>
    <span
      class="value"
      role="spinbutton"
      [class.blank]="value() === null"
      [class.negative]="isNegative()"
      [attr.tabindex]="disabled() ? -1 : 0"
      [attr.aria-label]="label()"
      [attr.aria-valuenow]="value()"
      [attr.aria-valuemin]="min()"
      [attr.aria-valuemax]="max()"
      [attr.aria-valuetext]="value() === null ? empty() : null"
      [attr.aria-disabled]="disabled() || null"
      (keydown)="onKeydown($event)"
    >
      {{ text() }}
    </span>
    <button
      type="button"
      class="step"
      tabindex="-1"
      aria-label="Subir"
      i18n-aria-label="@@ui.stepper.increase"
      [disabled]="disabled() || !canIncrease()"
      (click)="stepBy(1)"
    >
      <svg lucidePlus [size]="20"></svg>
    </button>
  `,
  styleUrl: './stepper.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.disabled]': 'disabled()' },
})
export class Stepper {
  readonly label = input.required<string>();
  readonly min = input<number | null>(null);
  readonly max = input<number | null>(null);
  readonly step = input(1);
  readonly value = model<number | null>(null);
  /** The text of the empty value, such as _Sin voto_; without it the value is never `null`. */
  readonly empty = input<string | null>(null);
  /** Where the first step from the empty value goes. */
  readonly start = input<number | null>(null);
  readonly disabled = input(false, { transform: booleanAttribute });

  protected readonly canDecrease = computed(() => {
    const value = this.value();
    const min = this.min();
    return value === null || min === null || value > min;
  });
  protected readonly canIncrease = computed(() => {
    const value = this.value();
    const max = this.max();
    return value === null || max === null || value < max;
  });
  protected readonly isNegative = computed(() => (this.value() ?? 0) < 0);
  protected readonly text = computed(() => {
    const value = this.value();
    return value === null ? this.empty() : String(value).replace('-', '−');
  });

  protected stepBy(direction: 1 | -1): void {
    const value = this.value();
    this.set(value === null ? (this.start() ?? this.min() ?? 0) : value + direction * this.step());
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (this.disabled()) {
      return;
    }
    switch (event.key) {
      case 'ArrowUp':
        this.stepBy(1);
        break;
      case 'ArrowDown':
        this.stepBy(-1);
        break;
      case 'Home': {
        const min = this.min();
        if (min === null) {
          return;
        }
        this.set(min);
        break;
      }
      case 'End': {
        const max = this.max();
        if (max === null) {
          return;
        }
        this.set(max);
        break;
      }
      default:
        return;
    }
    event.preventDefault();
  }

  private set(value: number): void {
    this.value.set(Math.min(this.max() ?? value, Math.max(this.min() ?? value, value)));
  }
}
