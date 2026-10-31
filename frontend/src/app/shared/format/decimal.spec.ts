import { decimalText, parseDecimal } from './decimal';

describe('decimal', () => {
  it('writes and reads them with a comma', () => {
    expect(decimalText(100.5)).toBe('100,5');
    expect(parseDecimal('100,5')).toBe(100.5);
    expect(parseDecimal(' 4.25 ')).toBe(4.25);
    expect(parseDecimal('−2')).toBe(-2);
  });

  it('rejects what is not a number', () => {
    expect(parseDecimal('')).toBeNull();
    expect(parseDecimal('4,')).toBeNull();
    expect(parseDecimal('-')).toBeNull();
    expect(parseDecimal('1e3')).toBeNull();
  });
});
