import { render, screen } from '@testing-library/angular';

import { PointsBreakdown } from './points-breakdown';

describe('PointsBreakdown', () => {
  it('lists each action that scored, the rating and the total', async () => {
    await render(PointsBreakdown, {
      inputs: { breakdown: { actions: { ASSIST: 1, GOAL: 3 }, ratingPoints: 10, points: 14 } },
    });

    const terms = screen.getAllByRole('term').map((term) => term.textContent);
    const details = screen.getAllByRole('definition').map((detail) => detail.textContent);
    expect(terms).toEqual(['Gol', 'Asistencia', 'Valoración', 'Total']);
    expect(details).toEqual(['+3', '+1', '+10', '+14']);
  });

  it('subtracts an action and shows a rating of zero', async () => {
    await render(PointsBreakdown, {
      inputs: { breakdown: { actions: { YELLOW_CARD: -1 }, ratingPoints: 0, points: -1 } },
    });

    expect(screen.getAllByRole('definition').map((detail) => detail.textContent)).toEqual([
      '−1',
      '0',
      '−1',
    ]);
  });
});
