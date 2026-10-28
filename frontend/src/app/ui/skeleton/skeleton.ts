import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** A line of text, a title, an avatar or a block such as a list row. */
export type SkeletonShape = 'line' | 'title' | 'avatar' | 'block';

/**
 * The shape of what is loading (FE-14), still and hidden from screen readers: the page marks the
 * loading region with `aria-busy`. It does not shimmer, because nothing moves unless the user acts
 * (ui/README.md). `width` takes any CSS length.
 */
@Component({
  selector: 'bf-skeleton',
  template: '',
  styleUrl: './skeleton.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[class]': 'shape()', '[style.width]': 'width()' },
})
export class Skeleton {
  readonly shape = input<SkeletonShape>('line');
  readonly width = input<string | null>(null);
}
