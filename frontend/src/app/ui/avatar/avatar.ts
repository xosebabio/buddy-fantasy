import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

/** 32 px in bars and lists, 40 px in headers, 64 px on the profile. */
export type AvatarSize = 'small' | 'medium' | 'large';

/**
 * A person's picture or, without one or if it does not load, their initials (FE-18). It is
 * decorative: the name goes next to it or in the label of what contains it.
 */
@Component({
  selector: 'bf-avatar',
  template: `
    @if (picture(); as picture) {
      <img [src]="picture" alt="" referrerpolicy="no-referrer" (error)="failed.set(true)" />
    } @else {
      {{ initials() }}
    }
  `,
  styleUrl: './avatar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[class]': 'size()' },
})
export class Avatar {
  readonly name = input.required<string>();
  readonly src = input<string | null>(null);
  readonly size = input<AvatarSize>('small');

  /** Whether the picture failed to load; a new `src` tries again. */
  protected readonly failed = linkedSignal({ source: this.src, computation: () => false });
  protected readonly picture = computed(() => (this.failed() ? null : this.src()));
  protected readonly initials = computed(() => initials(this.name()));
}

const graphemes = new Intl.Segmenter('es', { granularity: 'grapheme' });

/** The first letter of the first and the last word, in capitals: «Xosé Babío» is «XB». */
export function initials(name: string): string {
  const words = name
    .trim()
    .split(/\s+/u)
    .filter((word) => word.length > 0);
  const first = words.at(0);
  const last = words.length > 1 ? words.at(-1) : undefined;
  return [first, last].map(firstLetter).join('').toLocaleUpperCase('es');
}

function firstLetter(word: string | undefined): string {
  for (const { segment } of graphemes.segment(word ?? '')) {
    return segment;
  }
  return '';
}
