import { booleanAttribute, ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface SegmentedOption<T extends string> {
  readonly value: T;
  readonly label: string;
}

let nextId = 0;

/**
 * A choice among a few options shown side by side (FE-18), such as the theme. They are native
 * radio buttons in a group named by `label`, so the arrow keys move between them; `value` is the
 * chosen one.
 */
@Component({
  selector: 'bf-segmented',
  template: `
    <fieldset [disabled]="disabled()">
      <legend class="label">{{ label() }}</legend>
      <div class="options">
        @for (option of options(); track option.value) {
          <label class="option">
            <input
              type="radio"
              [name]="name"
              [value]="option.value"
              [checked]="option.value === value()"
              (change)="value.set(option.value)"
            />
            {{ option.label }}
          </label>
        }
      </div>
    </fieldset>
  `,
  styleUrl: './segmented.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Segmented<T extends string> {
  readonly label = input.required<string>();
  readonly options = input.required<readonly SegmentedOption<T>[]>();
  readonly value = model.required<T>();
  readonly disabled = input(false, { transform: booleanAttribute });

  protected readonly name = `bf-segmented-${String(nextId++)}`;
}
