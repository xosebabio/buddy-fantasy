import type { Venue } from '../../core/api/match/match-api';

/** Where a match is played (FE-23: a new venue breaks the build). */
export const VENUE_TEXTS: Record<Venue, string> = {
  HOME: $localize`:@@match.venue.home:En casa`,
  AWAY: $localize`:@@match.venue.away:Fuera`,
};
