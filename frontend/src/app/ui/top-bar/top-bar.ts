import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The bar at the top of the screen (FE-18, FE-30): what goes at the start (the logotype, or the
 * current league with its switcher) and, with `[bfTopBarEnd]`, the actions at the end (the profile).
 * It stays on top when scrolling and leaves room for the notch.
 */
@Component({
  selector: 'bf-top-bar',
  template: `
    <header class="bar">
      <div class="start"><ng-content /></div>
      <ng-content select="[bfTopBarEnd]" />
    </header>
  `,
  styleUrl: './top-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopBar {}
