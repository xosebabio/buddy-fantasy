import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { LineupList } from './lineup-list';
import type { LineupPlayerModel, LineupSpot } from './lineup-spots';

const PLAYERS: LineupPlayerModel[] = [
  { id: 'pablo', name: 'Pablo', position: 'FORWARD', shirtNumber: 9, value: 17.2 },
  { id: 'diego', name: 'Diego', position: 'GOALKEEPER', shirtNumber: 1, value: 16.4 },
];

describe('LineupList', () => {
  async function renderList(selected: string | null = null) {
    const spotSelected = vi.fn<(spot: LineupSpot) => void>();
    await render(LineupList, {
      inputs: { label: 'Tu alineación', players: PLAYERS, freeSpots: ['DEFENDER'], selected },
      on: { spotSelected },
    });
    return spotSelected;
  }

  it('lists the players from the goalkeeper forwards, with their position and value', async () => {
    await renderList();

    const rows = screen
      .getAllByRole('button')
      .map((row) => row.textContent.replace(/\s+/g, ' ').trim());
    expect(screen.getByRole('list', { name: 'Tu alineación' })).toBeInTheDocument();
    expect(rows).toEqual(['1DiegoPortero16,4 M', 'Plaza libre Defensa', '9PabloDelantero17,2 M']);
  });

  it('emits the row touched', async () => {
    const spotSelected = await renderList();

    await userEvent.click(screen.getByRole('button', { name: /Plaza libre/ }));

    expect(spotSelected).toHaveBeenCalledWith({ kind: 'free', index: 0, position: 'DEFENDER' });
  });

  it('marks the selected row', async () => {
    await renderList('diego');

    expect(screen.getByRole('button', { name: /Diego/ }).closest('li')).toHaveClass('selected');
  });
});
