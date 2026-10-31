import { signal } from '@angular/core';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Segmented } from './segmented';

const OPTIONS = [
  { value: 'system', label: 'Automático' },
  { value: 'light', label: 'Claro' },
  { value: 'dark', label: 'Oscuro' },
] as const;

describe('Segmented', () => {
  async function renderSegmented(disabled = false) {
    const value = signal<'system' | 'light' | 'dark'>('system');
    await render(
      `<bf-segmented label="Tema" [options]="options" [(value)]="value" [disabled]="disabled" />`,
      { imports: [Segmented], componentProperties: { options: OPTIONS, value, disabled } },
    );
    return value;
  }

  it('is a group of radio buttons named by its label, with the value checked', async () => {
    await renderSegmented();

    const group = screen.getByRole('group', { name: 'Tema' });
    expect(group).toBeInTheDocument();
    expect(screen.getAllByRole('radio')).toHaveLength(3);
    expect(screen.getByRole('radio', { name: 'Automático' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Oscuro' })).not.toBeChecked();
  });

  it('changes the value with a click or the arrow keys', async () => {
    const value = await renderSegmented();

    await userEvent.click(screen.getByText('Oscuro'));
    expect(value()).toBe('dark');
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked();

    await userEvent.keyboard('{ArrowLeft}');
    expect(value()).toBe('light');
  });

  it('can be disabled', async () => {
    const value = await renderSegmented(true);

    for (const radio of screen.getAllByRole('radio')) {
      expect(radio).toBeDisabled();
    }
    await userEvent.click(screen.getByText('Claro'));
    expect(value()).toBe('system');
  });
});
