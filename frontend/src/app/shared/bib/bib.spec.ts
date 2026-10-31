import { render, screen } from '@testing-library/angular';

import { Bib } from './bib';

describe('Bib', () => {
  it('shows the shirt number in the color of the position, hidden from screen readers', async () => {
    await render(Bib, { inputs: { position: 'DEFENDER', number: 4, size: 'large' } });

    const bib = screen.getByText('4');
    expect(bib).toHaveAttribute('aria-hidden', 'true');
    expect(bib).toHaveClass('defender', 'large');
  });

  it('is of the medium size by default', async () => {
    await render(Bib, { inputs: { position: 'GOALKEEPER', number: 1 } });

    expect(screen.getByText('1')).toHaveClass('goalkeeper', 'medium');
  });
});
