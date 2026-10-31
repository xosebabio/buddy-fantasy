import { render, screen } from '@testing-library/angular';

import { PlayerRow } from './player-row';

const PABLO = { name: 'Pablo', position: 'FORWARD', shirtNumber: 9 } as const;

describe('PlayerRow', () => {
  it('shows the bib, the name and the position in text', async () => {
    await render(PlayerRow, { inputs: { player: PABLO } });

    expect(screen.getByText('9')).toHaveClass('forward', 'small');
    expect(screen.getByText('Pablo')).toBeInTheDocument();
    expect(screen.getByText('Delantero')).toBeInTheDocument();
    expect(screen.queryByText(/ M$/)).not.toBeInTheDocument();
  });

  it('adds the value in millions', async () => {
    await render(PlayerRow, { inputs: { player: PABLO, value: 17.2 } });

    expect(screen.getByText('17,2 M')).toBeInTheDocument();
  });

  it('can leave the position out and take more details and a trailing state', async () => {
    await render(
      `<bf-player-row [player]="player" showPosition="false">
        <span bfPlayerRowMeta>1 gol</span>
        <span bfPlayerRowTrailing>En la alineación</span>
      </bf-player-row>`,
      { imports: [PlayerRow], componentProperties: { player: PABLO } },
    );

    expect(screen.queryByText('Delantero')).not.toBeInTheDocument();
    expect(screen.getByText('1 gol')).toBeInTheDocument();
    expect(screen.getByText('En la alineación')).toBeInTheDocument();
  });
});
