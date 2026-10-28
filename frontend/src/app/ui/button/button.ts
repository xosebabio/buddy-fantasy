import { booleanAttribute, ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * `primary` is the main action of a screen (one at most, at the bottom); `danger`, what cannot be
 * undone; `quiet`, a secondary action without outline.
 */
export type ButtonVariant = 'primary' | 'secondary' | 'quiet' | 'danger';

/**
 * A button, or a link that looks like one (FE-18). Icons go before the text, at 20 px
 * (`<svg lucidePlus [size]="20"></svg>`). While `loading` it is disabled and shows a spinner, and its
 * name stays the same; `disabled` replaces the native attribute, so that both can apply.
 */
@Component({
  selector: 'button[bfButton], a[bfButton]',
  template: `
    @if (loading()) {
      <span class="spinner" aria-hidden="true"></span>
    }
    <ng-content />
  `,
  styleUrl: './button.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'variant()',
    '[class.block]': 'block()',
    '[attr.disabled]': 'disabled() || loading() ? "" : null',
    '[attr.aria-busy]': 'loading() || null',
  },
})
export class Button {
  readonly variant = input<ButtonVariant>('secondary');
  /** Full width, as the main action at the bottom of a screen. */
  readonly block = input(false, { transform: booleanAttribute });
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly loading = input(false, { transform: booleanAttribute });
}
