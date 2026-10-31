import { CdkAccordionItem } from '@angular/cdk/accordion';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideChevronDown } from '@lucide/angular';

import { ListItem, ListItemSize } from '../list/list';

/**
 * A row of a `bfList` that opens to show its detail below (FE-18), such as the lineup behind a
 * manager's points. It is an `<li>` whose row is a `bfListItem` button with `aria-expanded` and a
 * chevron; it takes the same slots (`[bfListItemLeading]`, `[bfListItemMeta]`,
 * `[bfListItemTrailing]`) and the detail goes in `[bfDisclosurePanel]`, with the row's indent.
 *
 * It is the CDK's `CdkAccordionItem`: `[(expanded)]` and `disabled`, and inside a `cdkAccordion`
 * only one row is open at a time unless the accordion is `multi`.
 */
@Component({
  selector: 'li[bfDisclosure]',
  imports: [ListItem, LucideChevronDown],
  template: `
    <button
      bfListItem
      type="button"
      class="row"
      [size]="size()"
      [attr.aria-expanded]="item.expanded"
      [attr.aria-controls]="panelId"
      [disabled]="item.disabled"
      (click)="item.toggle()"
    >
      <ng-content select="[bfListItemLeading]" ngProjectAs="[bfListItemLeading]" />
      <ng-content select="[bfListItemMeta]" ngProjectAs="[bfListItemMeta]" />
      <ng-content select="[bfListItemTrailing]" ngProjectAs="[bfListItemTrailing]" />
      <svg lucideChevronDown ngProjectAs="[bfListItemTrailing]" class="chevron" [size]="20"></svg>
      <ng-content />
    </button>
    <div class="panel" [id]="panelId" [hidden]="!item.expanded">
      <ng-content select="[bfDisclosurePanel]" />
    </div>
  `,
  styleUrl: './disclosure.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [
    {
      directive: CdkAccordionItem,
      inputs: ['expanded', 'disabled'],
      outputs: ['expandedChange'],
    },
  ],
})
export class Disclosure {
  readonly size = input<ListItemSize>('large');

  protected readonly item = inject(CdkAccordionItem);
  protected readonly panelId = `${this.item.id}-panel`;
}
