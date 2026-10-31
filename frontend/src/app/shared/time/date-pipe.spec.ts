import { render, screen } from '@testing-library/angular';

import { BfDatePipe } from './date-pipe';

describe('BfDatePipe', () => {
  it('writes an instant in Madrid time, as a date and time by default', async () => {
    await render(
      `<p>{{ '2026-11-02T10:00:00Z' | bfDate }}</p><p>{{ '2026-11-02T10:00:00Z' | bfDate: 'time' }}</p>`,
      {
        imports: [BfDatePipe],
      },
    );

    expect(screen.getByText('lunes, 2 de noviembre, 11:00')).toBeInTheDocument();
    expect(screen.getByText('11:00')).toBeInTheDocument();
  });

  it('writes nothing for a missing instant', () => {
    const pipe = new BfDatePipe();

    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined, 'time')).toBe('');
  });
});
