import type { MatchSummaryResponse } from '../../core/api/match/match-api';

/** The closed matches from the most recent matchday: the only ones that can have points. */
export function closedMatches(matches: readonly MatchSummaryResponse[]): MatchSummaryResponse[] {
  return matches
    .filter((match) => match.status === 'CLOSED')
    .sort((a, b) => b.matchday - a.matchday);
}

/** The matchdays of a manager or a player from the most recent one: the API gives them from the oldest. */
export function latestFirst<T extends { matchday: number }>(matchdays: readonly T[]): T[] {
  return [...matchdays].sort((a, b) => b.matchday - a.matchday);
}
