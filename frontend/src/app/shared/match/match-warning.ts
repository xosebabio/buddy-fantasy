import type { MatchResponse } from '../../core/api/match/match-api';

export type MatchWarning = NonNullable<MatchResponse['warnings']>[number];

/**
 * The coherence warnings of a sheet in text (FE-23: a new warning breaks the build). They never
 * prevent saving or closing: in a Sunday league a scorer may be missing.
 */
export const MATCH_WARNING_TEXTS: Record<MatchWarning, string> = {
  GOALS_MISMATCH: $localize`:@@shared.match.warning.goals-mismatch:Los goles de los jugadores no suman los del resultado.`,
  OWN_GOALS_EXCEED_GOALS_AGAINST: $localize`:@@shared.match.warning.own-goals:Hay más goles en propia puerta que goles en contra.`,
};
