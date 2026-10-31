import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { answerSession, LEAGUES, USER } from '../auth/session.testing';
import { SessionStore } from '../auth/session-store';
import { LeagueSwitcher } from './league-switcher';

describe('LeagueSwitcher', () => {
  async function renderSwitcher() {
    const result = await render(LeagueSwitcher, {
      inputs: { league: LEAGUES[0] },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const loaded = TestBed.inject(SessionStore).load();
    answerSession(TestBed.inject(HttpTestingController), USER);
    await loaded;
    await result.fixture.whenStable();
    return result;
  }

  async function open() {
    await userEvent.click(screen.getByRole('button', { name: 'Os Domingueiros, cambiar de liga' }));
    return screen.findByRole('dialog', { name: 'Cambiar de liga' });
  }

  it('shows the current league and says that it opens a dialog', async () => {
    await renderSwitcher();

    expect(
      screen.getByRole('button', { name: 'Os Domingueiros, cambiar de liga' }),
    ).toHaveAttribute('aria-haspopup', 'dialog');
  });

  it('lists the user’s leagues with their role, the current one marked', async () => {
    await renderSwitcher();

    const sheet = await open();

    const current = within(sheet).getByRole('button', { name: /Os Domingueiros/ });
    const other = within(sheet).getByRole('button', { name: /Veteranos do Sar/ });
    expect(current).toHaveAttribute('aria-current', 'true');
    expect(current).toHaveTextContent('Admin');
    expect(other).not.toHaveAttribute('aria-current');
    expect(other).toHaveTextContent('Manager');
    expect(within(sheet).getByRole('link', { name: 'Ver todas tus ligas' })).toHaveAttribute(
      'href',
      '/leagues',
    );
  });

  it('goes to the league chosen', async () => {
    await renderSwitcher();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await userEvent.click(within(await open()).getByRole('button', { name: /Veteranos do Sar/ }));

    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l2']);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('stays when the current league is chosen', async () => {
    await renderSwitcher();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');

    await userEvent.click(within(await open()).getByRole('button', { name: /Os Domingueiros/ }));

    expect(navigate).not.toHaveBeenCalled();
  });
});
