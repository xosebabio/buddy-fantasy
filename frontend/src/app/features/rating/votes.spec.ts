import { sameVotes, savedVotes, votedCount, votesRequest } from './votes';

describe('votes', () => {
  const saved = savedVotes([
    { playerId: 'p1', myRating: 3 },
    { playerId: 'p2', myRating: null },
    { playerId: 'p3', myRating: 0 },
  ]);

  it('keeps a 0 as a vote and sends only the players with one', () => {
    expect(votesRequest(saved)).toEqual({
      ratings: [
        { playerId: 'p1', value: 3 },
        { playerId: 'p3', value: 0 },
      ],
    });
    expect(votedCount(saved)).toBe(2);
  });

  it('tells a changed vote from an equal set', () => {
    expect(sameVotes(saved, new Map(saved))).toBe(true);
    expect(sameVotes(saved, new Map(saved).set('p2', 1))).toBe(false);
    expect(sameVotes(saved, new Map(saved).set('p3', null))).toBe(false);
    expect(sameVotes(saved, new Map([['p1', 3]]))).toBe(false);
  });
});
