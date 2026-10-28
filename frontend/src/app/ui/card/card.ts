import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * A surface with border for a block that stands apart, such as the preview of an invitation
 * (FE-18). Lists are not cards: they go in `bfList` rows (ui/README.md).
 */
@Component({
  selector: 'bf-card',
  template: '<ng-content />',
  styleUrl: './card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Card {}
