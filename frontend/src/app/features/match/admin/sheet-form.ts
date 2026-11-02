import type {
  MatchResponse,
  MatchSheetRequest,
  PlayerPerformanceRequest,
  PlayerPerformanceResponse,
} from '../../../core/api/match/match-api';
import type { PlayerResponse } from '../../../core/api/squad/squad-api';
import type { ErrorCode } from '../../../core/http/error-texts';
import { POSITIONS, type Position } from '../../../shared/position/position';

/** The controls of the sheet that the server can reject, to paint its error under them. */
export type SheetField = 'result';

/** The counters of a player in the sheet. */
export type StatKey =
  'goals' | 'penaltyGoals' | 'assists' | 'ownGoals' | 'penaltiesSaved' | 'penaltiesMissed';

export const STAT_KEYS: readonly StatKey[] = [
  'goals',
  'penaltyGoals',
  'assists',
  'ownGoals',
  'penaltiesSaved',
  'penaltiesMissed',
];

export type CardKey = 'yellowCard' | 'doubleYellow' | 'redCard';

/** What the sheet records of a player who played. */
export interface PerformanceDraft {
  readonly goals: number;
  readonly penaltyGoals: number;
  readonly assists: number;
  readonly ownGoals: number;
  readonly penaltiesSaved: number;
  readonly penaltiesMissed: number;
  readonly yellowCard: boolean;
  readonly doubleYellow: boolean;
  readonly redCard: boolean;
}

/**
 * What the sheet edits: the score, which is empty until it is typed, and who played with what they
 * did, by player id. A player who is not a key did not play.
 */
export interface SheetDraft {
  readonly goalsFor: number | null;
  readonly goalsAgainst: number | null;
  readonly players: Readonly<Record<string, PerformanceDraft>>;
}

export const EMPTY_PERFORMANCE: PerformanceDraft = {
  goals: 0,
  penaltyGoals: 0,
  assists: 0,
  ownGoals: 0,
  penaltiesSaved: 0,
  penaltiesMissed: 0,
  yellowCard: false,
  doubleYellow: false,
  redCard: false,
};

export const EMPTY_SHEET: SheetDraft = { goalsFor: null, goalsAgainst: null, players: {} };

function toPerformanceDraft(performance: PlayerPerformanceResponse): PerformanceDraft {
  return {
    goals: performance.goals,
    penaltyGoals: performance.penaltyGoals,
    assists: performance.assists,
    ownGoals: performance.ownGoals,
    penaltiesSaved: performance.penaltiesSaved,
    penaltiesMissed: performance.penaltiesMissed,
    yellowCard: performance.yellowCard,
    doubleYellow: performance.doubleYellow,
    redCard: performance.redCard,
  };
}

/** The sheet that the server has for a match; an empty one if it has none yet. */
export function toSheetDraft(match: Pick<MatchResponse, 'result' | 'performances'>): SheetDraft {
  return {
    goalsFor: match.result?.goalsFor ?? null,
    goalsAgainst: match.result?.goalsAgainst ?? null,
    players: Object.fromEntries(
      (match.performances ?? []).map((performance) => [
        performance.playerId,
        toPerformanceDraft(performance),
      ]),
    ),
  };
}

/** The ids of who played, in the order of the sheet. */
export function playerIds(draft: SheetDraft): string[] {
  return Object.keys(draft.players);
}

/**
 * The score is sent when both marks have a value; with one alone the other one is missing, which
 * the contract does not accept (`MatchResultRequest` asks for both), so the form marks it.
 */
export function missingMark(draft: SheetDraft): 'goalsFor' | 'goalsAgainst' | null {
  if (draft.goalsFor === null && draft.goalsAgainst !== null) {
    return 'goalsFor';
  }
  if (draft.goalsAgainst === null && draft.goalsFor !== null) {
    return 'goalsAgainst';
  }
  return null;
}

function toPerformanceRequest(playerId: string, draft: PerformanceDraft): PlayerPerformanceRequest {
  return { playerId, ...draft };
}

/** The body of `PUT …/sheet`; the draft must not be missing a mark. */
export function toSheetRequest(draft: SheetDraft): MatchSheetRequest {
  return {
    result:
      draft.goalsFor === null || draft.goalsAgainst === null
        ? null
        : { goalsFor: draft.goalsFor, goalsAgainst: draft.goalsAgainst },
    performances: Object.entries(draft.players).map(([playerId, performance]) =>
      toPerformanceRequest(playerId, performance),
    ),
  };
}

function samePerformance(a: PerformanceDraft, b: PerformanceDraft): boolean {
  return (
    STAT_KEYS.every((key) => a[key] === b[key]) &&
    a.yellowCard === b.yellowCard &&
    a.doubleYellow === b.doubleYellow &&
    a.redCard === b.redCard
  );
}

/** Whether two drafts hold the same score and the same players with the same stats. */
export function sameSheet(a: SheetDraft, b: SheetDraft): boolean {
  const ids = playerIds(a);
  return (
    a.goalsFor === b.goalsFor &&
    a.goalsAgainst === b.goalsAgainst &&
    ids.length === playerIds(b).length &&
    ids.every((id) => {
      const other = b.players[id];
      return other !== undefined && samePerformance(a.players[id] ?? EMPTY_PERFORMANCE, other);
    })
  );
}

/** Marks a player as having played, with nothing recorded, or takes them out of the sheet. */
export function setPlayed(draft: SheetDraft, playerId: string, played: boolean): SheetDraft {
  if (played === playerId in draft.players) {
    return draft;
  }
  if (played) {
    return { ...draft, players: { ...draft.players, [playerId]: EMPTY_PERFORMANCE } };
  }
  return {
    ...draft,
    players: Object.fromEntries(Object.entries(draft.players).filter(([id]) => id !== playerId)),
  };
}

/** Changes a counter of a player who played. */
export function setStat(
  draft: SheetDraft,
  playerId: string,
  key: StatKey,
  value: number,
): SheetDraft {
  const performance = draft.players[playerId];
  return performance === undefined
    ? draft
    : { ...draft, players: { ...draft.players, [playerId]: { ...performance, [key]: value } } };
}

/**
 * Checks or unchecks a card of a player who played. A double yellow excludes the other two (decision
 * 9 of plan 13, the shape of the contract): checking it clears them. A yellow and a straight red can
 * go together.
 */
export function setCard(
  draft: SheetDraft,
  playerId: string,
  card: CardKey,
  checked: boolean,
): SheetDraft {
  const performance = draft.players[playerId];
  if (performance === undefined) {
    return draft;
  }
  const next: PerformanceDraft =
    checked && card === 'doubleYellow'
      ? { ...performance, yellowCard: false, redCard: false, doubleYellow: true }
      : { ...performance, [card]: checked };
  return { ...draft, players: { ...draft.players, [playerId]: next } };
}

/** Whether a card cannot be checked because the player has a double yellow. */
export function cardDisabled(performance: PerformanceDraft, card: CardKey): boolean {
  return card !== 'doubleYellow' && performance.doubleYellow;
}

/** The ids that `saved` has and `draft` does not: who a correction takes out of the sheet. */
export function removedPlayerIds(saved: SheetDraft, draft: SheetDraft): string[] {
  return playerIds(saved).filter((id) => !(id in draft.players));
}

/** The players of one position that the sheet lists. */
export interface SheetGroup {
  readonly position: Position;
  readonly players: PlayerResponse[];
}

/**
 * Who the sheet lists, by position from the own goal forwards and each group by name: the active
 * players and the retired ones that are in the saved sheet, which stay there (decision 5 of plan 13).
 * A retired player who is taken out cannot come back, so they are listed only while they are in
 * `saved`.
 */
export function sheetGroups(players: readonly PlayerResponse[], saved: SheetDraft): SheetGroup[] {
  const listed = players.filter(
    (player) => player.retiredAt === null || player.id in saved.players,
  );
  return POSITIONS.map((position) => ({
    position,
    players: listed
      .filter((player) => player.position === position)
      .sort((a, b) => a.name.localeCompare(b.name, 'es')),
  })).filter((group) => group.players.length > 0);
}

const SHEET_ERROR_FIELDS: Partial<Record<ErrorCode, SheetField>> = {
  INVALID_GOALS: 'result',
};

/**
 * The control that a `400` of the sheet is about (decision 11 of plan 13); `null` if none, which
 * goes in the notice because the server does not say which player it is about.
 */
export function sheetErrorField(code: ErrorCode): SheetField | null {
  return SHEET_ERROR_FIELDS[code] ?? null;
}
