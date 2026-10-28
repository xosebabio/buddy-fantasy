import { render, screen } from '@testing-library/angular';

import { EmptyState } from './empty-state';

describe('EmptyState', () => {
  it('shows the message and the action, with the bib hidden from screen readers', async () => {
    const { container } = await render(
      `<bf-empty-state>
        <p>Aún no juegas en ninguna liga.</p>
        <a bfEmptyStateAction href="/leagues/new">Crear liga</a>
      </bf-empty-state>`,
      { imports: [EmptyState] },
    );

    expect(screen.getByText('Aún no juegas en ninguna liga.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Crear liga' })).toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });
});
