import type { MatchSummaryResponse } from '../../core/api/match/match-api';

/**
 * Where a match is for the admin: `SCHEDULED` until it starts, `PENDING` once it has started and
 * the sheet is not closed (the admin owes the sheet), and `CLOSED`.
 */
export type MatchPhase = 'SCHEDULED' | 'PENDING' | 'CLOSED';

/** The phase of a match at `now`. It uses the browser's clock, so it is a guide (FE-09): the server decides. */
export function matchPhase(
  match: Pick<MatchSummaryResponse, 'status' | 'kickoffAt'>,
  now: Date,
): MatchPhase {
  if (match.status === 'CLOSED') {
    return 'CLOSED';
  }
  return new Date(match.kickoffAt).getTime() <= now.getTime() ? 'PENDING' : 'SCHEDULED';
}

/** The phases in text (FE-23: a new phase breaks the build). */
export const MATCH_PHASE_TEXTS: Record<MatchPhase, string> = {
  SCHEDULED: $localize`:@@shared.match.phase.scheduled:Programado`,
  PENDING: $localize`:@@shared.match.phase.pending:Acta pendiente`,
  CLOSED: $localize`:@@shared.match.phase.closed:Cerrado`,
};
