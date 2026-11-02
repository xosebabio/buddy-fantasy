import type {
  MatchLimitsResponse,
  MatchRequest,
  MatchSummaryResponse,
  Venue,
} from '../../../core/api/match/match-api';
import type { ErrorCode } from '../../../core/http/error-texts';
import { instantToMadrid, madridToInstant } from '../../../core/time/madrid-time';

/** The controls of the match form that the server can reject, to paint its error under them. */
export type MatchField = 'matchday' | 'opponent' | 'date' | 'time';

/** What the form edits; the matchday is text, and the day and the time are those of Madrid. */
export interface MatchDraft {
  readonly matchday: string;
  readonly opponent: string;
  readonly venue: Venue;
  readonly date: string;
  readonly time: string;
}

export const EMPTY_MATCH: MatchDraft = {
  matchday: '',
  opponent: '',
  venue: 'HOME',
  date: '',
  time: '',
};

export function toMatchDraft(match: MatchSummaryResponse): MatchDraft {
  return {
    matchday: String(match.matchday),
    opponent: match.opponent,
    venue: match.venue,
    ...instantToMadrid(match.kickoffAt),
  };
}

const WEEK_IN_DAYS = 7;

/**
 * The draft to schedule a match, as a suggestion (they are data, not rules): the matchday after the
 * latest of the calendar and, if there are matches, the time of the latest one a week later.
 */
export function proposeMatch(matches: readonly MatchSummaryResponse[]): MatchDraft {
  const last = matches.reduce<MatchSummaryResponse | undefined>(
    (latest, match) => (latest === undefined || match.matchday > latest.matchday ? match : latest),
    undefined,
  );
  if (last === undefined) {
    return EMPTY_MATCH;
  }
  const { date, time } = instantToMadrid(
    matches.reduce(
      (latest, match) => (match.kickoffAt > latest ? match.kickoffAt : latest),
      last.kickoffAt,
    ),
  );
  // The day is added in UTC, which has no clock changes; the time of day stays the one in Madrid.
  const next = new Date(`${date}T00:00:00Z`);
  next.setUTCDate(next.getUTCDate() + WEEK_IN_DAYS);
  return {
    ...EMPTY_MATCH,
    matchday: String(last.matchday + 1),
    date: next.toISOString().slice(0, 10),
    time,
  };
}

/** A matchday is a whole number; `undefined` if what is typed is not one. */
function parseMatchday(text: string): number | undefined {
  const trimmed = text.trim();
  return /^\d+$/.test(trimmed) ? Number(trimmed) : undefined;
}

/**
 * The controls whose text cannot be sent: a matchday that is not a whole number within the limit of
 * `GET /matches/limits`, and a day or a time left empty. The rest the server validates.
 */
export function invalidMatchFields(draft: MatchDraft, limits: MatchLimitsResponse): MatchField[] {
  const fields: MatchField[] = [];
  const matchday = parseMatchday(draft.matchday);
  if (matchday === undefined || matchday < limits.matchdayMin) {
    fields.push('matchday');
  }
  if (draft.date === '') {
    fields.push('date');
  }
  if (draft.time === '') {
    fields.push('time');
  }
  return fields;
}

/** The body of `POST`/`PUT /matches`; the draft must pass `invalidMatchFields`. */
export function toMatchRequest(draft: MatchDraft): MatchRequest {
  return {
    matchday: parseMatchday(draft.matchday) ?? 0,
    opponent: draft.opponent.trim(),
    venue: draft.venue,
    kickoffAt: madridToInstant(draft.date, draft.time),
  };
}

/** Whether two drafts hold the same values. */
export function sameMatch(a: MatchDraft, b: MatchDraft): boolean {
  return (
    a.matchday === b.matchday &&
    a.opponent === b.opponent &&
    a.venue === b.venue &&
    a.date === b.date &&
    a.time === b.time
  );
}

/** Whether going from `saved` to `draft` postpones the match: its matchday or its time change. */
export function postpones(draft: MatchDraft, saved: MatchDraft): boolean {
  return (
    draft.matchday !== saved.matchday || draft.date !== saved.date || draft.time !== saved.time
  );
}

const MATCH_ERROR_FIELDS: Partial<Record<ErrorCode, MatchField>> = {
  INVALID_MATCHDAY: 'matchday',
  MATCHDAY_TAKEN: 'matchday',
  INVALID_OPPONENT: 'opponent',
  KICKOFF_TAKEN: 'time',
};

/** The control that a `400` or `409` of the match form is about (decision 11 of plan 13); `null` if none. */
export function matchErrorField(code: ErrorCode): MatchField | null {
  return MATCH_ERROR_FIELDS[code] ?? null;
}
