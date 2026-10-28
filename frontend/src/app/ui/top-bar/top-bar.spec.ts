import { render, screen } from '@testing-library/angular';

import { TopBar } from './top-bar';
import { Wordmark } from './wordmark';

describe('TopBar', () => {
  it('is the banner, with its start and end content', async () => {
    await render(
      `<bf-top-bar>
        <bf-wordmark />
        <a bfTopBarEnd href="/profile">Perfil</a>
      </bf-top-bar>`,
      { imports: [TopBar, Wordmark] },
    );

    const banner = screen.getByRole('banner');
    expect(banner).toHaveTextContent('BuddyFantasy');
    expect(banner.lastElementChild).toHaveTextContent('Perfil');
  });
});
