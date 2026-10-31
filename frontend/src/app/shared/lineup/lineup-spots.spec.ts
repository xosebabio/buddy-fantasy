import type { Position } from '../position/position';
import {
  type LineupPlayerModel,
  type LineupSpot,
  listSpots,
  pitchLayout,
  spotKey,
} from './lineup-spots';

function player(id: string, position: Position): LineupPlayerModel {
  return { id, name: id, position, shirtNumber: null, value: 10 };
}

const keys = (spots: readonly LineupSpot[]) => spots.map(spotKey);

describe('pitchLayout', () => {
  it('puts a row per position from the forwards to the goalkeeper, without empty rows', () => {
    const layout = pitchLayout(
      [player('gk', 'GOALKEEPER'), player('fw', 'FORWARD'), player('d1', 'DEFENDER')],
      [],
    );

    expect(layout.rows.map(keys)).toEqual([['fw'], ['d1'], ['gk']]);
    expect(layout.foot).toEqual([]);
  });

  it('puts a free place with a position in its row and the others at the foot', () => {
    const layout = pitchLayout([player('gk', 'GOALKEEPER')], [null, 'DEFENDER', null]);

    expect(layout.rows.map(keys)).toEqual([['free-1'], ['gk']]);
    expect(keys(layout.foot)).toEqual(['free-0', 'free-2']);
    expect(layout.rows[0]?.[0]).toEqual({ kind: 'free', index: 1, position: 'DEFENDER' });
  });

  it('splits a row of more than five as evenly as it can', () => {
    const defenders = ['1', '2', '3', '4', '5', '6', '7'].map((id) => player(id, 'DEFENDER'));

    const layout = pitchLayout(defenders, []);

    expect(layout.rows.map(keys)).toEqual([
      ['1', '2', '3', '4'],
      ['5', '6', '7'],
    ]);
  });

  it('keeps a row of five whole', () => {
    const layout = pitchLayout(
      ['1', '2', '3', '4', '5'].map((id) => player(id, 'MIDFIELDER')),
      [],
    );

    expect(layout.rows).toHaveLength(1);
  });
});

describe('listSpots', () => {
  it('lists from the goalkeeper forwards, with each free place after its position', () => {
    const spots = listSpots(
      [player('fw', 'FORWARD'), player('gk', 'GOALKEEPER')],
      [null, 'MIDFIELDER'],
    );

    expect(keys(spots)).toEqual(['gk', 'free-1', 'fw', 'free-0']);
  });
});
