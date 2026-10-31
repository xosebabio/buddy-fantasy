import type { LeagueSettingsBody } from '../../../core/api/league/league-api';
import {
  sameDraft,
  settingsErrorField,
  toSettingsBody,
  toSettingsDraft,
  unparsableDecimals,
  withPoints,
} from './settings-form';

const SETTINGS: LeagueSettingsBody = {
  ratingWindowHours: 24,
  ratingFormat: 'SPADES',
  ratingSpadesFactor: 4,
  ratingSpadesBase: -2,
  ratingDecimalFactor: 1.5,
  ratingDecimalBase: 0,
  ratingMinVotes: 1,
  defaultRating: null,
  lineupLockHours: 2,
  lineupMaxChanges: 2,
  lineupSize: 7,
  lineupFormationEnabled: false,
  lineupMinGoalkeepers: 1,
  lineupMinDefenders: 2,
  lineupMinMidfielders: 2,
  lineupMinForwards: 1,
  lineupBudgetEnabled: true,
  lineupBudget: 100.5,
  marketK: 0.05,
  marketMaxChangePct: 10,
  marketMinValue: 1,
  scoringRules: {
    GOAL: { GOALKEEPER: 6, DEFENDER: 5, MIDFIELDER: 4, FORWARD: 3 },
    OWN_GOAL: { GOALKEEPER: -2, DEFENDER: -2, MIDFIELDER: -2, FORWARD: -2 },
  },
};

describe('settings form', () => {
  describe('draft', () => {
    it('maps the settings to the form and back without changes', () => {
      const body = toSettingsBody(toSettingsDraft(SETTINGS), SETTINGS);

      expect(body).toEqual({
        ...SETTINGS,
        // The map holds the whole table: what was missing goes back as 0.
        scoringRules: {
          ...SETTINGS.scoringRules,
          ASSIST: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          PENALTY_GOAL: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          PENALTY_SAVED: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          PENALTY_MISSED: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          YELLOW_CARD: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          DOUBLE_YELLOW: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
          RED_CARD: { GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 },
        },
      });
    });

    it('sends the market parameters as they came', () => {
      const body = toSettingsBody({ ...toSettingsDraft(SETTINGS), lineupSize: 5 }, SETTINGS);

      expect(body.lineupSize).toBe(5);
      expect([body.marketK, body.marketMaxChangePct, body.marketMinValue]).toEqual([0.05, 10, 1]);
    });

    it('keeps what is switched off', () => {
      const draft = { ...toSettingsDraft(SETTINGS), budgetEnabled: false, formationEnabled: false };
      const body = toSettingsBody(draft, SETTINGS);

      expect(body.lineupBudgetEnabled).toBe(false);
      expect(body.lineupBudget).toBe(100.5);
      expect([body.lineupMinGoalkeepers, body.lineupMinDefenders]).toEqual([1, 2]);
    });

    it('changes one score of the table', () => {
      const draft = withPoints(toSettingsDraft(SETTINGS), 'GOAL', 3, 7);
      const body = toSettingsBody(draft, SETTINGS);

      expect(body.scoringRules['GOAL']).toEqual({
        GOALKEEPER: 6,
        DEFENDER: 5,
        MIDFIELDER: 4,
        FORWARD: 7,
      });
      expect(body.scoringRules['OWN_GOAL']?.['FORWARD']).toBe(-2);
    });

    it('tells whether two drafts are the same', () => {
      const draft = toSettingsDraft(SETTINGS);

      expect(sameDraft(draft, toSettingsDraft(SETTINGS))).toBe(true);
      expect(sameDraft(draft, withPoints(draft, 'GOAL', 0, 1))).toBe(false);
    });
  });

  describe('unparsableDecimals', () => {
    it('is empty for a valid draft', () => {
      expect(unparsableDecimals(toSettingsDraft(SETTINGS))).toEqual([]);
    });

    it('names the budget and the conversion of the format in use', () => {
      const draft = { ...toSettingsDraft(SETTINGS), budget: 'x', spadesBase: '', decimalBase: 'y' };

      expect(unparsableDecimals(draft)).toEqual(['lineupBudget', 'ratingConversion']);
      expect(unparsableDecimals({ ...draft, ratingFormat: 'DECIMAL' })).toEqual([
        'lineupBudget',
        'ratingConversion',
      ]);
      expect(unparsableDecimals({ ...draft, budget: '1', ratingFormat: 'DECIMAL' })).toEqual([
        'ratingConversion',
      ]);
    });
  });

  describe('settingsErrorField', () => {
    it('points each code to its control', () => {
      expect(settingsErrorField('INVALID_LINEUP_LOCK_HOURS')).toBe('lineupLockHours');
      expect(settingsErrorField('INVALID_LINEUP_MINIMUMS')).toBe('lineupFormation');
      expect(settingsErrorField('INVALID_RATING_CONVERSION')).toBe('ratingConversion');
      expect(settingsErrorField('INVALID_DEFAULT_RATING')).toBe('defaultRating');
    });

    it('has no control for the codes of the notice', () => {
      expect(settingsErrorField('SCORING_TABLE_INCOMPLETE')).toBeNull();
      expect(settingsErrorField('INVALID_MARKET_K')).toBeNull();
    });
  });
});
