import { booleanAttribute, ChangeDetectionStrategy, Component, input, model } from '@angular/core';

/**
 * An on/off switch (FE-18), such as the budget or the formation of the lineup settings:
 * `<button bfSwitch aria-label="Presupuesto" [(checked)]="budget"></button>`. It is a button with
 * `role="switch"`, so the page names it (`aria-label` or `aria-labelledby`); Space and Enter toggle
 * it as in any button.
 */
@Component({
  selector: 'button[bfSwitch]',
  template: '',
  styleUrl: './switch.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    type: 'button',
    role: 'switch',
    '[attr.aria-checked]': 'checked()',
    '[disabled]': 'disabled()',
    '(click)': 'checked.set(!checked())',
  },
})
export class Switch {
  readonly checked = model(false);
  readonly disabled = input(false, { transform: booleanAttribute });
}
