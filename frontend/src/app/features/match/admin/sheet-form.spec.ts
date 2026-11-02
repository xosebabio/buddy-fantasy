import { CLOSED_MATCH, SCHEDULED_MATCH } from '../../../shared/roster/matches.testing';
import { ALBERTO, DIEGO, PABLO, XURXO } from '../../../shared/roster/players.testing';
import {
  cardDisabled,
  EMPTY_PERFORMANCE,
  EMPTY_SHEET,
  missingMark,
  removedPlayerIds,
  sameSheet,
  setCard,
  setPlayed,
  setStat,
  sheetErrorField,
  sheetGroups,
  toSheetDraft,
  toSheetRequest,
} from './sheet-form';

describe('sheet form', () => {
  it('maps a closed match to the draft and back', () => {
    const draft = toSheetDraft(CLOSED_MATCH);

    expect(draft.goalsFor).toBe(3);
    expect(draft.goalsAgainst).toBe(1);
    expect(Object.keys(draft.players)).toEqual(['p9', 'p1', 'p7']);
    expect(draft.players['p9']).toMatchObject({ goals: 2, assists: 1, yellowCard: true });
    expect(toSheetRequest(draft)).toEqual({
      result: { goalsFor: 3, goalsAgainst: 1 },
      performances: [
        expect.objectContaining({ playerId: 'p9', goals: 2 }),
        expect.objectContaining({ playerId: 'p1', penaltiesSaved: 1 }),
        expect.objectContaining({ playerId: 'p7', goals: 0 }),
      ],
    });
  });

  it('starts empty for a match without a sheet and sends no result until both marks are typed', () => {
    const draft = toSheetDraft(SCHEDULED_MATCH);

    expect(draft).toEqual(EMPTY_SHEET);
    expect(toSheetRequest({ ...draft, goalsFor: 2 }).result).toBeNull();
    expect(toSheetRequest({ ...draft, goalsFor: 2, goalsAgainst: 0 }).result).toEqual({
      goalsFor: 2,
      goalsAgainst: 0,
    });
  });

  it('reports the mark that is missing when only one is typed', () => {
    expect(missingMark(EMPTY_SHEET)).toBeNull();
    expect(missingMark({ ...EMPTY_SHEET, goalsFor: 1 })).toBe('goalsAgainst');
    expect(missingMark({ ...EMPTY_SHEET, goalsAgainst: 1 })).toBe('goalsFor');
    expect(missingMark({ ...EMPTY_SHEET, goalsFor: 1, goalsAgainst: 0 })).toBeNull();
  });

  it('adds and removes players and edits their counters', () => {
    let draft = setPlayed(EMPTY_SHEET, 'p1', true);
    expect(draft.players['p1']).toEqual(EMPTY_PERFORMANCE);
    draft = setStat(draft, 'p1', 'goals', 2);
    expect(draft.players['p1']?.goals).toBe(2);
    expect(setStat(draft, 'p9', 'goals', 1)).toBe(draft);
    expect(setPlayed(draft, 'p1', true)).toBe(draft);
    expect(setPlayed(draft, 'p1', false).players).toEqual({});
  });

  it('clears the other cards when the double yellow is checked and blocks them', () => {
    let draft = setPlayed(EMPTY_SHEET, 'p1', true);
    draft = setCard(setCard(draft, 'p1', 'yellowCard', true), 'p1', 'redCard', true);
    expect(draft.players['p1']).toMatchObject({ yellowCard: true, redCard: true });

    draft = setCard(draft, 'p1', 'doubleYellow', true);
    const performance = draft.players['p1'] ?? EMPTY_PERFORMANCE;
    expect(performance).toMatchObject({ yellowCard: false, redCard: false, doubleYellow: true });
    expect(cardDisabled(performance, 'yellowCard')).toBe(true);
    expect(cardDisabled(performance, 'redCard')).toBe(true);
    expect(cardDisabled(performance, 'doubleYellow')).toBe(false);
  });

  it('compares drafts by score, players and stats', () => {
    const saved = toSheetDraft(CLOSED_MATCH);

    expect(sameSheet(saved, toSheetDraft(CLOSED_MATCH))).toBe(true);
    expect(sameSheet(saved, { ...saved, goalsFor: 4 })).toBe(false);
    expect(sameSheet(saved, setStat(saved, 'p9', 'assists', 2))).toBe(false);
    expect(sameSheet(saved, setPlayed(saved, 'p7', false))).toBe(false);
  });

  it('lists the players that a correction takes out of the sheet', () => {
    const saved = toSheetDraft(CLOSED_MATCH);

    expect(removedPlayerIds(saved, saved)).toEqual([]);
    expect(removedPlayerIds(saved, setPlayed(setPlayed(saved, 'p9', false), 'p1', false))).toEqual([
      'p9',
      'p1',
    ]);
  });

  it('groups the squad by position and keeps only the retired players that are in the sheet', () => {
    const retiredInSheet = { ...ALBERTO, retiredAt: '2026-10-01T10:00:00Z' };
    const retiredOut = { ...XURXO, retiredAt: '2026-10-01T10:00:00Z' };
    const groups = sheetGroups(
      [PABLO, retiredOut, DIEGO, retiredInSheet],
      toSheetDraft(CLOSED_MATCH),
    );

    expect(groups.map((group) => group.position)).toEqual(['GOALKEEPER', 'FORWARD']);
    expect(groups[1]?.players.map((player) => player.name)).toEqual(['Alberto', 'Pablo']);
  });

  it('paints only INVALID_GOALS in the result', () => {
    expect(sheetErrorField('INVALID_GOALS')).toBe('result');
    expect(sheetErrorField('INVALID_STAT_COUNT')).toBeNull();
    expect(sheetErrorField('DUPLICATE_PLAYER')).toBeNull();
  });
});
