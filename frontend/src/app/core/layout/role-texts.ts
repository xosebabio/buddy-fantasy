import type { LeagueRole } from '../api/league/league-api';

/** The name of each role in a league, next to the league or the member. */
export const ROLE_TEXTS: Readonly<Record<LeagueRole, string>> = {
  ADMIN: $localize`:@@core.roles.admin:Admin`,
  MANAGER: $localize`:@@core.roles.manager:Manager`,
};
