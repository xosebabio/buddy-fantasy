import { CdkAccordion } from '@angular/cdk/accordion';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { List } from '../list/list';
import { Disclosure } from './disclosure';

const ROWS = `
  <li bfDisclosure>
    <span bfListItemLeading>2</span>
    Xosé Babío
    <span bfListItemMeta>11 jornadas</span>
    <span bfListItemTrailing>+38</span>
    <div bfDisclosurePanel>Pablo +14</div>
  </li>
  <li bfDisclosure>
    Manu Vidal
    <div bfDisclosurePanel>Diego +9</div>
  </li>
`;

describe('Disclosure', () => {
  it('is a collapsed row button of the list, with its slots and a chevron', async () => {
    await render(`<ul bfList>${ROWS}</ul>`, { imports: [List, Disclosure] });

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(2);
    const row = within(items[0] ?? document.body).getByRole('button');
    expect(row).toHaveAccessibleName(/^2 Xosé Babío 11 jornadas \+38$/);
    expect(row).toHaveAttribute('aria-expanded', 'false');
    expect(row.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByText('Pablo +14')).not.toBeVisible();
  });

  it('opens and closes its detail', async () => {
    await render(`<ul bfList>${ROWS}</ul>`, { imports: [List, Disclosure] });
    const row = screen.getByRole('button', { name: /Xosé Babío/ });

    await userEvent.click(row);

    expect(row).toHaveAttribute('aria-expanded', 'true');
    const panel = screen.getByText('Pablo +14');
    expect(panel).toBeVisible();
    expect(row).toHaveAttribute('aria-controls', panel.parentElement?.id);

    await userEvent.click(row);

    expect(row).toHaveAttribute('aria-expanded', 'false');
    expect(panel).not.toBeVisible();
  });

  it('can start open and tells when it changes', async () => {
    const expandedChange = vi.fn();
    await render(
      `<ul bfList>
        <li bfDisclosure expanded (expandedChange)="expandedChange($event)">
          Xosé Babío<div bfDisclosurePanel>Pablo +14</div>
        </li>
      </ul>`,
      { imports: [List, Disclosure], componentProperties: { expandedChange } },
    );

    expect(screen.getByText('Pablo +14')).toBeVisible();

    await userEvent.click(screen.getByRole('button', { name: 'Xosé Babío' }));

    expect(expandedChange).toHaveBeenCalledWith(false);
  });

  it('keeps one row open at a time inside an accordion', async () => {
    await render(`<ul bfList cdkAccordion>${ROWS}</ul>`, {
      imports: [List, Disclosure, CdkAccordion],
    });

    await userEvent.click(screen.getByRole('button', { name: /Xosé Babío/ }));
    await userEvent.click(screen.getByRole('button', { name: /Manu Vidal/ }));

    expect(screen.getByRole('button', { name: /Xosé Babío/ })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(screen.getByText('Diego +9')).toBeVisible();
  });
});
