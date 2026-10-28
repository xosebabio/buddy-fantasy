import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
} from '@angular/core';
import { LucideChevronRight } from '@lucide/angular';

/**
 * A list of full-width rows with dividers (FE-18), the way lists look in the app (ui/README.md).
 * `bleed` cancels the side margin of the page, so that the dividers reach the edges.
 */
@Component({
  selector: 'ul[bfList], ol[bfList]',
  template: '<ng-content />',
  styleUrl: './list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.bleed]': 'bleed()' },
})
export class List {
  readonly bleed = input(false, { transform: booleanAttribute });
}

/** `large` for what the row names (a league), `compact` for people and players. */
export type ListItemSize = 'large' | 'compact';

/**
 * A row of a `bfList`: an `<li>`, or a link or button inside one when the whole row acts. Its
 * content is the title; `[bfListItemLeading]` (an avatar), `[bfListItemMeta]` (the line below) and
 * `[bfListItemTrailing]` (an action) are optional. A link row ends with a chevron. `current` marks
 * the row of what is in use, such as the current league.
 */
@Component({
  selector: 'li[bfListItem], a[bfListItem], button[bfListItem]',
  imports: [LucideChevronRight],
  template: `
    <ng-content select="[bfListItemLeading]" />
    <span class="body">
      <span class="title"><ng-content /></span>
      <span class="meta"><ng-content select="[bfListItemMeta]" /></span>
    </span>
    <ng-content select="[bfListItemTrailing]" />
    @if (link) {
      <svg lucideChevronRight class="chevron" [size]="20"></svg>
    }
  `,
  styleUrl: './list-item.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'size()',
    '[class.current]': 'current()',
    '[attr.aria-current]': 'current() || null',
  },
})
export class ListItem {
  readonly size = input<ListItemSize>('large');
  readonly current = input(false, { transform: booleanAttribute });

  protected readonly link =
    inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.tagName === 'A';
}
