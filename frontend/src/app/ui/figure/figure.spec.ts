import { render, screen } from '@testing-library/angular';

import { Figure } from './figure';

describe('Figure', () => {
  it('shows the figure with its unit and its label below', async () => {
    const { container } = await render(
      '<bf-figure label="Presupuesto">96,2 <span bfFigureUnit>/ 100 M</span></bf-figure>',
      { imports: [Figure] },
    );

    const figure = container.querySelector('bf-figure');
    expect(figure).toHaveTextContent('96,2 / 100 M Presupuesto');
    expect(figure).toHaveClass('medium');
    expect(figure).not.toHaveClass('danger');
    expect(screen.getByText('/ 100 M').closest('.unit')).toBeInTheDocument();
  });

  it('takes its size and the danger tone', async () => {
    const { container } = await render(
      '<bf-figure label="Presupuesto" size="large" tone="danger">101,5</bf-figure>',
      { imports: [Figure] },
    );

    expect(container.querySelector('bf-figure')).toHaveClass('large', 'danger');
    expect(container.querySelector('.unit')).toBeEmptyDOMElement();
  });
});
