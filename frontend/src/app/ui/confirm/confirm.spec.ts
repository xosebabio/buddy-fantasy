import { TestBed } from '@angular/core/testing';
import { screen, waitFor } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Confirm } from './confirm';

describe('Confirm', () => {
  function ask(tone?: 'danger') {
    const answer = vi.fn();
    TestBed.inject(Confirm)
      .ask({
        title: '¿Expulsar a Brais?',
        message: 'Dejará de ver la liga y perderá su alineación.',
        confirm: 'Expulsar',
        tone,
      })
      .subscribe(answer);
    return answer;
  }

  it('asks in an alert dialog named by the title and described by the message', async () => {
    ask('danger');

    const dialog = await screen.findByRole('alertdialog', { name: '¿Expulsar a Brais?' });
    expect(dialog).toHaveAccessibleDescription('Dejará de ver la liga y perderá su alineación.');
    expect(screen.getByRole('button', { name: 'Expulsar' })).toHaveClass('danger');
  });

  it('starts with the focus on cancel', async () => {
    ask();

    const cancel = await screen.findByRole('button', { name: 'Cancelar' });
    await waitFor(() => {
      expect(cancel).toHaveFocus();
    });
    expect(screen.getByRole('button', { name: 'Expulsar' })).toHaveClass('primary');
  });

  it('answers true when confirmed', async () => {
    const answer = ask();

    await userEvent.click(await screen.findByRole('button', { name: 'Expulsar' }));

    expect(answer).toHaveBeenCalledExactlyOnceWith(true);
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('answers false when cancelled', async () => {
    const answer = ask();

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));

    expect(answer).toHaveBeenCalledExactlyOnceWith(false);
  });

  it('has no description without a message', async () => {
    TestBed.inject(Confirm).ask({ title: '¿Salir de la liga?', confirm: 'Salir' }).subscribe();

    const dialog = await screen.findByRole('alertdialog', { name: '¿Salir de la liga?' });
    expect(dialog).not.toHaveAttribute('aria-describedby');
  });
});
