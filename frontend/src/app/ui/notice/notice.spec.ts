import { LucideBan } from '@lucide/angular';
import { render, screen } from '@testing-library/angular';

import { Notice } from './notice';

describe('Notice', () => {
  it('is an informative notice by default, with its icon hidden from screen readers', async () => {
    await render('<bf-notice><p>Calculando los resultados.</p></bf-notice>', {
      imports: [Notice],
    });

    const notice = screen.getByText('Calculando los resultados.').closest('bf-notice');
    expect(notice).toHaveClass('info');
    expect(notice).not.toHaveAttribute('role');
    expect(notice?.querySelectorAll('svg')).toHaveLength(1);
    expect(notice?.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it.each(['warning', 'danger'] as const)('takes the %s tone and its icon', async (tone) => {
    await render(`<bf-notice tone="${tone}">Te pasas 1,5 M del presupuesto.</bf-notice>`, {
      imports: [Notice],
    });

    const notice = screen.getByText('Te pasas 1,5 M del presupuesto.').closest('bf-notice');
    expect(notice).toHaveClass(tone);
    expect(notice?.querySelectorAll('svg')).toHaveLength(1);
  });

  it('shows the icon it is given instead of the tone one', async () => {
    await render(
      `<bf-notice>
        <svg lucideBan class="given"></svg>
        El administrador ha cancelado tus valoraciones de este partido.
      </bf-notice>`,
      { imports: [Notice, LucideBan] },
    );

    const notice = screen.getByText(/cancelado tus valoraciones/).closest('bf-notice');
    expect(notice?.querySelectorAll('svg')).toHaveLength(1);
    expect(notice?.querySelector('svg')).toHaveClass('given');
  });
});
