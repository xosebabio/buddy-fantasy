import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { LoadError } from './load-error';

describe('LoadError', () => {
  it('announces the error and asks to retry', async () => {
    const retry = vi.fn();
    await render(LoadError, { on: { retry } });

    expect(screen.getByRole('alert')).toHaveTextContent(/No se ha podido cargar/);
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(retry).toHaveBeenCalledOnce();
  });
});
