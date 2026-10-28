import { render, screen, within } from '@testing-library/angular';
import { LucideX } from '@lucide/angular';

import { List, ListItem } from './list';

describe('List', () => {
  it('is a list of rows with a title and a meta line', async () => {
    await render(
      `<ul bfList>
        <li bfListItem>Os Domingueiros<span bfListItemMeta>Admin</span></li>
        <li bfListItem>Veteranos do Sar<span bfListItemMeta>Manager</span></li>
      </ul>`,
      { imports: [List, ListItem] },
    );

    const items = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Os Domingueiros');
    expect(items[0]).toHaveTextContent('Admin');
    expect(items[0]).toHaveClass('large');
  });

  it('bleeds to the edges of the page when asked', async () => {
    await render('<ul bfList bleed><li bfListItem>Uno</li></ul>', {
      imports: [List, ListItem],
    });

    expect(screen.getByRole('list')).toHaveClass('bleed');
  });

  it('has a link row with a chevron hidden from screen readers', async () => {
    await render('<ul bfList><li><a bfListItem href="/leagues/1">Os Domingueiros</a></li></ul>', {
      imports: [List, ListItem],
    });

    const link = screen.getByRole('link', { name: 'Os Domingueiros' });
    expect(link.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('has no chevron in a row that is not a link', async () => {
    await render('<ul bfList><li bfListItem>Os Domingueiros</li></ul>', {
      imports: [List, ListItem],
    });

    expect(screen.getByRole('listitem').querySelector('svg')).toBeNull();
  });

  it('marks the current row', async () => {
    await render(
      `<ul bfList>
        <li><button bfListItem type="button" current>Os Domingueiros</button></li>
        <li><button bfListItem type="button">Veteranos do Sar</button></li>
      </ul>`,
      { imports: [List, ListItem] },
    );

    expect(screen.getByRole('button', { name: 'Os Domingueiros' })).toHaveAttribute(
      'aria-current',
      'true',
    );
    expect(screen.getByRole('button', { name: 'Veteranos do Sar' })).not.toHaveAttribute(
      'aria-current',
    );
  });

  it('places the leading and trailing content around the title', async () => {
    await render(
      `<ul bfList>
        <li bfListItem size="compact">
          <span bfListItemLeading>XB</span>
          Xosé Babío
          <button bfListItemTrailing type="button" aria-label="Expulsar"><svg lucideX></svg></button>
        </li>
      </ul>`,
      { imports: [List, ListItem, LucideX] },
    );

    const item = screen.getByRole('listitem');
    expect(item).toHaveClass('compact');
    expect(item.firstElementChild).toHaveTextContent('XB');
    expect(within(item).getByRole('button', { name: 'Expulsar' })).toBeInTheDocument();
  });
});
