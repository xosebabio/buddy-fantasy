import { booleanAttribute, ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * A button, or a link, with only an icon (FE-18): 44 × 44 px to the touch, and its name in `label`,
 * because the icon is hidden from screen readers.
 */
@Component({
  selector: 'button[bfIconButton], a[bfIconButton]',
  template: '<ng-content />',
  styleUrl: './icon-button.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.aria-label]': 'label()',
    '[attr.disabled]': 'disabled() ? "" : null',
  },
})
export class IconButton {
  readonly label = input.required<string>();
  readonly disabled = input(false, { transform: booleanAttribute });
}
