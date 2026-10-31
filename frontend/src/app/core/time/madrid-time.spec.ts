import { formatInstant, madridToInstant } from './madrid-time';

describe('formatInstant', () => {
  it.each([
    ['dateTime', 'lunes, 2 de noviembre, 11:00'],
    ['date', 'lunes, 2 de noviembre'],
    ['short', '2 nov, 11:00'],
    ['time', '11:00'],
  ] as const)('writes the %s format in Spanish and in Madrid time', (dateFormat, text) => {
    expect(formatInstant('2026-11-02T10:00:00Z', dateFormat)).toBe(text);
  });

  it('follows summer time in Madrid', () => {
    expect(formatInstant('2026-07-05T09:00:00Z', 'time')).toBe('11:00');
  });

  it('takes a Date as well', () => {
    expect(formatInstant(new Date('2026-11-02T10:00:00Z'), 'time')).toBe('11:00');
  });

  it.each([
    ['2027-03-28T00:59:00Z', '01:59'],
    ['2027-03-28T01:00:00Z', '03:00'],
    ['2026-10-25T00:30:00Z', '02:30'],
    ['2026-10-25T01:30:00Z', '02:30'],
    ['2026-10-25T02:00:00Z', '03:00'],
  ])('shows %s as %s across the clock changes', (instant, time) => {
    expect(formatInstant(instant, 'time')).toBe(time);
  });
});

describe('madridToInstant', () => {
  it.each([
    ['2026-11-02', '11:00', '2026-11-02T10:00:00.000Z'],
    ['2026-07-05', '11:00', '2026-07-05T09:00:00.000Z'],
  ])('turns %s %s in Madrid into %s', (date, time, instant) => {
    expect(madridToInstant(date, time)).toBe(instant);
  });

  describe('on the last Sunday of March, when the clock goes forward at 02:00', () => {
    it.each([
      ['01:59', '2027-03-28T00:59:00.000Z'],
      ['03:00', '2027-03-28T01:00:00.000Z'],
    ])('turns %s into %s', (time, instant) => {
      expect(madridToInstant('2027-03-28', time)).toBe(instant);
    });

    it('takes 02:30, which does not exist, as 03:30', () => {
      expect(madridToInstant('2027-03-28', '02:30')).toBe('2027-03-28T01:30:00.000Z');
    });
  });

  describe('on the last Sunday of October, when the clock goes back at 03:00', () => {
    it.each([
      ['01:59', '2026-10-24T23:59:00.000Z'],
      ['03:00', '2026-10-25T02:00:00.000Z'],
    ])('turns %s into %s', (time, instant) => {
      expect(madridToInstant('2026-10-25', time)).toBe(instant);
    });

    it('takes 02:30, which happens twice, as the first one', () => {
      expect(madridToInstant('2026-10-25', '02:30')).toBe('2026-10-25T00:30:00.000Z');
    });
  });

  it.each([
    ['2026-11-2', '11:00'],
    ['2026-11-02', '11'],
    ['', ''],
  ])('rejects %j %j, which no form input gives', (date, time) => {
    expect(() => madridToInstant(date, time)).toThrow();
  });
});
