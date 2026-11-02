import type { MatchSnapshotResponse } from '../../../core/api/match/match-api';
import { IDLE, KEEPER, STRIKER } from '../../../shared/roster/matches.testing';
import { auditChanges } from './audit-changes';

const NAMES: Record<string, string> = { p1: 'Diego', p7: 'Alberto', p9: 'Pablo' };
const nameOf = (id: string) => NAMES[id] ?? '?';

const SNAPSHOT: MatchSnapshotResponse = {
  matchday: 11,
  opponent: 'Racing Vilariño',
  venue: 'HOME',
  kickoffAt: '2026-10-26T10:00:00Z',
  status: 'CLOSED',
  closedAt: '2026-10-26T12:00:00Z',
  result: { goalsFor: 3, goalsAgainst: 1 },
  performances: [STRIKER, KEEPER],
};

describe('auditChanges', () => {
  it('tells the whole state of a match that was created', () => {
    const changes = auditChanges(null, { ...SNAPSHOT, result: null, performances: [IDLE] }, nameOf);

    expect(changes.map((c) => c.text)).toEqual([
      'Jornada: 11',
      'Hora: lunes, 26 de octubre, 11:00',
      'Rival: Racing Vilariño',
      'Campo: En casa',
      'Alberto jugó',
    ]);
    expect(changes.every((c) => c.before === null && c.after === null)).toBe(true);
  });

  it('tells the whole state of a match that was deleted', () => {
    const changes = auditChanges(SNAPSHOT, null, nameOf);

    expect(changes.map((c) => c.text)).toContain('Resultado: 3–1');
    expect(changes.map((c) => c.text)).toContain('Pablo: 2 goles, 1 asistencia, tarjeta amarilla');
    expect(changes.map((c) => c.text)).toContain('Diego: 1 penalti parado');
  });

  it('is empty when nothing differs', () => {
    expect(auditChanges(SNAPSHOT, SNAPSHOT, nameOf)).toEqual([]);
  });

  it('tells the data of the match that changed', () => {
    const changes = auditChanges(
      SNAPSHOT,
      {
        ...SNAPSHOT,
        matchday: 12,
        opponent: 'CD Arrabal',
        venue: 'AWAY',
        kickoffAt: '2026-11-02T10:00:00Z',
      },
      nameOf,
    );

    expect(changes).toEqual([
      { text: 'Jornada', before: '11', after: '12' },
      {
        text: 'Hora',
        before: 'lunes, 26 de octubre, 11:00',
        after: 'lunes, 2 de noviembre, 11:00',
      },
      { text: 'Rival', before: 'Racing Vilariño', after: 'CD Arrabal' },
      { text: 'Campo', before: 'En casa', after: 'Fuera' },
    ]);
  });

  it('tells the score, also when it was missing or is now', () => {
    expect(
      auditChanges(SNAPSHOT, { ...SNAPSHOT, result: { goalsFor: 3, goalsAgainst: 2 } }, nameOf),
    ).toEqual([{ text: 'Resultado', before: '3–1', after: '3–2' }]);
    expect(auditChanges({ ...SNAPSHOT, result: null }, SNAPSHOT, nameOf)).toEqual([
      { text: 'Resultado', before: 'sin resultado', after: '3–1' },
    ]);
  });

  it('tells who comes in and who goes out of the sheet', () => {
    const changes = auditChanges(SNAPSHOT, { ...SNAPSHOT, performances: [STRIKER, IDLE] }, nameOf);

    expect(changes).toEqual([
      { text: 'Alberto entra en el acta', before: null, after: null },
      { text: 'Diego sale del acta', before: null, after: null },
    ]);
  });

  it('tells the stats and the cards that changed, only for that player', () => {
    const changes = auditChanges(
      SNAPSHOT,
      {
        ...SNAPSHOT,
        performances: [{ ...STRIKER, assists: 2, yellowCard: false, redCard: true }, KEEPER],
      },
      nameOf,
    );

    expect(changes).toEqual([
      { text: 'Pablo: asistencias', before: '1', after: '2' },
      { text: 'Pablo: amarilla', before: 'sí', after: 'no' },
      { text: 'Pablo: roja directa', before: 'no', after: 'sí' },
    ]);
  });
});
