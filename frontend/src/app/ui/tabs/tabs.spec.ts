import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fireEvent, render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Tab, TabLink, TabNav, TabPanel, Tabs } from './tabs';

/**
 * The CDK's key manager reads `keyCode`, which user-event leaves at 0 in jsdom: the keys go with the
 * one a browser gives them.
 */
const KEY_CODES = { ArrowLeft: 37, ArrowRight: 39, Home: 36, End: 35 } as const;

function press(key: keyof typeof KEY_CODES): void {
  if (document.activeElement !== null) {
    fireEvent.keyDown(document.activeElement, { key, keyCode: KEY_CODES[key] });
  }
}

describe('Tabs', () => {
  async function renderTabs() {
    const selected = signal('managers');
    await render(
      `<bf-tabs label="Puntos de la jornada" [(selected)]="selected">
        <button bfTab="managers">Managers</button>
        <button bfTab="players">Jugadores</button>
        <button bfTab="rules">Reglas</button>
        <section bfTabPanel="managers">Lucía Romero</section>
        <section bfTabPanel="players">Pablo</section>
        <section bfTabPanel="rules">Gol</section>
      </bf-tabs>`,
      { imports: [Tabs, Tab, TabPanel], componentProperties: { selected } },
    );
    return selected;
  }

  it('is a tablist named by its label, with the selected tab and its panel', async () => {
    await renderTabs();

    expect(screen.getByRole('tablist', { name: 'Puntos de la jornada' })).toBeInTheDocument();
    const managers = screen.getByRole('tab', { name: 'Managers' });
    expect(managers).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Jugadores' })).toHaveAttribute(
      'aria-selected',
      'false',
    );
    const panel = screen.getByRole('tabpanel', { name: 'Managers' });
    expect(panel).toHaveTextContent('Lucía Romero');
    expect(managers).toHaveAttribute('aria-controls', panel.id);
    expect(screen.getByText('Pablo')).not.toBeVisible();
  });

  it('keeps only the selected tab in the tab order', async () => {
    await renderTabs();

    expect(screen.getByRole('tab', { name: 'Managers' })).toHaveAttribute('tabindex', '0');
    expect(screen.getByRole('tab', { name: 'Jugadores' })).toHaveAttribute('tabindex', '-1');
  });

  it('selects a tab with a click', async () => {
    const selected = await renderTabs();

    await userEvent.click(screen.getByRole('tab', { name: 'Jugadores' }));

    expect(selected()).toBe('players');
    expect(screen.getByRole('tabpanel', { name: 'Jugadores' })).toHaveTextContent('Pablo');
    expect(screen.getByText('Lucía Romero')).not.toBeVisible();
  });

  it('moves and selects with the arrow keys, Home and End, wrapping at the ends', async () => {
    const selected = await renderTabs();

    await userEvent.tab();
    expect(screen.getByRole('tab', { name: 'Managers' })).toHaveFocus();

    press('ArrowRight');
    expect(selected()).toBe('players');
    expect(screen.getByRole('tab', { name: 'Jugadores' })).toHaveFocus();

    press('End');
    expect(selected()).toBe('rules');

    press('ArrowRight');
    expect(selected()).toBe('managers');

    press('ArrowLeft');
    expect(selected()).toBe('rules');

    press('Home');
    expect(selected()).toBe('managers');
  });
});

@Component({ selector: 'bf-test-page', template: '' })
class Page {}

describe('TabNav', () => {
  it('is a navigation whose links mark the current route', async () => {
    const { navigate } = await render(
      `<bf-tab-nav label="Clasificación">
        <a bfTabLink routerLink="/standings" [routerLinkActiveOptions]="{ exact: true }">General</a>
        <a bfTabLink routerLink="/standings/matchday">Por jornada</a>
      </bf-tab-nav>`,
      {
        imports: [TabNav, TabLink, RouterLink],
        routes: [
          { path: 'standings', component: Page },
          { path: 'standings/matchday', component: Page },
        ],
      },
    );

    await navigate('/standings/matchday');

    expect(screen.getByRole('navigation', { name: 'Clasificación' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Por jornada' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getByRole('link', { name: 'General' })).not.toHaveAttribute('aria-current');
  });
});
