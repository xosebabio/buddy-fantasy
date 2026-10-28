import { ERROR_TEXTS, isErrorCode } from './error-texts';

describe('ERROR_TEXTS', () => {
  it('has a text for every code', () => {
    for (const text of Object.values(ERROR_TEXTS)) {
      expect(text.trim()).not.toBe('');
    }
  });
});

describe('isErrorCode', () => {
  it('accepts a code of the spec', () => {
    expect(isErrorCode('ADMIN_CANNOT_LEAVE')).toBe(true);
  });

  it.each(['UNKNOWN', 'toString', 42, null])('rejects %s', (value) => {
    expect(isErrorCode(value)).toBe(false);
  });
});
