import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * What a screen shows when there is nothing yet (FE-14): the outline of an empty bib, a message
 * that invites to fill it and, optionally, the action that does (`[bfEmptyStateAction]`).
 */
@Component({
  selector: 'bf-empty-state',
  template: `
    <svg class="bib" viewBox="0 0 44 50" aria-hidden="true">
      <path d="M1 5.4 13.2 1 22 8.2 30.8 1 43 5.4V49H1z" />
    </svg>
    <div class="message"><ng-content /></div>
    <ng-content select="[bfEmptyStateAction]" />
  `,
  styleUrl: './empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {}
