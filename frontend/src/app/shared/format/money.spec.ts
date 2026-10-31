import { render, screen } from '@testing-library/angular';

import { BfMoneyPipe, formatMoney } from './money';

describe('formatMoney', () => {
  it('writes millions with one or two decimals and the unit', () => {
    expect(formatMoney(17.2)).toBe('17,2 M');
    expect(formatMoney(100)).toBe('100,0 M');
    expect(formatMoney(12.25)).toBe('12,25 M');
    expect(formatMoney(12.255)).toBe('12,26 M');
  });

  it('leaves the unit out for a figure', () => {
    expect(formatMoney(87.4, 'number')).toBe('87,4');
  });
});

describe('BfMoneyPipe', () => {
  it('writes an amount in a template', async () => {
    await render(`<p>{{ 21 | bfMoney }}</p><p>{{ 96.2 | bfMoney: 'number' }}</p>`, {
      imports: [BfMoneyPipe],
    });

    expect(screen.getByText('21,0 M')).toBeInTheDocument();
    expect(screen.getByText('96,2')).toBeInTheDocument();
  });
});
