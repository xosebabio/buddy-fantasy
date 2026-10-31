import { render, screen } from '@testing-library/angular';

import { PositionChip } from './position-chip';

describe('PositionChip', () => {
  it('writes the position on its color', async () => {
    await render(PositionChip, { inputs: { position: 'FORWARD' } });

    expect(screen.getByText('Delantero')).toHaveClass('forward');
  });
});
