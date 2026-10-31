import { MATCH_WARNING_TEXTS } from './match-warning';

describe('MATCH_WARNING_TEXTS', () => {
  it('has a text for every coherence warning of the API', () => {
    expect(Object.keys(MATCH_WARNING_TEXTS).sort()).toEqual([
      'GOALS_MISMATCH',
      'OWN_GOALS_EXCEED_GOALS_AGAINST',
    ]);
    expect(Object.values(MATCH_WARNING_TEXTS).every((text) => text.length > 0)).toBe(true);
  });
});
