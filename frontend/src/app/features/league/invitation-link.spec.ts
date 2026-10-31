import { TestBed } from '@angular/core/testing';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Toasts } from '../../ui/toast/toasts';
import { InvitationLink } from './invitation-link';

describe('InvitationLink', () => {
  const url = `${location.origin}/invite/tok%2F1`;

  afterEach(() => {
    Reflect.deleteProperty(navigator, 'share');
  });

  async function renderLink() {
    await render(InvitationLink, { inputs: { token: 'tok/1', leagueName: 'Os Domingueiros' } });
  }

  it('shows the link to copy it by hand', async () => {
    await renderLink();

    expect(screen.getByLabelText('Enlace de invitación')).toHaveValue(url);
    expect(screen.getByLabelText('Enlace de invitación')).toHaveAttribute('readonly');
  });

  it('shares it with the device’s share sheet where there is one', async () => {
    const share = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'share', { value: share, configurable: true });
    await renderLink();

    await userEvent.click(screen.getByRole('button', { name: 'Compartir enlace' }));

    expect(share).toHaveBeenCalledWith({
      title: 'Únete a Os Domingueiros',
      text: 'Juega en Os Domingueiros con nosotros en BuddyFantasy.',
      url,
    });
  });

  it('copies it to the clipboard elsewhere', async () => {
    const user = userEvent.setup();
    await renderLink();
    const show = vi.spyOn(TestBed.inject(Toasts), 'show');

    await user.click(screen.getByRole('button', { name: 'Copiar enlace' }));

    expect(await navigator.clipboard.readText()).toBe(url);
    expect(show).toHaveBeenCalledWith({ message: 'Enlace copiado' });
  });
});
