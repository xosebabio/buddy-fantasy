import { render, screen } from '@testing-library/angular';

import { BfPointsPipe, formatPoints } from './points';

describe('formatPoints', () => {
  it('writes the sign, with the typographic minus, and no sign for zero', () => {
    expect(formatPoints(14)).toBe('+14');
    expect(formatPoints(-3)).toBe('−3');
    expect(formatPoints(0)).toBe('0');
  });

  it('can leave the plus out, as the totals of the standings', () => {
    expect(formatPoints(342, false)).toBe('342');
    expect(formatPoints(-2, false)).toBe('−2');
  });
});

describe('BfPointsPipe', () => {
  it('writes points in a template', async () => {
    await render(`<p>{{ 38 | bfPoints }}</p><p>{{ -5 | bfPoints }}</p>`, {
      imports: [BfPointsPipe],
    });

    expect(screen.getByText('+38')).toBeInTheDocument();
    expect(screen.getByText('−5')).toBeInTheDocument();
  });
});
