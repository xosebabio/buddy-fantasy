import { render, screen } from '@testing-library/angular';

import { Badge } from './badge';

describe('Badge', () => {
  it('is neutral by default', async () => {
    await render('<bf-badge>Manager</bf-badge>', { imports: [Badge] });

    expect(screen.getByText('Manager')).toHaveClass('neutral');
  });

  it('takes its tone', async () => {
    await render('<bf-badge tone="accent">Admin</bf-badge>', { imports: [Badge] });

    expect(screen.getByText('Admin')).toHaveClass('accent');
  });
});
