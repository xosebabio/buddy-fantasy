import { signal } from '@angular/core';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { RatingFormat } from '../../core/api/rating/rating-api';
import { RatingInput } from './rating-input';

describe('RatingInput', () => {
  async function renderInput(format: RatingFormat, initial: number | null) {
    const value = signal(initial);
    await render(`<bf-rating-input [format]="format" playerName="Diego" [(value)]="value" />`, {
      imports: [RatingInput],
      componentProperties: { format, value },
    });
    return value;
  }

  describe('in spades', () => {
    it('is a group of five radio buttons from 0 to 4, without a vote at first', async () => {
      await renderInput('SPADES', null);

      const group = screen.getByRole('radiogroup', { name: 'Picas para Diego' });
      const radios = screen.getAllByRole('radio');
      expect(group).toContainElement(radios[0] ?? null);
      expect(radios.map((radio) => radio.getAttribute('value'))).toEqual(['0', '1', '2', '3', '4']);
      expect(radios.some((radio) => radio instanceof HTMLInputElement && radio.checked)).toBe(
        false,
      );
      expect(screen.getByText('Sin voto')).toBeInTheDocument();
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('takes a 0 as a vote', async () => {
      const value = await renderInput('SPADES', null);

      await userEvent.click(screen.getByRole('radio', { name: '0' }));

      expect(value()).toBe(0);
      expect(screen.getByRole('radio', { name: '0' })).toBeChecked();
      expect(screen.getByRole('button', { name: 'Quitar voto a Diego' })).toHaveTextContent(
        'Quitar',
      );
    });

    it('takes the vote back to none, keeping the focus on the group', async () => {
      const value = await renderInput('SPADES', 3);
      expect(screen.getByRole('radio', { name: '3' })).toBeChecked();

      await userEvent.click(screen.getByRole('button', { name: 'Quitar voto a Diego' }));

      expect(value()).toBeNull();
      expect(screen.getByRole('radio', { name: '3' })).not.toBeChecked();
      expect(screen.getByRole('radio', { name: '0' })).toHaveFocus();
      expect(screen.getByText('Sin voto')).toBeInTheDocument();
    });
  });

  describe('in marks', () => {
    it('is a stepper from 0 to 10 whose first step goes to 5', async () => {
      const value = await renderInput('DECIMAL', null);

      const spinbutton = screen.getByRole('spinbutton', { name: 'Nota de Diego' });
      expect(spinbutton).toHaveAttribute('aria-valuemax', '10');
      expect(spinbutton).toHaveTextContent('Sin voto');

      await userEvent.click(screen.getByRole('button', { name: 'Subir' }));

      expect(value()).toBe(5);
    });

    it('takes the vote back to none', async () => {
      const value = await renderInput('DECIMAL', 7);

      await userEvent.click(screen.getByRole('button', { name: 'Quitar voto a Diego' }));

      expect(value()).toBeNull();
      expect(screen.getByRole('spinbutton', { name: 'Nota de Diego' })).toHaveFocus();
    });
  });
});
