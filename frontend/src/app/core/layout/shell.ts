import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { LucideDynamicIcon, LucideHouse, type LucideIcon, LucideUsers } from '@lucide/angular';

import { LastLeague } from '../auth/last-league';
import { SessionStore } from '../auth/session-store';
import { Avatar } from '../../ui/avatar/avatar';
import { IconButton } from '../../ui/button/icon-button';
import { NavBar, NavItem, SideNav } from '../../ui/nav/nav';
import { TopBar } from '../../ui/top-bar/top-bar';
import { Wordmark } from '../../ui/top-bar/wordmark';
import { LeagueSwitcher } from './league-switcher';

interface Destination {
  /** Relative to the league, `''` being its start. */
  readonly path: string;
  readonly label: string;
  readonly icon: LucideIcon;
}

/**
 * The destinations of a league that exist so far (FE-30): the navigation grows with each screen, up
 * to the five of the matchday.
 */
const DESTINATIONS: readonly Destination[] = [
  { path: '', label: $localize`:@@core.shell.nav.matchday:Jornada`, icon: LucideHouse },
  { path: 'members', label: $localize`:@@core.shell.nav.league:Liga`, icon: LucideUsers },
];

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
  protected readonly destinations = computed(() => {
    const leagueId = this.leagueId();
    return leagueId === undefined
      ? []
      : DESTINATIONS.map((destination) => ({
          ...destination,
          link: ['/leagues', leagueId, ...(destination.path === '' ? [] : [destination.path])],
          exact: destination.path === '',
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
