import type {
  PlayerRequest,
  PlayerResponse,
  SquadLimitsResponse,
} from '../../../core/api/squad/squad-api';
import type { ErrorCode } from '../../../core/http/error-texts';
import { decimalText, parseDecimal } from '../../../shared/format/decimal';
import type { Position } from '../../../shared/position/position';

/** The controls of the player form that the server can reject, to paint its error under them. */
export type PlayerField = 'name' | 'shirtNumber' | 'initialValue';

/** What the form edits; the shirt number and the value are text, as the user types them. */
export interface PlayerDraft {
  readonly name: string;
  readonly shirtNumber: string;
  readonly position: Position;
  readonly initialValue: string;
}

export const EMPTY_PLAYER: PlayerDraft = {
  name: '',
  shirtNumber: '',
  position: 'GOALKEEPER',
  initialValue: '',
};

export function toPlayerDraft(player: PlayerResponse): PlayerDraft {
  return {
    name: player.name,
    shirtNumber: player.shirtNumber === null ? '' : String(player.shirtNumber),
    position: player.position,
    initialValue: decimalText(player.initialValue),
  };
}

/** A shirt number is optional: empty is none; `undefined` if what is typed is not a whole number. */
function parseShirtNumber(text: string): number | null | undefined {
  const trimmed = text.trim();
  if (trimmed === '') {
    return null;
  }
  return /^\d+$/.test(trimmed) ? Number(trimmed) : undefined;
}

/**
 * The controls whose text cannot be sent: a value that is not a number, and a shirt number that is
 * not a whole number within the limits of `GET /players/limits`. The rest the server validates.
 */
export function invalidPlayerFields(
  draft: PlayerDraft,
  limits: SquadLimitsResponse,
): PlayerField[] {
  const fields: PlayerField[] = [];
  const shirtNumber = parseShirtNumber(draft.shirtNumber);
  if (
    shirtNumber === undefined ||
    (shirtNumber !== null &&
      (shirtNumber < limits.shirtNumberMin || shirtNumber > limits.shirtNumberMax))
  ) {
    fields.push('shirtNumber');
  }
  if (parseDecimal(draft.initialValue) === null) {
    fields.push('initialValue');
  }
  return fields;
}

/** The body of `POST`/`PUT /players`; the draft must pass `invalidPlayerFields`. */
export function toPlayerRequest(draft: PlayerDraft): PlayerRequest {
  return {
    name: draft.name.trim(),
    shirtNumber: parseShirtNumber(draft.shirtNumber) ?? null,
    position: draft.position,
    initialValue: parseDecimal(draft.initialValue) ?? 0,
  };
}

/** Whether two drafts hold the same values. */
export function samePlayer(a: PlayerDraft, b: PlayerDraft): boolean {
  return (
    a.name === b.name &&
    a.shirtNumber === b.shirtNumber &&
    a.position === b.position &&
    a.initialValue === b.initialValue
  );
}

const PLAYER_ERROR_FIELDS: Partial<Record<ErrorCode, PlayerField>> = {
  INVALID_PLAYER_NAME: 'name',
  INVALID_SHIRT_NUMBER: 'shirtNumber',
  SHIRT_NUMBER_TAKEN: 'shirtNumber',
  INVALID_INITIAL_VALUE: 'initialValue',
};

/** The control that a `400` or `409` of the player form is about (decision 11 of plan 13); `null` if none. */
export function playerErrorField(code: ErrorCode): PlayerField | null {
  return PLAYER_ERROR_FIELDS[code] ?? null;
}
