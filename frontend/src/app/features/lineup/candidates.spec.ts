import { ALBERTO, DIEGO, PABLO, PLAYERS, XURXO } from '../../shared/roster/players.testing';
import { candidateGroups } from './candidates';

describe('candidateGroups', () => {
  it('lists the position of a free place first and leaves out the lined up and the retired', () => {
    const groups = candidateGroups(PLAYERS, [DIEGO.id], undefined, 'FORWARD', 50);

    expect(groups.map((group) => group.position)).toEqual(['FORWARD']);
    expect(groups[0]?.candidates.map((c) => c.player.id)).toEqual([PABLO.id, ALBERTO.id]);
  });

  it('puts the position of the player being changed first, with them marked as current', () => {
    const groups = candidateGroups(PLAYERS, [PABLO.id], PABLO, null, 20);

    expect(groups.map((group) => group.position)).toEqual(['FORWARD', 'GOALKEEPER']);
    expect(groups[0]?.candidates.map((c) => [c.player.id, c.current])).toEqual([
      [PABLO.id, true],
      [ALBERTO.id, false],
    ]);
  });

  it('marks what does not fit in what is left of the budget, and all fits without a budget', () => {
    const tight = candidateGroups(PLAYERS, [], undefined, 'FORWARD', 12);
    const open = candidateGroups(PLAYERS, [], undefined, 'FORWARD', null);

    expect(tight[0]?.candidates.map((c) => c.fits)).toEqual([false, true]);
    expect(open[0]?.candidates.every((c) => c.fits)).toBe(true);
  });

  it('keeps the order of the positions after the first one when the place has none', () => {
    const groups = candidateGroups(
      [...PLAYERS, { ...XURXO, retiredAt: null }],
      [],
      undefined,
      null,
      null,
    );

    expect(groups.map((group) => group.position)).toEqual(['GOALKEEPER', 'DEFENDER', 'FORWARD']);
  });
});
