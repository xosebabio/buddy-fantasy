import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  linkedSignal,
  output,
} from '@angular/core';
import { LucideClock } from '@lucide/angular';

const SECOND = 1000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/**
 * Below this, the countdown turns amber (decision 11 of plan 12). It is presentation, not a business
 * rule: no validation uses it.
 */
const WARNING_BELOW = 3 * HOUR;

/**
 * The time left until an instant of the API (FE-18), such as _Se bloquea en 1 d 4 h_: the clock
 * icon, the content (the words before the time) and the time in a `<time>` with the instant. It is
 * a guide with the browser's clock (FE-09): the server has the last word. It knows no time zones;
 * the absolute date, where needed, is the page's (`bfDate`).
 *
 * It refreshes every minute, and every second in the last one; below 3 h it turns amber. When it
 * reaches zero it emits `elapsed` once, so that the page reloads; an instant that had already
 * passed when it arrived does not count.
 */
@Component({
  selector: 'bf-countdown',
  imports: [LucideClock],
  template: `
    <svg lucideClock [size]="16"></svg>
    <span
      ><ng-content />&ngsp;<time [attr.datetime]="until()">{{ text() }}</time></span
    >
  `,
  styleUrl: './countdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.warning]': 'warning()' },
})
export class Countdown {
  /** An instant in ISO 8601, as the API gives them. */
  readonly until = input.required<string>();
  readonly elapsed = output();

  /** The time of the last tick, taken again whenever `until` changes. */
  private readonly now = linkedSignal({ source: this.until, computation: () => Date.now() });
  private readonly remaining = computed(() => Date.parse(this.until()) - this.now());
  protected readonly warning = computed(() => this.remaining() < WARNING_BELOW);
  protected readonly text = computed(() => remainingText(this.remaining()));

  constructor() {
    let counting: string | null = null;
    effect((onCleanup) => {
      const remaining = this.remaining();
      const until = this.until();
      if (remaining > 0) {
        counting = until;
        const timer = setTimeout(() => {
          this.now.set(Date.now());
        }, nextTick(remaining));
        onCleanup(() => {
          clearTimeout(timer);
        });
      } else if (counting === until) {
        counting = null;
        this.elapsed.emit();
      }
    });
  }
}

/**
 * When the text changes: just after the next whole minute, since the time is rounded down, or in a
 * second within the last minute.
 */
function nextTick(remaining: number): number {
  return remaining >= MINUTE ? (remaining % MINUTE) + 1 : Math.min(SECOND, remaining);
}

/** _1 d 4 h_, _3 h 20 min_, _12 min_ or _menos de 1 min_, rounded down. */
function remainingText(remaining: number): string {
  if (remaining < MINUTE) {
    return $localize`:@@ui.countdown.underAMinute:menos de 1 min`;
  }
  const days = String(Math.floor(remaining / DAY));
  const hours = String(Math.floor((remaining % DAY) / HOUR));
  const minutes = String(Math.floor((remaining % HOUR) / MINUTE));
  if (remaining >= DAY) {
    return hours === '0'
      ? $localize`:@@ui.countdown.days:${days}:days: d`
      : $localize`:@@ui.countdown.daysHours:${days}:days: d ${hours}:hours: h`;
  }
  if (remaining >= HOUR) {
    return minutes === '0'
      ? $localize`:@@ui.countdown.hours:${hours}:hours: h`
      : $localize`:@@ui.countdown.hoursMinutes:${hours}:hours: h ${minutes}:minutes: min`;
  }
  return $localize`:@@ui.countdown.minutes:${minutes}:minutes: min`;
}
