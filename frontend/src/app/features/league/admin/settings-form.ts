import type { LeagueSettingsBody } from '../../../core/api/league/league-api';
import type { ErrorCode } from '../../../core/http/error-texts';
import { decimalText, parseDecimal } from '../../../shared/format/decimal';
import { POSITIONS } from '../../../shared/position/position';
import type { ScoringAction } from '../../../shared/scoring/scoring-action';
import { type ScoringRow, scoringRows } from '../rules';

export type RatingFormat = LeagueSettingsBody['ratingFormat'];

/** The controls of the form that the server can reject, to paint its error under them. */
export type SettingsField =
  | 'lineupSize'
  | 'lineupLockHours'
  | 'lineupMaxChanges'
  | 'lineupBudget'
  | 'lineupFormation'
  | 'ratingFormat'
  | 'ratingWindowHours'
  | 'ratingMinVotes'
  | 'defaultRating'
  | 'ratingConversion';

/** The settings that are a whole number in a stepper. */
export type SettingsNumberKey =
  | 'lineupSize'
  | 'lineupLockHours'
  | 'lineupMaxChanges'
  | 'minGoalkeepers'
  | 'minDefenders'
  | 'minMidfielders'
  | 'minForwards'
  | 'ratingWindowHours'
  | 'ratingMinVotes';

/** The settings that are typed as a decimal, which the draft keeps as the user wrote them. */
export type SettingsDecimalKey =
  'budget' | 'spadesFactor' | 'spadesBase' | 'decimalFactor' | 'decimalBase';

/**
 * What the form edits (decision 3 of plan 13: the market parameters are not in it). The decimals are
 * text, so that «4,» or «−» can be typed on the way to a number; the optional ones are a switch and
 * their value, so that the value that was there is kept while the switch is off.
 */
export interface SettingsDraft extends Record<SettingsNumberKey, number> {
  readonly budgetEnabled: boolean;
  readonly budget: string;
  readonly formationEnabled: boolean;
  readonly ratingFormat: RatingFormat;
  /** The mark of someone below the minimum of votes; `null` for none. */
  readonly defaultRating: number | null;
  readonly spadesFactor: string;
  readonly spadesBase: string;
  readonly decimalFactor: string;
  readonly decimalBase: string;
  readonly scoring: readonly ScoringRow[];
}

export function toSettingsDraft(settings: LeagueSettingsBody): SettingsDraft {
  return {
    lineupSize: settings.lineupSize,
    lineupLockHours: settings.lineupLockHours,
    lineupMaxChanges: settings.lineupMaxChanges,
    budgetEnabled: settings.lineupBudgetEnabled,
    budget: decimalText(settings.lineupBudget),
    formationEnabled: settings.lineupFormationEnabled,
    minGoalkeepers: settings.lineupMinGoalkeepers,
    minDefenders: settings.lineupMinDefenders,
    minMidfielders: settings.lineupMinMidfielders,
    minForwards: settings.lineupMinForwards,
    ratingFormat: settings.ratingFormat,
    ratingWindowHours: settings.ratingWindowHours,
    ratingMinVotes: settings.ratingMinVotes,
    defaultRating: settings.defaultRating,
    spadesFactor: decimalText(settings.ratingSpadesFactor),
    spadesBase: decimalText(settings.ratingSpadesBase),
    decimalFactor: decimalText(settings.ratingDecimalFactor),
    decimalBase: decimalText(settings.ratingDecimalBase),
    scoring: scoringRows(settings.scoringRules),
  };
}

/** The decimals that are not a number and so cannot be sent: the budget and the conversion in use. */
export function unparsableDecimals(draft: SettingsDraft): SettingsField[] {
  const spades = draft.ratingFormat === 'SPADES';
  const conversion = spades
    ? [draft.spadesFactor, draft.spadesBase]
    : [draft.decimalFactor, draft.decimalBase];
  const fields: SettingsField[] = [];
  if (parseDecimal(draft.budget) === null) {
    fields.push('lineupBudget');
  }
  if (conversion.some((text) => parseDecimal(text) === null)) {
    fields.push('ratingConversion');
  }
  return fields;
}

/**
 * The body of `PUT /settings`, which replaces everything: what the form does not edit (the market)
 * goes back as it came in `base`, and so does what is switched off. The decimals must parse
 * (`unparsableDecimals`); one that does not goes back as it was.
 */
export function toSettingsBody(draft: SettingsDraft, base: LeagueSettingsBody): LeagueSettingsBody {
  return {
    ...base,
    lineupSize: draft.lineupSize,
    lineupLockHours: draft.lineupLockHours,
    lineupMaxChanges: draft.lineupMaxChanges,
    lineupBudgetEnabled: draft.budgetEnabled,
    lineupBudget: parseDecimal(draft.budget) ?? base.lineupBudget,
    lineupFormationEnabled: draft.formationEnabled,
    lineupMinGoalkeepers: draft.minGoalkeepers,
    lineupMinDefenders: draft.minDefenders,
    lineupMinMidfielders: draft.minMidfielders,
    lineupMinForwards: draft.minForwards,
    ratingFormat: draft.ratingFormat,
    ratingWindowHours: draft.ratingWindowHours,
    ratingMinVotes: draft.ratingMinVotes,
    defaultRating: draft.defaultRating,
    ratingSpadesFactor: parseDecimal(draft.spadesFactor) ?? base.ratingSpadesFactor,
    ratingSpadesBase: parseDecimal(draft.spadesBase) ?? base.ratingSpadesBase,
    ratingDecimalFactor: parseDecimal(draft.decimalFactor) ?? base.ratingDecimalFactor,
    ratingDecimalBase: parseDecimal(draft.decimalBase) ?? base.ratingDecimalBase,
    scoringRules: Object.fromEntries(
      draft.scoring.map((row) => [
        row.action,
        Object.fromEntries(POSITIONS.map((position, index) => [position, row.points[index] ?? 0])),
      ]),
    ),
  };
}

/** The draft with one score of the points table changed. */
export function withPoints(
  draft: SettingsDraft,
  action: ScoringAction,
  positionIndex: number,
  points: number,
): SettingsDraft {
  return {
    ...draft,
    scoring: draft.scoring.map((row) =>
      row.action === action
        ? { ...row, points: row.points.map((p, index) => (index === positionIndex ? points : p)) }
        : row,
    ),
  };
}

/** Whether two drafts hold the same values. */
export function sameDraft(a: SettingsDraft, b: SettingsDraft): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

const SETTINGS_ERROR_FIELDS: Partial<Record<ErrorCode, SettingsField>> = {
  INVALID_LINEUP_SIZE: 'lineupSize',
  INVALID_LINEUP_LOCK_HOURS: 'lineupLockHours',
  INVALID_LINEUP_MAX_CHANGES: 'lineupMaxChanges',
  INVALID_LINEUP_BUDGET: 'lineupBudget',
  INVALID_LINEUP_MINIMUMS: 'lineupFormation',
  INVALID_RATING_FORMAT: 'ratingFormat',
  INVALID_RATING_WINDOW_HOURS: 'ratingWindowHours',
  INVALID_RATING_MIN_VOTES: 'ratingMinVotes',
  INVALID_DEFAULT_RATING: 'defaultRating',
  INVALID_RATING_CONVERSION: 'ratingConversion',
};

/**
 * The control that a `400` of `PUT /settings` is about (decision 11 of plan 13); `null` for a code
 * with no control, such as `SCORING_TABLE_INCOMPLETE` or a market one, which goes in the notice.
 */
export function settingsErrorField(code: ErrorCode): SettingsField | null {
  return SETTINGS_ERROR_FIELDS[code] ?? null;
}
