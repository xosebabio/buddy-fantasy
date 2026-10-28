import { render } from '@testing-library/angular';

import { Skeleton } from './skeleton';

describe('Skeleton', () => {
  it('is a line hidden from screen readers by default', async () => {
    const { container } = await render('<bf-skeleton />', { imports: [Skeleton] });

    const skeleton = container.querySelector('bf-skeleton');
    expect(skeleton).toHaveAttribute('aria-hidden', 'true');
    expect(skeleton).toHaveClass('line');
  });

  it('takes its shape and width', async () => {
    const { container } = await render('<bf-skeleton shape="title" width="40%" />', {
      imports: [Skeleton],
    });

    const skeleton = container.querySelector('bf-skeleton');
    expect(skeleton).toHaveClass('title');
    expect(skeleton).toHaveStyle({ width: '40%' });
  });
});
