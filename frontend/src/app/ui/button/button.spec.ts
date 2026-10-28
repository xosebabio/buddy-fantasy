import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Button } from './button';

describe('Button', () => {
  it('is a button with its text as name and the secondary look by default', async () => {
    await render('<button bfButton type="button" class="mine">Crear liga</button>', {
      imports: [Button],
    });

    const button = screen.getByRole('button', { name: 'Crear liga' });
    expect(button).toHaveClass('secondary', 'mine');
    expect(button).toBeEnabled();
  });

  it('takes the variant and the full width', async () => {
    await render('<button bfButton variant="primary" block>Guardar</button>', {
      imports: [Button],
    });

    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveClass('primary', 'block');
  });

  it('can be a link', async () => {
    await render('<a bfButton href="/leagues/new">Crear liga</a>', { imports: [Button] });

    expect(screen.getByRole('link', { name: 'Crear liga' })).toHaveAttribute(
      'href',
      '/leagues/new',
    );
  });

  it('does not click when disabled', async () => {
    const clicked = vi.fn();
    await render('<button bfButton [disabled]="true" (click)="clicked()">Guardar</button>', {
      imports: [Button],
      componentProperties: { clicked },
    });

    const button = screen.getByRole('button', { name: 'Guardar' });
    await userEvent.click(button);

    expect(button).toBeDisabled();
    expect(clicked).not.toHaveBeenCalled();
  });

  it('is busy and disabled while loading, with the same name', async () => {
    await render('<button bfButton loading>Guardar</button>', { imports: [Button] });

    const button = screen.getByRole('button', { name: 'Guardar' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
  });

  it('goes back to normal when it stops loading', async () => {
    const { rerender } = await render('<button bfButton [loading]="loading">Guardar</button>', {
      imports: [Button],
      componentProperties: { loading: true },
    });

    await rerender({ componentProperties: { loading: false } });

    const button = screen.getByRole('button', { name: 'Guardar' });
    expect(button).toBeEnabled();
    expect(button).not.toHaveAttribute('aria-busy');
  });
});
