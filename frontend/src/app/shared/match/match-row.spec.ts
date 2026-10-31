import { render, screen } from '@testing-library/angular';

import { CLOSED_SUMMARY, SCHEDULED_SUMMARY } from '../roster/matches.testing';
import { MatchRow } from './match-row';

describe('MatchRow', () => {
  it('shows the matchday, the opponent, the venue and the date', async () => {
    const { container } = await render(MatchRow, { inputs: { match: SCHEDULED_SUMMARY } });

    expect(container).toHaveTextContent('Jornada 12, contra CD Arrabal');
    expect(screen.getByText('12')).toHaveAttribute('aria-hidden', 'true');
    expect(container).toHaveTextContent('Fuera, ');
    expect(container).not.toHaveTextContent('–');
  });

  it('shows the result at the end, with the state under it', async () => {
    await render(
      `<bf-match-row [match]="match"><span bfMatchRowTrailing>Cerrado</span></bf-match-row>`,
      { imports: [MatchRow], componentProperties: { match: CLOSED_SUMMARY } },
    );

    expect(screen.getByText('3–1')).toBeInTheDocument();
    expect(screen.getByText('Cerrado')).toBeInTheDocument();
  });
});
