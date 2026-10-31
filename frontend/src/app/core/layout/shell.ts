import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, PRIMARY_OUTLET, Router, RouterLink, RouterOutlet } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { filter, map } from 'rxjs';

import { LastLeague } from '../auth/last-league';
import { SessionStore } from '../auth/session-store';
import { Avatar } from '../../ui/avatar/avatar';
import { IconButton } from '../../ui/button/icon-button';
import { NavBar, NavItem, SideNav } from '../../ui/nav/nav';
import { TopBar } from '../../ui/top-bar/top-bar';
import { Wordmark } from '../../ui/top-bar/wordmark';
import { DESTINATIONS, destinationOf } from './destinations';
import { LeagueSwitcher } from './league-switcher';

/**
 * The frame of the pages (FE-29, FE-30). Outside a league, the top bar with the logotype and the
 * profile. In a league (`leagueId` from the route), the bar has the league switcher and the
 * navigation goes at the bottom on mobile, and from `lg` in a side column with the logotype, the
 * switcher and the profile.
 */
@Component({
  selector: 'bf-shell',
  imports: [
    Avatar,
    IconButton,
    LeagueSwitcher,
    LucideDynamicIcon,
    NavBar,
    NavItem,
    RouterLink,
    RouterOutlet,
    SideNav,
    TopBar,
    Wordmark,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  /** The league of the route, which `memberGuard` has checked; none outside a league. */
  readonly leagueId = input<string>();

  protected readonly session = inject(SessionStore);
  protected readonly league = computed(() => {
    const leagueId = this.leagueId();
    return leagueId === undefined ? undefined : this.session.league(leagueId);
  });
  private readonly router = inject(Router);
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );
  /** The first segment of the page after the league (`''` at its start). */
  private readonly section = computed(() => {
    const segments =
      this.router
        .parseUrl(this.url())
        .root.children[PRIMARY_OUTLET]?.segments.map((segment) => segment.path) ?? [];
    return segments[2] ?? '';
  });
  protected readonly destinations = computed(() => {
    const leagueId = this.leagueId();
    const active = destinationOf(this.section());
    return leagueId === undefined
      ? []
      : DESTINATIONS.map((destination) => ({
          ...destination,
          link: ['/leagues', leagueId, ...(destination.path === '' ? [] : [destination.path])],
          active: destination === active,
        }));
  });

  constructor() {
    const lastLeague = inject(LastLeague);
    effect(() => {
      const leagueId = this.league()?.leagueId;
      if (leagueId !== undefined) {
        lastLeague.save(leagueId);
      }
    });
  }
}
