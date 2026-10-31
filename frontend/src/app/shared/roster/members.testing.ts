import type { MemberResponse } from '../../core/api/league/league-api';

/** The members of a league in the tests of its pages; only the tests import this file. The first is the user of `session.testing`. */
export const MEMBERS: MemberResponse[] = [
  {
    userId: 'u1',
    displayName: 'Xosé Babío',
    avatarUrl: null,
    role: 'ADMIN',
    joinedAt: '2026-01-11T10:00:00Z',
  },
  {
    userId: 'u2',
    displayName: 'Brais Otero',
    avatarUrl: null,
    role: 'MANAGER',
    joinedAt: '2026-02-01T10:00:00Z',
  },
  {
    userId: 'u3',
    displayName: null,
    avatarUrl: null,
    role: 'MANAGER',
    joinedAt: '2026-03-01T10:00:00Z',
  },
];
