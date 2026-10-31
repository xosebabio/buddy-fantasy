import type { MatchSummaryResponse } from '../../core/api/match/match-api';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../shared/roster/matches.testing';
import { closedMatches, latestFirst } from './matchdays';

describe('closedMatches', () => {
  it('keeps only the closed matches, the latest matchday first', () => {
    const older: MatchSummaryResponse = { ...CLOSED_SUMMARY, id: 'm10', matchday: 10 };

    const closed = closedMatches([older, SCHEDULED_SUMMARY, CLOSED_SUMMARY]);

    expect(closed.map((match) => match.id)).toEqual(['m11', 'm10']);
  });
});

describe('latestFirst', () => {
  it('reverses the matchdays that the API gives from the oldest', () => {
    expect(latestFirst([{ matchday: 1 }, { matchday: 3 }, { matchday: 2 }])).toEqual([
      { matchday: 3 },
      { matchday: 2 },
      { matchday: 1 },
    ]);
  });
});
