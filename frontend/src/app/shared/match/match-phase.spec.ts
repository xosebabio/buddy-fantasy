import { matchPhase } from './match-phase';

const NOW = new Date('2026-11-02T10:00:00Z');

describe('matchPhase', () => {
  it('is scheduled before the kickoff', () => {
    expect(matchPhase({ status: 'SCHEDULED', kickoffAt: '2026-11-02T10:00:01Z' }, NOW)).toBe(
      'SCHEDULED',
    );
  });

  it('is pending from the kickoff until it is closed', () => {
    expect(matchPhase({ status: 'SCHEDULED', kickoffAt: '2026-11-02T10:00:00Z' }, NOW)).toBe(
      'PENDING',
    );
  });

  it('is closed once the admin closes it, whatever the time', () => {
    expect(matchPhase({ status: 'CLOSED', kickoffAt: '2026-11-09T10:00:00Z' }, NOW)).toBe('CLOSED');
  });
});
