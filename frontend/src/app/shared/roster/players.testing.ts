import type { PlayerResponse } from '../../core/api/squad/squad-api';

/** The squad of a league in the tests of the pages that cross player ids; only the tests import this file. */
export const DIEGO: PlayerResponse = {
  id: 'p1',
  name: 'Diego',
  position: 'GOALKEEPER',
  shirtNumber: 1,
  currentValue: 8.5,
  initialValue: 8,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: null,
};

export const PABLO: PlayerResponse = {
  id: 'p9',
  name: 'Pablo',
  position: 'FORWARD',
  shirtNumber: 9,
  currentValue: 17.2,
  initialValue: 15,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: null,
};

export const ALBERTO: PlayerResponse = {
  id: 'p7',
  name: 'Alberto',
  position: 'FORWARD',
  shirtNumber: 7,
  currentValue: 12,
  initialValue: 12,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: null,
};

export const XURXO: PlayerResponse = {
  id: 'p5',
  name: 'Xurxo',
  position: 'DEFENDER',
  shirtNumber: 5,
  currentValue: 9.1,
  initialValue: 10,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: '2026-06-01T10:00:00Z',
};

export const PLAYERS: PlayerResponse[] = [DIEGO, PABLO, ALBERTO, XURXO];
