import { IDLE, KEEPER, STRIKER } from '../roster/matches.testing';
import { performanceSummary } from './performance';

describe('performanceSummary', () => {
  it('names what a player did, with its count', () => {
    expect(performanceSummary({ ...STRIKER, goals: 1 })).toEqual([
      '1 gol',
      '1 asistencia',
      'tarjeta amarilla',
    ]);
    expect(performanceSummary(STRIKER)).toEqual(['2 goles', '1 asistencia', 'tarjeta amarilla']);
    expect(performanceSummary(KEEPER)).toEqual(['1 penalti parado']);
  });

  it('is empty for a player who only played', () => {
    expect(performanceSummary(IDLE)).toEqual([]);
  });

  it('names penalties, own goals and the other cards', () => {
    expect(
      performanceSummary({
        ...IDLE,
        penaltyGoals: 2,
        ownGoals: 1,
        penaltiesMissed: 1,
        doubleYellow: true,
        redCard: true,
      }),
    ).toEqual([
      '2 goles de penalti',
      '1 gol en propia puerta',
      '1 penalti fallado',
      'doble amarilla',
      'roja directa',
    ]);
  });
});
