import type { PlayerRatingResponse, RatingsRequest } from '../../core/api/rating/rating-api';

/** The vote of each player by id; `null` is no vote, which a 0 never is. */
export type Votes = ReadonlyMap<string, number | null>;

/** The user's votes as the server has them. */
export function savedVotes(
  players: readonly Pick<PlayerRatingResponse, 'playerId' | 'myRating'>[],
): Votes {
  return new Map(players.map((player) => [player.playerId, player.myRating]));
}

/** Whether two sets of votes say the same for every player. */
export function sameVotes(a: Votes, b: Votes): boolean {
  return (
    a.size === b.size &&
    [...a].every(([playerId, vote]) => b.has(playerId) && b.get(playerId) === vote)
  );
}

/** The request that replaces the user's votes: whoever has no vote is left out. */
export function votesRequest(votes: Votes): RatingsRequest {
  return {
    ratings: [...votes].flatMap(([playerId, value]) =>
      value === null ? [] : [{ playerId, value }],
    ),
  };
}

/** How many players have a vote. */
export function votedCount(votes: Votes): number {
  return [...votes.values()].filter((vote) => vote !== null).length;
}
