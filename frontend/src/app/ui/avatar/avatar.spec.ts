import { fireEvent, render, screen } from '@testing-library/angular';

import { Avatar, initials } from './avatar';

describe('initials', () => {
  it.each([
    ['Xosé Babío', 'XB'],
    ['  maría  del   carmen  ', 'MC'],
    ['Iago', 'I'],
    ['Óscar Martínez Pérez', 'ÓP'],
    ['', ''],
  ])('of «%s» is «%s»', (name, expected) => {
    expect(initials(name)).toBe(expected);
  });
});

describe('Avatar', () => {
  it('shows the initials, hidden from screen readers', async () => {
    const { container } = await render('<bf-avatar name="Xosé Babío" />', { imports: [Avatar] });

    const avatar = container.querySelector('bf-avatar');
    expect(avatar).toHaveTextContent('XB');
    expect(avatar).toHaveAttribute('aria-hidden', 'true');
    expect(avatar).toHaveClass('small');
  });

  it('shows the picture without sending the referrer', async () => {
    const { container } = await render(
      '<bf-avatar name="Xosé Babío" src="https://example.com/x.png" size="large" />',
      { imports: [Avatar] },
    );

    const img = container.querySelector('img');
    expect(img).toHaveAttribute('src', 'https://example.com/x.png');
    expect(img).toHaveAttribute('alt', '');
    expect(img).toHaveAttribute('referrerpolicy', 'no-referrer');
    expect(container.querySelector('bf-avatar')).not.toHaveTextContent('XB');
  });

  it('falls back to the initials when the picture does not load, and retries a new one', async () => {
    const { container, rerender } = await render('<bf-avatar name="Xosé Babío" [src]="src" />', {
      imports: [Avatar],
      componentProperties: { src: 'https://example.com/broken.png' },
    });

    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    if (img !== null) {
      fireEvent.error(img);
    }
    expect(await screen.findByText('XB')).toBeInTheDocument();

    await rerender({ componentProperties: { src: 'https://example.com/new.png' } });

    expect(container.querySelector('img')).toHaveAttribute('src', 'https://example.com/new.png');
  });
});
