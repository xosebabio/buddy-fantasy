import { actionPoints, isScoringAction, SCORING_ACTION_TEXTS } from './scoring-action';

describe('isScoringAction', () => {
  it('tells the actions from any other key', () => {
    expect(isScoringAction('PENALTY_SAVED')).toBe(true);
    expect(isScoringAction('CLEAN_SHEET')).toBe(false);
    expect(isScoringAction('toString')).toBe(false);
  });
});

describe('actionPoints', () => {
  it('gives the actions of a map in the order of the points table, without unknown keys', () => {
    expect(actionPoints({ YELLOW_CARD: -1, CLEAN_SHEET: 4, GOAL: 6, ASSIST: 1 })).toEqual([
      { action: 'GOAL', points: 6 },
      { action: 'ASSIST', points: 1 },
      { action: 'YELLOW_CARD', points: -1 },
    ]);
  });

  it('gives nothing for a player without actions', () => {
    expect(actionPoints({})).toEqual([]);
  });

  it('has a text for every action', () => {
    expect(SCORING_ACTION_TEXTS.OWN_GOAL).toBe('Gol en propia puerta');
  });
});
