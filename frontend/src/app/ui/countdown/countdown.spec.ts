import { render, screen } from '@testing-library/angular';

import { Countdown } from './countdown';

const NOW = Date.parse('2026-11-01T10:00:00Z');
const MINUTE = 60_000;
const HOUR = 60 * MINUTE;

describe('Countdown', () => {
  beforeEach(() => {
    vi.useFakeTimers({ now: NOW });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  async function renderCountdown(left: number) {
    const elapsed = vi.fn();
    const until = new Date(NOW + left).toISOString();
    const view = await render(
      '<bf-countdown [until]="until" (elapsed)="elapsed()">Se bloquea en</bf-countdown>',
      { imports: [Countdown], componentProperties: { until, elapsed } },
    );
    const advance = async (ms: number) => {
      await vi.advanceTimersByTimeAsync(ms);
      view.detectChanges();
    };
    return { ...view, elapsed, until, advance };
  }

  it.each([
    [28 * HOUR + 5 * MINUTE, '1 d 4 h'],
    [24 * HOUR + 30 * MINUTE, '1 d'],
    [3 * HOUR + 20 * MINUTE + 59_000, '3 h 20 min'],
    [5 * HOUR, '5 h'],
    [12 * MINUTE + 30_000, '12 min'],
    [59_000, 'menos de 1 min'],
  ])('with %d ms left, shows «%s»', async (left, text) => {
    await renderCountdown(left);

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it('says the instant in its time element, after the content', async () => {
    const { container, until } = await renderCountdown(5 * HOUR);

    expect(container).toHaveTextContent('Se bloquea en 5 h');
    expect(screen.getByText('5 h')).toHaveAttribute('datetime', until);
  });

  it('turns amber below 3 hours', async () => {
    const { container, advance } = await renderCountdown(3 * HOUR + MINUTE);
    const countdown = container.querySelector('bf-countdown');
    expect(countdown).not.toHaveClass('warning');

    await advance(1);
    expect(screen.getByText('3 h')).toBeInTheDocument();
    await advance(MINUTE);

    expect(screen.getByText('2 h 59 min')).toBeInTheDocument();
    expect(countdown).toHaveClass('warning');
  });

  it('refreshes every minute and every second in the last one, and emits once at zero', async () => {
    const { elapsed, advance } = await renderCountdown(2 * MINUTE + 30_000);
    expect(screen.getByText('2 min')).toBeInTheDocument();

    await advance(30_000);
    expect(screen.getByText('2 min')).toBeInTheDocument();
    await advance(1);
    expect(screen.getByText('1 min')).toBeInTheDocument();

    await advance(MINUTE);
    expect(screen.getByText('menos de 1 min')).toBeInTheDocument();
    expect(elapsed).not.toHaveBeenCalled();

    await advance(MINUTE);
    expect(elapsed).toHaveBeenCalledOnce();

    await advance(HOUR);
    expect(elapsed).toHaveBeenCalledOnce();
  });

  it('does not emit for an instant that had already passed', async () => {
    const { elapsed, advance } = await renderCountdown(-MINUTE);

    await advance(MINUTE);

    expect(screen.getByText('menos de 1 min')).toBeInTheDocument();
    expect(elapsed).not.toHaveBeenCalled();
  });

  it('counts again towards a new instant', async () => {
    const { rerender, advance } = await renderCountdown(HOUR);

    await rerender({ componentProperties: { until: new Date(NOW + 2 * HOUR).toISOString() } });
    await advance(0);

    expect(screen.getByText('2 h')).toBeInTheDocument();
  });
});
