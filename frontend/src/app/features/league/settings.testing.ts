import type { LeagueSettingsBody } from '../../core/api/league/league-api';

/** The settings of a league in the tests of its pages; only the tests import this file. */
export const SETTINGS: LeagueSettingsBody = {
  ratingWindowHours: 48,
  ratingFormat: 'SPADES',
  ratingSpadesFactor: 1,
  ratingSpadesBase: 0,
  ratingDecimalFactor: 1,
  ratingDecimalBase: 0,
  ratingMinVotes: 3,
  defaultRating: null,
  lineupLockHours: 2,
  lineupMaxChanges: 2,
  lineupSize: 7,
  lineupFormationEnabled: true,
  lineupMinGoalkeepers: 1,
  lineupMinDefenders: 2,
  lineupMinMidfielders: 2,
  lineupMinForwards: 1,
  lineupBudgetEnabled: true,
  lineupBudget: 100,
  marketK: 0.1,
  marketMaxChangePct: 10,
  marketMinValue: 1,
  scoringRules: {
    GOAL: { GOALKEEPER: 10, DEFENDER: 6, MIDFIELDER: 5, FORWARD: 4 },
    RED_CARD: { FORWARD: -3 },
  },
};
