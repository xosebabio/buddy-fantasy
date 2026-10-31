import { signal } from '@angular/core';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Stepper } from './stepper';

describe('Stepper', () => {
  async function renderStepper(initial: number | null, options: { disabled?: boolean } = {}) {
    const value = signal(initial);
    await render(
      `<bf-stepper label="Nota de Diego" [min]="0" [max]="10" [start]="5" empty="Sin voto"
        [(value)]="value" [disabled]="disabled" />`,
      {
        imports: [Stepper],
        componentProperties: { value, disabled: options.disabled ?? false },
      },
    );
    return value;
  }

  it('is a spinbutton named by its label, with its value and range', async () => {
    await renderStepper(7);

    const spinbutton = screen.getByRole('spinbutton', { name: 'Nota de Diego' });
    expect(spinbutton).toHaveTextContent('7');
    expect(spinbutton).toHaveAttribute('aria-valuenow', '7');
    expect(spinbutton).toHaveAttribute('aria-valuemin', '0');
    expect(spinbutton).toHaveAttribute('aria-valuemax', '10');
    expect(spinbutton).not.toHaveAttribute('aria-valuetext');
  });

  it('shows the empty text without a value, and the first step goes to start', async () => {
    const value = await renderStepper(null);

    const spinbutton = screen.getByRole('spinbutton', { name: 'Nota de Diego' });
    expect(spinbutton).toHaveTextContent('Sin voto');
    expect(spinbutton).toHaveAttribute('aria-valuetext', 'Sin voto');
    expect(spinbutton).not.toHaveAttribute('aria-valuenow');

    await userEvent.click(screen.getByRole('button', { name: 'Bajar' }));

    expect(value()).toBe(5);
    expect(spinbutton).toHaveTextContent('5');
  });

  it('steps with the buttons and stops at the ends', async () => {
    const value = await renderStepper(9);
    const up = screen.getByRole('button', { name: 'Subir' });

    await userEvent.click(up);
    expect(value()).toBe(10);
    expect(up).toBeDisabled();

    await userEvent.click(screen.getByRole('button', { name: 'Bajar' }));
    expect(value()).toBe(9);
    expect(up).toBeEnabled();
  });

  it('keeps the buttons out of the tab order', async () => {
    await renderStepper(3);

    await userEvent.tab();

    expect(screen.getByRole('spinbutton')).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Subir' })).toHaveAttribute('tabindex', '-1');
  });

  it('changes with the arrow keys, Home and End', async () => {
    const value = await renderStepper(3);
    await userEvent.tab();

    await userEvent.keyboard('{ArrowUp}');
    expect(value()).toBe(4);
    await userEvent.keyboard('{ArrowDown}{ArrowDown}');
    expect(value()).toBe(2);
    await userEvent.keyboard('{End}');
    expect(value()).toBe(10);
    await userEvent.keyboard('{Home}');
    expect(value()).toBe(0);
    await userEvent.keyboard('{ArrowDown}');
    expect(value()).toBe(0);
  });

  it('can be disabled', async () => {
    const value = await renderStepper(3, { disabled: true });

    expect(screen.getByRole('button', { name: 'Subir' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Bajar' })).toBeDisabled();
    const spinbutton = screen.getByRole('spinbutton');
    expect(spinbutton).toHaveAttribute('aria-disabled', 'true');
    expect(spinbutton).toHaveAttribute('tabindex', '-1');

    spinbutton.focus();
    await userEvent.keyboard('{ArrowUp}');
    expect(value()).toBe(3);
  });
});
