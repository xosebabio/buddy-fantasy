import { render, screen } from '@testing-library/angular';

import { Card } from './card';

describe('Card', () => {
  it('shows its content', async () => {
    await render('<bf-card><h2>Os Domingueiros</h2></bf-card>', { imports: [Card] });

    expect(screen.getByRole('heading', { name: 'Os Domingueiros' })).toBeInTheDocument();
  });
});
