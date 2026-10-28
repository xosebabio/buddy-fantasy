import { apiPath } from './api-path';

describe('apiPath', () => {
  it('returns a path without parameters as it is', () => {
    expect(apiPath('/api/me')).toBe('/api/me');
  });

  it('fills in every parameter', () => {
    expect(apiPath('/api/leagues/{id}/members/{memberId}', { id: 'l1', memberId: 'u2' })).toBe(
      '/api/leagues/l1/members/u2',
    );
  });

  it('encodes the parameters', () => {
    expect(apiPath('/api/invitations/{token}', { token: 'a/b c' })).toBe(
      '/api/invitations/a%2Fb%20c',
    );
  });

  it('only takes paths of the spec with all their parameters', () => {
    // @ts-expect-error -- not a path of the spec
    expect(apiPath('/api/unknown')).toBe('/api/unknown');
    // @ts-expect-error -- the id is missing
    expect(() => apiPath('/api/leagues/{id}')).toThrow('Missing the id parameter');
    expect(() =>
      // @ts-expect-error -- the memberId is missing
      apiPath('/api/leagues/{id}/members/{memberId}', { id: 'l1' }),
    ).toThrow('Missing the memberId parameter');
  });
});
