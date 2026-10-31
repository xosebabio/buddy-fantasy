import type { MemberResponse } from '../../core/api/league/league-api';
import { USER } from '../../core/auth/session.testing';

/** The members of a league in the tests of its pages; only the tests import this file. */
export const MEMBERS: MemberResponse[] = [
  {
    userId: USER.id,
    displayName: USER.displayName,
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
