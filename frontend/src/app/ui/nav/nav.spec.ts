import { Component } from '@angular/core';
import { LucideHouse, LucideUsers } from '@lucide/angular';
import { RouterLink } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { NavBar, NavItem, SideNav } from './nav';

@Component({ selector: 'bf-test-page', template: '' })
class Page {}

const routes = [
  { path: 'leagues/:id', component: Page },
  { path: 'leagues/:id/members', component: Page },
];

const imports = [NavBar, SideNav, NavItem, RouterLink, LucideHouse, LucideUsers];

const ITEMS = `
  <a bfNavItem routerLink="/leagues/1" [routerLinkActiveOptions]="{ exact: true }">
    <svg lucideHouse></svg>Jornada
  </a>
  <a bfNavItem routerLink="/leagues/1/members"><svg lucideUsers></svg>Liga</a>
`;

describe('NavBar', () => {
  it('is a named navigation whose active link is the current page', async () => {
    const { navigate } = await render(`<bf-nav-bar label="Principal">${ITEMS}</bf-nav-bar>`, {
      imports,
      routes,
    });

    await navigate('/leagues/1/members');

    const nav = screen.getByRole('navigation', { name: 'Principal' });
    const league = within(nav).getByRole('link', { name: 'Liga' });
    const home = within(nav).getByRole('link', { name: 'Jornada' });
    expect(league).toHaveAttribute('aria-current', 'page');
    expect(league).toHaveClass('bar', 'active');
    expect(home).not.toHaveAttribute('aria-current');
  });

  it('moves the active link when navigating', async () => {
    const { navigate } = await render(`<bf-nav-bar label="Principal">${ITEMS}</bf-nav-bar>`, {
      imports,
      routes,
    });
    await navigate('/leagues/1/members');

    await userEvent.click(screen.getByRole('link', { name: 'Jornada' }));

    expect(await screen.findByRole('link', { current: 'page' })).toHaveTextContent('Jornada');
  });
});

describe('SideNav', () => {
  it('has its header and footer around the navigation', async () => {
    const { navigate } = await render(
      `<bf-side-nav label="Principal">
        <span bfSideNavHeader>BuddyFantasy</span>
        ${ITEMS}
        <a bfSideNavFooter href="/profile">Perfil</a>
      </bf-side-nav>`,
      { imports, routes },
    );

    await navigate('/leagues/1');

    const nav = screen.getByRole('navigation', { name: 'Principal' });
    expect(within(nav).getAllByRole('link')).toHaveLength(2);
    expect(within(nav).getByRole('link', { name: 'Jornada' })).toHaveClass('side', 'active');
    expect(nav).not.toHaveTextContent('BuddyFantasy');
    expect(screen.getByRole('link', { name: 'Perfil' })).toBeInTheDocument();
  });
});
