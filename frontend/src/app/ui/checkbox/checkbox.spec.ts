import { signal } from '@angular/core';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Checkbox } from './checkbox';

describe('Checkbox', () => {
  async function renderCheckbox(disabled = false) {
    const played = signal(false);
    await render(
      `<label>
        <input type="checkbox" bfCheckbox [checked]="played()" [disabled]="disabled"
          (change)="played.set(!played())" />
        Jugó
      </label>`,
      { imports: [Checkbox], componentProperties: { played, disabled } },
    );
    return played;
  }

  it('is a native checkbox named by its label', async () => {
    await renderCheckbox();

    const checkbox = screen.getByRole('checkbox', { name: 'Jugó' });
    expect(checkbox).toHaveClass('bf-checkbox');
    expect(checkbox).not.toBeChecked();
  });

  it('is checked by clicking the label or with the space bar', async () => {
    const played = await renderCheckbox();

    await userEvent.click(screen.getByText('Jugó'));
    expect(played()).toBe(true);
    expect(screen.getByRole('checkbox')).toBeChecked();

    await userEvent.keyboard(' ');
    expect(played()).toBe(false);
  });

  it('can be disabled', async () => {
    const played = await renderCheckbox(true);

    await userEvent.click(screen.getByText('Jugó'));

    expect(screen.getByRole('checkbox')).toBeDisabled();
    expect(played()).toBe(false);
  });
});
