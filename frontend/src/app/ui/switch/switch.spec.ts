import { signal } from '@angular/core';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Switch } from './switch';

describe('Switch', () => {
  async function renderSwitch(initial: boolean, disabled = false) {
    const checked = signal(initial);
    await render(
      `<button bfSwitch aria-label="Presupuesto" [(checked)]="checked" [disabled]="disabled"></button>`,
      { imports: [Switch], componentProperties: { checked, disabled } },
    );
    return checked;
  }

  it('is a switch named by the page, with its state', async () => {
    await renderSwitch(false);

    const toggle = screen.getByRole('switch', { name: 'Presupuesto' });
    expect(toggle).toHaveAttribute('aria-checked', 'false');
    expect(toggle).toHaveAttribute('type', 'button');
  });

  it('toggles with a click and with the keyboard', async () => {
    const checked = await renderSwitch(false);
    const toggle = screen.getByRole('switch');

    await userEvent.click(toggle);
    expect(checked()).toBe(true);
    expect(toggle).toHaveAttribute('aria-checked', 'true');

    await userEvent.keyboard('{Enter}');
    expect(checked()).toBe(false);
    await userEvent.keyboard(' ');
    expect(checked()).toBe(true);
  });

  it('can be disabled', async () => {
    const checked = await renderSwitch(true, true);

    await userEvent.click(screen.getByRole('switch'));

    expect(screen.getByRole('switch')).toBeDisabled();
    expect(checked()).toBe(true);
  });
});
