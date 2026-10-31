import { leagueRules, scoringRows } from './rules';
import { SETTINGS } from './settings.testing';

describe('leagueRules', () => {
  it('lists the lineup and rating parameters in the order of the page', () => {
    expect(leagueRules(SETTINGS).map((rule) => [rule.label, rule.value])).toEqual([
      ['Jugadores por alineación', '7'],
      ['Bloqueo de la alineación', '2 h antes del partido'],
      ['Cambios por jornada', '2'],
      ['Presupuesto', '100,0 M'],
      ['Mínimos por posición', '1-2-2-1 (porteros, defensas, medios y delanteros)'],
      ['Valoraciones', 'Picas'],
      ['Ventana de valoración', '48 h'],
    ]);
  });

  it('says that a budget or a formation that are off do not limit', () => {
    const rules = leagueRules({
      ...SETTINGS,
      lineupBudgetEnabled: false,
      lineupFormationEnabled: false,
      ratingFormat: 'DECIMAL',
    });

    expect(rules.map((rule) => rule.value)).toEqual(
      expect.arrayContaining(['Sin límite', 'Libre', 'Nota decimal']),
    );
  });
});

describe('scoringRows', () => {
  it('has a row per action in the order of the table and a column per position', () => {
    const rows = scoringRows(SETTINGS.scoringRules);

    expect(rows).toHaveLength(9);
    expect(rows[0]).toEqual({ action: 'GOAL', text: 'Gol', points: [10, 6, 5, 4] });
    expect(rows.find((row) => row.action === 'RED_CARD')?.points).toEqual([0, 0, 0, -3]);
  });

  it('leaves out a key that is not an action', () => {
    const rows = scoringRows({ ...SETTINGS.scoringRules, FLYING_KICK: { FORWARD: 9 } });

    expect(rows.map((row) => row.action)).not.toContain('FLYING_KICK');
  });
});
