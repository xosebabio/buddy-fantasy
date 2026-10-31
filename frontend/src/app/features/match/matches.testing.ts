import type {
  MatchResponse,
  MatchSummaryResponse,
  PlayerPerformanceResponse,
} from '../../core/api/match/match-api';

/** The calendar of a league in the tests of its pages; only the tests import this file. */
export const CLOSED_SUMMARY: MatchSummaryResponse = {
  id: 'm11',
  matchday: 11,
  opponent: 'Racing Vilariño',
  venue: 'HOME',
  kickoffAt: '2026-10-26T10:00:00Z',
  status: 'CLOSED',
  closedAt: '2026-10-26T12:00:00Z',
  result: { goalsFor: 3, goalsAgainst: 1 },
};

export const SCHEDULED_SUMMARY: MatchSummaryResponse = {
  id: 'm12',
  matchday: 12,
  opponent: 'CD Arrabal',
  venue: 'AWAY',
  kickoffAt: '2026-11-02T10:00:00Z',
  status: 'SCHEDULED',
  closedAt: null,
  result: null,
};

export const STRIKER: PlayerPerformanceResponse = {
  playerId: 'p9',
  position: 'FORWARD',
  goals: 2,
  penaltyGoals: 0,
  assists: 1,
  ownGoals: 0,
  penaltiesSaved: 0,
  penaltiesMissed: 0,
  yellowCard: true,
  doubleYellow: false,
  redCard: false,
};

export const KEEPER: PlayerPerformanceResponse = {
  playerId: 'p1',
  position: 'GOALKEEPER',
  goals: 0,
  penaltyGoals: 0,
  assists: 0,
  ownGoals: 0,
  penaltiesSaved: 1,
  penaltiesMissed: 0,
  yellowCard: false,
  doubleYellow: false,
  redCard: false,
};

export const IDLE: PlayerPerformanceResponse = {
  playerId: 'p7',
  position: 'FORWARD',
  goals: 0,
  penaltyGoals: 0,
  assists: 0,
  ownGoals: 0,
  penaltiesSaved: 0,
  penaltiesMissed: 0,
  yellowCard: false,
  doubleYellow: false,
  redCard: false,
};

export const CLOSED_MATCH: MatchResponse = {
  ...CLOSED_SUMMARY,
  warnings: null,
  performances: [STRIKER, KEEPER, IDLE],
};

export const SCHEDULED_MATCH: MatchResponse = {
  ...SCHEDULED_SUMMARY,
  warnings: null,
  performances: null,
};
