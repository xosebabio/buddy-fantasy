import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import type { Position } from '../position/position';
import type { LineupPlayerModel, LineupSpot } from './lineup-spots';
import { Pitch } from './pitch';

const PLAYERS: LineupPlayerModel[] = [
  { id: 'diego', name: 'Diego', position: 'GOALKEEPER', shirtNumber: 1, value: 16.4 },
  { id: 'pablo', name: 'Pablo', position: 'FORWARD', shirtNumber: 9, value: 17.2 },
];

describe('Pitch', () => {
  async function renderPitch(
    options: {
      freeSpots?: (Position | null)[];
      selected?: string | null;
      players?: LineupPlayerModel[];
    } = {},
  ) {
    const spotSelected = vi.fn<(spot: LineupSpot) => void>();
    const { container } = await render(Pitch, {
      inputs: {
        label: 'Tu alineación',
        players: options.players ?? PLAYERS,
        freeSpots: options.freeSpots ?? [],
        selected: options.selected ?? null,
      },
      on: { spotSelected },
    });
    return { spotSelected, container };
  }

  it('is a group of buttons named with the name, the position and the value', async () => {
    await renderPitch();

    const pitch = screen.getByRole('group', { name: 'Tu alineación' });
    expect(
      within(pitch).getByRole('button', { name: 'Pablo, Delantero, 17,2 M' }),
    ).toHaveTextContent('9Pablo17,2 M');
    expect(
      within(pitch).getByRole('button', { name: 'Diego, Portero, 16,4 M' }),
    ).toBeInTheDocument();
  });

  it('puts the forwards at the top and the goalkeeper at the bottom', async () => {
    await renderPitch();

    const names = screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'));
    expect(names).toEqual(['Pablo, Delantero, 17,2 M', 'Diego, Portero, 16,4 M']);
  });

  it('emits the spot touched, a player or a free place', async () => {
    const { spotSelected } = await renderPitch({ freeSpots: [null, 'DEFENDER'] });

    await userEvent.click(screen.getByRole('button', { name: /^Pablo/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Plaza libre, Defensa' }));
    await userEvent.click(screen.getByRole('button', { name: 'Plaza libre' }));

    expect(spotSelected.mock.calls.map(([spot]) => spot)).toEqual([
      { kind: 'player', player: PLAYERS[1] },
      { kind: 'free', index: 1, position: 'DEFENDER' },
      { kind: 'free', index: 0, position: null },
    ]);
  });

  it('puts the free places without a position at the foot', async () => {
    const { container } = await renderPitch({ freeSpots: [null] });

    const foot = container.querySelector<HTMLElement>('.foot');
    expect(foot).not.toBeNull();
    expect(
      within(foot ?? document.body).getByRole('button', { name: 'Plaza libre' }),
    ).toBeInTheDocument();
  });

  it('rings the selected spot', async () => {
    await renderPitch({ selected: 'pablo' });

    expect(screen.getByRole('button', { name: /^Pablo/ })).toHaveClass('selected');
    expect(screen.getByRole('button', { name: /^Diego/ })).not.toHaveClass('selected');
  });

  it('narrows the spots when a row has five', async () => {
    const defenders = ['1', '2', '3', '4', '5'].map((id): LineupPlayerModel => ({
      id,
      name: id,
      position: 'DEFENDER',
      shirtNumber: null,
      value: 5,
    }));

    await renderPitch({ players: defenders });

    expect(screen.getByRole('group')).toHaveClass('dense');
  });
});
