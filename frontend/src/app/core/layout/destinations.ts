import {
  LucideHouse,
  type LucideIcon,
  LucideShirt,
  LucideStar,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

export interface Destination {
  /** Relative to the league, `''` being its start. */
  readonly path: string;
  readonly label: string;
  readonly icon: LucideIcon;
  /** The first segments, relative to the league, of the routes that the destination covers. */
  readonly prefixes: readonly string[];
}

/**
 * The five destinations of a league (FE-30). Each covers the routes that belong to it, so that
 * the navigation says where the user is in a screen that is not its own link, such as a player's
 * sheet under «Liga».
 */
export const DESTINATIONS: readonly Destination[] = [
  {
    path: '',
    label: $localize`:@@core.shell.nav.matchday:Jornada`,
    icon: LucideHouse,
    prefixes: [''],
  },
  {
    path: 'lineup',
    label: $localize`:@@core.shell.nav.lineup:Alineación`,
    icon: LucideShirt,
    prefixes: ['lineup', 'lineups'],
  },
  {
    path: 'ratings',
    label: $localize`:@@core.shell.nav.ratings:Valorar`,
    icon: LucideStar,
    prefixes: ['ratings'],
  },
  {
    path: 'standings',
    label: $localize`:@@core.shell.nav.standings:Clasificación`,
    icon: LucideTrophy,
    prefixes: ['standings', 'managers'],
  },
  {
    path: 'league',
    label: $localize`:@@core.shell.nav.league:Liga`,
    icon: LucideUsers,
    prefixes: ['league', 'squad', 'matches', 'rules', 'members', 'admin'],
  },
];

/**
 * The destination that covers the page of a league whose route starts with `section` (`''` for the
 * league's start), if any.
 */
export function destinationOf(section: string): Destination | undefined {
  return DESTINATIONS.find((destination) => destination.prefixes.includes(section));
}
