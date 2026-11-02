import { TZDate, tzOffset } from '@date-fns/tz';

/** The time zone of the league: the interface shows and asks for dates in it (RNF-05). */
const TIME_ZONE = 'Europe/Madrid';
/** The locale of dates, numbers and amounts (RNF-15). */
export const LOCALE = 'es-ES';

const MINUTE_MS = 60_000;
const DAY_MS = 24 * 60 * MINUTE_MS;

/**
 * `dateTime` «domingo, 2 de noviembre, 11:00», `date` «domingo, 2 de noviembre», `day`
 * «2 de noviembre de 2026» (a day further away, such as when someone joined), `short`
 * «2 nov, 11:00» and `time` «11:00».
 */
export type DateFormat = 'dateTime' | 'date' | 'day' | 'short' | 'time';

const FORMATS: Record<DateFormat, Intl.DateTimeFormat> = {
  dateTime: format({
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }),
  date: format({ weekday: 'long', day: 'numeric', month: 'long' }),
  day: format({ day: 'numeric', month: 'long', year: 'numeric' }),
  short: format({ day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }),
  time: format({ hour: '2-digit', minute: '2-digit' }),
};

function format(options: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
  return new Intl.DateTimeFormat(LOCALE, { ...options, timeZone: TIME_ZONE });
}

/** An instant of the API (ISO 8601 in UTC), as the time in Madrid (RNF-05, RNF-15). */
export function formatInstant(instant: string | Date, dateFormat: DateFormat): string {
  return FORMATS[dateFormat].format(new Date(instant));
}

const DATE_TIME = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})$/;

/** The offset of Madrid from UTC at an instant, in milliseconds: an hour in winter, two in summer. */
function madridOffset(instant: number): number {
  return tzOffset(TIME_ZONE, new Date(instant)) * MINUTE_MS;
}

/**
 * The instant, in ISO 8601 and UTC as the API takes it, of a day and a time in Madrid as an
 * `<input type="date">` and an `<input type="time">` give them (`2026-11-02`, `11:00`). A time that
 * does not exist because the clock goes forward is taken an hour later (02:30 is 03:30), and one
 * that happens twice because it goes back, the first time.
 */
export function madridToInstant(date: string, time: string): string {
  const parts = DATE_TIME.exec(`${date} ${time}`);
  if (parts === null) {
    throw new Error(`Not a date and a time of the form inputs: ${date} ${time}`);
  }
  // The pattern has all five groups; the defaults are only for the compiler.
  const [, year = 0, month = 1, day = 1, hours = 0, minutes = 0] = parts.map(Number);
  // The wall clock read as UTC, minus each offset that Madrid has around that day: an offset is right
  // when the instant it gives has that same offset. Two right ones mean the time happens twice, and the
  // earlier instant is the first. None means the clock skipped it, and the offset from before applies.
  // The host time zone plays no part, so the result is the same on any machine.
  const wall = Date.UTC(year, month - 1, day, hours, minutes);
  const before = madridOffset(wall - DAY_MS);
  const after = madridOffset(wall + DAY_MS);
  const instants = [...new Set([before, after])]
    .map((offset) => ({ offset, instant: wall - offset }))
    .filter(({ offset, instant }) => madridOffset(instant) === offset)
    .map(({ instant }) => instant);
  const instant = instants.length > 0 ? Math.min(...instants) : wall - before;
  return new Date(instant).toISOString();
}

/** The day and the time in Madrid of an instant of the API, as the form inputs take them (`2026-11-02`, `11:00`). */
export function instantToMadrid(instant: string): { date: string; time: string } {
  const madrid = new TZDate(new Date(instant).getTime(), TIME_ZONE);
  const two = (value: number): string => String(value).padStart(2, '0');
  return {
    date: `${String(madrid.getFullYear())}-${two(madrid.getMonth() + 1)}-${two(madrid.getDate())}`,
    time: `${two(madrid.getHours())}:${two(madrid.getMinutes())}`,
  };
}
