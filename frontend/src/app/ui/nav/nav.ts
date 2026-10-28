import {
  ChangeDetectionStrategy,
  Component,
  inject,
  InjectionToken,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLinkActive } from '@angular/router';

/** Where a `bfNavItem` is: the bottom bar on mobile or the side navigation from `lg`. */
type NavLayout = 'bar' | 'side';

const NAV_LAYOUT = new InjectionToken<NavLayout>('NAV_LAYOUT');

/**
 * The navigation at the bottom of the screen on mobile (FE-18, FE-30): 4 or 5 `bfNavItem` links, only
 * to destinations that exist, under the main action and above the safe area. The shell hides it
 * from `lg`, where the side navigation takes its place.
 */
@Component({
  selector: 'bf-nav-bar',
  template: '<nav [attr.aria-label]="label()"><ng-content /></nav>',
  styleUrl: './nav-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NAV_LAYOUT, useValue: 'bar' }],
})
export class NavBar {
  readonly label = input.required<string>();
}

/**
 * The side navigation from `lg` (FE-18, FE-29): `[bfSideNavHeader]` (the logotype and the league
 * switcher), the `bfNavItem` links and `[bfSideNavFooter]` (the profile), at the full height.
 */
@Component({
  selector: 'bf-side-nav',
  template: `
    <div class="header"><ng-content select="[bfSideNavHeader]" /></div>
    <nav [attr.aria-label]="label()"><ng-content /></nav>
    <div class="footer"><ng-content select="[bfSideNavFooter]" /></div>
  `,
  styleUrl: './side-nav.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NAV_LAYOUT, useValue: 'side' }],
})
export class SideNav {
  readonly label = input.required<string>();
}

/**
 * A destination of `bf-nav-bar` or `bf-side-nav`: a `routerLink` with its icon (at the default 24 px)
 * and its text. It is active, with `aria-current="page"`, while its route is; pass
 * `[routerLinkActiveOptions]="{ exact: true }"` to the one whose route contains the others'.
 */
@Component({
  selector: 'a[bfNavItem]',
  template: '<ng-content select="svg" /><span class="text"><ng-content /></span>',
  styleUrl: './nav-item.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [{ directive: RouterLinkActive, inputs: ['routerLinkActiveOptions'] }],
  host: {
    '[class]': 'layout',
    '[class.active]': 'active()',
    '[attr.aria-current]': 'active() ? "page" : null',
  },
})
export class NavItem {
  protected readonly layout = inject(NAV_LAYOUT);
  protected readonly active = signal(false);

  constructor() {
    inject(RouterLinkActive)
      .isActiveChange.pipe(takeUntilDestroyed())
      .subscribe((active) => {
        this.active.set(active);
      });
  }
}
