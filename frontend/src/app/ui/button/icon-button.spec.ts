import { LucideX } from '@lucide/angular';
import { render, screen } from '@testing-library/angular';

import { IconButton } from './icon-button';

describe('IconButton', () => {
  it('takes its name from the label, not from the hidden icon', async () => {
    await render('<button bfIconButton type="button" label="Cerrar"><svg lucideX></svg></button>', {
      imports: [IconButton, LucideX],
    });

    const button = screen.getByRole('button', { name: 'Cerrar' });
    expect(button.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('can be disabled', async () => {
    await render('<button bfIconButton label="Cerrar" disabled><svg lucideX></svg></button>', {
      imports: [IconButton, LucideX],
    });

    expect(screen.getByRole('button', { name: 'Cerrar' })).toBeDisabled();
  });

  it('can be a link', async () => {
    await render('<a bfIconButton href="/profile" label="Perfil"><svg lucideX></svg></a>', {
      imports: [IconButton, LucideX],
    });

    expect(screen.getByRole('link', { name: 'Perfil' })).toHaveAttribute('href', '/profile');
  });
});
