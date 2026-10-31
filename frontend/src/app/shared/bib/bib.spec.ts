import { render, screen } from '@testing-library/angular';

import { Bib, EmptyBib } from './bib';

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

  it('has no number for a player without one', async () => {
    const { container } = await render('<bf-bib position="MIDFIELDER" [number]="null" />', {
      imports: [Bib],
    });

    expect(container.querySelector('bf-bib')).toHaveTextContent('');
  });
});

describe('EmptyBib', () => {
  it('is a decorative outline of the given size', async () => {
    const { container } = await render('<bf-empty-bib size="small" />', { imports: [EmptyBib] });

    const bib = container.querySelector('bf-empty-bib');
    expect(bib).toHaveAttribute('aria-hidden', 'true');
    expect(bib).toHaveClass('small');
  });
});
