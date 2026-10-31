import { ChangeDetectionStrategy, Component, ViewEncapsulation } from '@angular/core';

/**
 * A native checkbox with the look of the app (FE-18): `<label><input type="checkbox" bfCheckbox />
 * Jugó</label>`. The label is the touch target (44 px high) and the box is lavender when checked.
 * It is the native control, so `[checked]`, `(change)`, forms and the keyboard work as usual. Its
 * styles are global (like `bfInput`), because the label around it is not part of the component.
 */
@Component({
  selector: 'input[type=checkbox][bfCheckbox]',
  template: '',
  styleUrl: './checkbox.scss',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'bf-checkbox' },
})
export class Checkbox {}
