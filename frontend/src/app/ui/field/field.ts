import {
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChild,
  Directive,
  inject,
  input,
  ViewEncapsulation,
} from '@angular/core';
import { LucideChevronDown, LucideCircleAlert } from '@lucide/angular';

let nextId = 0;

/**
 * A form field (FE-18): its label, the control (`<input bfInput>`, `<textarea bfInput>` or
 * `<select bfSelect>`), a hint and the error. The field gives the control its id and describes it with the hint and the error,
 * which the page sets from the server's answer (FE-15): the client does not validate limits.
 *
 * Its styles are global (`bf-field` and `.bf-input`), because the control is projected content.
 */
@Component({
  selector: 'bf-field',
  imports: [LucideChevronDown, LucideCircleAlert],
  template: `
    <label class="bf-field-label" [for]="controlId">{{ label() }}</label>
    <div class="bf-field-control">
      <ng-content />
      @if (select()) {
        <svg lucideChevronDown class="bf-field-chevron" [size]="20"></svg>
      }
    </div>
    @if (hint(); as hint) {
      <p class="bf-field-hint" [id]="hintId">{{ hint }}</p>
    }
    @if (error(); as error) {
      <p class="bf-field-error" [id]="errorId">
        <svg lucideCircleAlert [size]="16"></svg>
        {{ error }}
      </p>
    }
  `,
  styleUrl: './field.scss',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Field {
  readonly label = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly error = input<string | null>(null);

  readonly controlId = `bf-field-${String(nextId++)}`;
  protected readonly hintId = `${this.controlId}-hint`;
  protected readonly errorId = `${this.controlId}-error`;
  /** A native select draws no arrow of its own (`appearance: none`): the field draws it. */
  protected readonly select = contentChild(FieldSelect);

  /** The ids of the hint and the error that are shown, for `aria-describedby`. */
  readonly describedBy = computed(
    () =>
      [this.hint() ? this.hintId : null, this.error() ? this.errorId : null]
        .filter((id) => id !== null)
        .join(' ') || null,
  );
}

/** The control of a `bf-field`; use `type`, `inputmode` and `autocomplete` as they apply (FE-30). */
@Directive({
  selector: 'input[bfInput], textarea[bfInput]',
  host: {
    class: 'bf-input',
    '[id]': 'field.controlId',
    '[attr.aria-describedby]': 'field.describedBy()',
    '[attr.aria-invalid]': 'field.error() ? true : null',
  },
})
export class FieldInput {
  protected readonly field = inject(Field);
}

/** A native `<select>` in a `bf-field`, with the look of `bfInput` and a chevron. */
@Directive({
  selector: 'select[bfSelect]',
  host: {
    class: 'bf-input bf-select',
    '[id]': 'field.controlId',
    '[attr.aria-describedby]': 'field.describedBy()',
    '[attr.aria-invalid]': 'field.error() ? true : null',
  },
})
export class FieldSelect {
  protected readonly field = inject(Field);
}
