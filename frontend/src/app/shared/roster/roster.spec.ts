import type { MemberResponse } from '../../core/api/league/league-api';
import type { PlayerResponse } from '../../core/api/squad/squad-api';
import { displayName, memberIndex, memberName, playerIndex } from './roster';

const ACTIVE: PlayerResponse = {
  id: 'p1',
  name: 'Diego',
  position: 'GOALKEEPER',
  shirtNumber: 1,
  initialValue: 10,
  currentValue: 11.5,
  photoUrl: null,
  createdAt: '2026-01-11T10:00:00Z',
  retiredAt: null,
};

const RETIRED: PlayerResponse = {
  ...ACTIVE,
  id: 'p2',
  name: 'Pablo',
  position: 'FORWARD',
  shirtNumber: 9,
  retiredAt: '2026-09-01T10:00:00Z',
};

const NAMED: MemberResponse = {
  userId: 'u1',
  displayName: 'Brais Otero',
  avatarUrl: null,
  role: 'ADMIN',
  joinedAt: '2026-01-11T10:00:00Z',
};

const NAMELESS: MemberResponse = { ...NAMED, userId: 'u2', displayName: null, role: 'MANAGER' };

describe('roster', () => {
  it('indexes the squad by id, retired players included', () => {
    const index = playerIndex([ACTIVE, RETIRED]);

    expect(index.get('p1')).toBe(ACTIVE);
    expect(index.get('p2')).toBe(RETIRED);
    expect(index.get('p3')).toBeUndefined();
  });

  it('indexes the members by user id', () => {
    const index = memberIndex([NAMED, NAMELESS]);

    expect(index.get('u1')).toBe(NAMED);
    expect(index.get('u2')).toBe(NAMELESS);
  });

  it('names a member by their name, or as a user without one', () => {
    expect(displayName(NAMED)).toBe('Brais Otero');
    expect(displayName(NAMELESS)).toBe('Usuario sin nombre');
  });

  it('names a user who is no longer a member as a former member', () => {
    const index = memberIndex([NAMED, NAMELESS]);

    expect(memberName(index, 'u1')).toBe('Brais Otero');
    expect(memberName(index, 'u2')).toBe('Usuario sin nombre');
    expect(memberName(index, 'u9')).toBe('Antiguo miembro');
  });
});
