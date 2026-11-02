import type { MatchLimitsResponse, MatchSummaryResponse } from '../../../core/api/match/match-api';
import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../../../shared/roster/matches.testing';
import {
  EMPTY_MATCH,
  invalidMatchFields,
  matchErrorField,
  postpones,
  proposeMatch,
  sameMatch,
  toMatchDraft,
  toMatchRequest,
} from './match-form';

const LIMITS: MatchLimitsResponse = {
  opponentMaxLength: 40,
  matchdayMin: 1,
  goalsMax: 30,
  statCountMax: 20,
  sheetPlayersMax: 30,
};

describe('match form', () => {
  it('maps a match to the form, in Madrid time, and back', () => {
    const draft = toMatchDraft(SCHEDULED_SUMMARY);

    expect(draft).toEqual({
      matchday: '12',
      opponent: 'CD Arrabal',
      venue: 'AWAY',
      date: '2026-11-02',
      time: '11:00',
    });
    expect(toMatchRequest(draft)).toEqual({
      matchday: 12,
      opponent: 'CD Arrabal',
      venue: 'AWAY',
      kickoffAt: '2026-11-02T10:00:00.000Z',
    });
  });

  it('trims the opponent', () => {
    expect(
      toMatchRequest({ ...toMatchDraft(SCHEDULED_SUMMARY), opponent: ' Arrabal ' }),
    ).toMatchObject({
      opponent: 'Arrabal',
    });
  });

  it('flags a matchday that is not a whole number within the limit and an empty day or time', () => {
    const draft = toMatchDraft(SCHEDULED_SUMMARY);

    expect(invalidMatchFields(draft, LIMITS)).toEqual([]);
    expect(invalidMatchFields({ ...draft, matchday: '0' }, LIMITS)).toEqual(['matchday']);
    expect(invalidMatchFields({ ...draft, matchday: '1,5' }, LIMITS)).toEqual(['matchday']);
    expect(invalidMatchFields({ ...draft, date: '', time: '' }, LIMITS)).toEqual(['date', 'time']);
  });

  it('proposes the matchday after the latest and its time a week later', () => {
    const calendar: MatchSummaryResponse[] = [SCHEDULED_SUMMARY, CLOSED_SUMMARY];

    expect(proposeMatch(calendar)).toEqual({
      matchday: '13',
      opponent: '',
      venue: 'HOME',
      date: '2026-11-09',
      time: '11:00',
    });
  });

  it('keeps the time in Madrid when a week later the clock has changed', () => {
    // Saturday 24 October 2026, 11:00 in Madrid (summer time); the clock goes back on the 25th.
    const proposal = proposeMatch([{ ...SCHEDULED_SUMMARY, kickoffAt: '2026-10-24T09:00:00Z' }]);

    expect(proposal).toMatchObject({ date: '2026-10-31', time: '11:00' });
  });

  it('proposes nothing but an empty form for an empty calendar', () => {
    expect(proposeMatch([])).toEqual(EMPTY_MATCH);
  });

  it('compares drafts and tells when a change postpones the match', () => {
    const saved = toMatchDraft(SCHEDULED_SUMMARY);

    expect(sameMatch(saved, { ...saved })).toBe(true);
    expect(sameMatch(saved, { ...saved, opponent: 'Otro' })).toBe(false);
    expect(postpones({ ...saved, opponent: 'Otro', venue: 'HOME' }, saved)).toBe(false);
    expect(postpones({ ...saved, matchday: '13' }, saved)).toBe(true);
    expect(postpones({ ...saved, date: '2026-11-03' }, saved)).toBe(true);
    expect(postpones({ ...saved, time: '12:00' }, saved)).toBe(true);
  });

  it.each([
    ['INVALID_MATCHDAY', 'matchday'],
    ['MATCHDAY_TAKEN', 'matchday'],
    ['INVALID_OPPONENT', 'opponent'],
    ['KICKOFF_TAKEN', 'time'],
    ['MATCH_CLOSED', null],
  ] as const)('puts %s in the %s control', (code, field) => {
    expect(matchErrorField(code)).toBe(field);
  });
});
