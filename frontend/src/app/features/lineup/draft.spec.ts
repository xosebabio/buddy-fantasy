import { SETTINGS } from '../../shared/roster/settings.testing';
import { ALBERTO, DIEGO, PABLO, XURXO } from '../../shared/roster/players.testing';
import { checkDraft, draftRules, lineupPlayer, sameLineup } from './draft';

const rules = draftRules(SETTINGS, 40);
const NONE = new Set<string>();

describe('draft check', () => {
  it('takes the minimums from the settings and zeroes them without a formation', () => {
    expect(rules.minimums).toEqual({ GOALKEEPER: 1, DEFENDER: 2, MIDFIELDER: 2, FORWARD: 1 });
    const free = draftRules({ ...SETTINGS, lineupFormationEnabled: false }, null);

    expect(free.minimums).toEqual({ GOALKEEPER: 0, DEFENDER: 0, MIDFIELDER: 0, FORWARD: 0 });
  });

  it('puts the free places with a position first and the rest without one', () => {
    const check = checkDraft([DIEGO, PABLO].map(lineupPlayer), rules, NONE);

    expect(check.freeSpots).toEqual(['DEFENDER', 'DEFENDER', 'MIDFIELDER', 'MIDFIELDER', null]);
    expect(check.lacking).toEqual(['DEFENDER', 'MIDFIELDER']);
    expect(check.excess).toBe(0);
  });

  it('has no free places with a position when the formation is off', () => {
    const check = checkDraft(
      [DIEGO].map(lineupPlayer),
      draftRules({ ...SETTINGS, lineupFormationEnabled: false }, null),
      NONE,
    );

    expect(check.freeSpots).toEqual([null, null, null, null, null, null]);
    expect(check.lacking).toEqual([]);
  });

  it('measures how far the total is over the budget, rounded', () => {
    const check = checkDraft(
      [DIEGO, PABLO, ALBERTO].map(lineupPlayer),
      draftRules(SETTINGS, 35),
      NONE,
    );

    expect(check.total).toBeCloseTo(37.7);
    expect(check.overBudgetBy).toBe(2.7);
  });

  it('does not complain about the budget when there is none or it fits', () => {
    const players = [DIEGO, PABLO].map(lineupPlayer);

    expect(checkDraft(players, draftRules(SETTINGS, null), NONE).overBudgetBy).toBe(0);
    expect(checkDraft(players, rules, NONE).overBudgetBy).toBe(0);
  });

  it('counts the players over the size and the retired ones', () => {
    const players = [DIEGO, PABLO, ALBERTO, XURXO].map(lineupPlayer);
    const check = checkDraft(players, { ...rules, size: 3 }, new Set([XURXO.id]));

    expect(check.excess).toBe(1);
    expect(check.freeSpots).toEqual([]);
    expect(check.retired).toBe(1);
  });
});

describe('sameLineup', () => {
  it('ignores the order', () => {
    expect(sameLineup(['a', 'b'], ['b', 'a'])).toBe(true);
  });

  it('tells a different player or size apart', () => {
    expect(sameLineup(['a', 'b'], ['a', 'c'])).toBe(false);
    expect(sameLineup(['a'], ['a', 'b'])).toBe(false);
  });
});
