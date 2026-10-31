import type { PlayerResponse, SquadLimitsResponse } from '../../../core/api/squad/squad-api';
import { ALBERTO, DIEGO } from '../../../shared/roster/players.testing';
import {
  EMPTY_PLAYER,
  invalidPlayerFields,
  playerErrorField,
  samePlayer,
  toPlayerDraft,
  toPlayerRequest,
} from './player-form';

const LIMITS: SquadLimitsResponse = { nameMaxLength: 30, shirtNumberMin: 0, shirtNumberMax: 99 };

describe('player form', () => {
  it('maps a player to the form and back', () => {
    const player: PlayerResponse = { ...ALBERTO, initialValue: 12.5 };

    const draft = toPlayerDraft(player);

    expect(draft).toEqual({
      name: 'Alberto',
      shirtNumber: '7',
      position: 'FORWARD',
      initialValue: '12,5',
    });
    expect(toPlayerRequest(draft)).toEqual({
      name: 'Alberto',
      shirtNumber: 7,
      position: 'FORWARD',
      initialValue: 12.5,
    });
  });

  it('sends no shirt number when it is empty and trims the name', () => {
    const draft = { ...toPlayerDraft(DIEGO), name: '  Diego ', shirtNumber: ' ' };

    expect(toPlayerRequest(draft)).toMatchObject({ name: 'Diego', shirtNumber: null });
  });

  it('flags a value that is not a number and a shirt number out of the limits', () => {
    const base = { ...EMPTY_PLAYER, name: 'Iago', initialValue: '10' };

    expect(invalidPlayerFields(base, LIMITS)).toEqual([]);
    expect(invalidPlayerFields({ ...base, shirtNumber: '99' }, LIMITS)).toEqual([]);
    expect(invalidPlayerFields({ ...base, shirtNumber: '100' }, LIMITS)).toEqual(['shirtNumber']);
    expect(invalidPlayerFields({ ...base, shirtNumber: '9a' }, LIMITS)).toEqual(['shirtNumber']);
    expect(invalidPlayerFields({ ...base, initialValue: '' }, LIMITS)).toEqual(['initialValue']);
  });

  it('compares drafts by value', () => {
    expect(samePlayer(toPlayerDraft(DIEGO), toPlayerDraft(DIEGO))).toBe(true);
    expect(samePlayer(toPlayerDraft(DIEGO), { ...toPlayerDraft(DIEGO), name: 'Dieguito' })).toBe(
      false,
    );
  });

  it('puts the codes of the form in their control and leaves the rest without one', () => {
    expect(playerErrorField('INVALID_PLAYER_NAME')).toBe('name');
    expect(playerErrorField('SHIRT_NUMBER_TAKEN')).toBe('shirtNumber');
    expect(playerErrorField('INVALID_SHIRT_NUMBER')).toBe('shirtNumber');
    expect(playerErrorField('INVALID_INITIAL_VALUE')).toBe('initialValue');
    expect(playerErrorField('PLAYER_RETIRED')).toBeNull();
  });
});
