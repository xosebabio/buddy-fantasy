import { render, screen } from '@testing-library/angular';

import { Points } from './points';

describe('Points', () => {
  it('paints the points that add in green, with their sign', async () => {
    await render(Points, { inputs: { value: 14 } });

    expect(screen.getByText('+14')).toHaveClass('positive', 'list');
  });

  it('paints the points that subtract in red, with the typographic minus', async () => {
    await render(Points, { inputs: { value: -3, size: 'hero' } });

    expect(screen.getByText('−3')).toHaveClass('negative', 'hero');
  });

  it('leaves zero neutral', async () => {
    await render(Points, { inputs: { value: 0 } });

    const points = screen.getByText('0');
    expect(points).not.toHaveClass('positive');
    expect(points).not.toHaveClass('negative');
  });

  it('can be a total without sign or color', async () => {
    await render(Points, { inputs: { value: 342, unsigned: true } });

    expect(screen.getByText('342')).not.toHaveClass('positive');
  });
});
