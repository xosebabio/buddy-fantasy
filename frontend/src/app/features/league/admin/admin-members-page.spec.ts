import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { loadSession } from '../../../core/auth/session.testing';
import { MEMBERS } from '../../../shared/roster/members.testing';
import { AdminMembersPage } from './admin-members-page';

describe('AdminMembersPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderAdmin() {
    const result = await render(AdminMembersPage, {
      inputs: { leagueId: 'l1' },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession();
    http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS);
    await result.fixture.whenStable();
    return result;
  }

  /** Clicks the button and confirms in the dialog that it opens. */
  async function confirm(button: string, title: string, confirmation: string) {
    await userEvent.click(screen.getByRole('button', { name: button }));
    const dialog = await screen.findByRole('alertdialog', { name: title });
    await userEvent.click(within(dialog).getByRole('button', { name: confirmation }));
  }

  it('explains that the link is only seen when it is generated', async () => {
    await renderAdmin();

    expect(screen.getByText(/El enlace solo se ve al generarlo/)).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Enlace de invitación' })).not.toBeInTheDocument();
  });

  it('generates a new link to share', async () => {
    const { fixture } = await renderAdmin();

    await confirm('Generar enlace nuevo', '¿Generar un enlace nuevo?', 'Generar enlace');
    const request = http.expectOne('/api/leagues/l1/invitation');
    expect(request.request.method).toBe('POST');
    request.flush({ token: 'tok-2' });
    await fixture.whenStable();

    expect(screen.getByRole('textbox', { name: 'Enlace de invitación' })).toHaveValue(
      `${location.origin}/invite/tok-2`,
    );
    expect(await screen.findByText('Enlace generado')).toBeInTheDocument();
  });

  it('revokes the link', async () => {
    const { fixture } = await renderAdmin();

    await confirm('Desactivar enlace', '¿Desactivar el enlace?', 'Desactivar enlace');
    const request = http.expectOne('/api/leagues/l1/invitation');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(await screen.findByText('Enlace desactivado')).toBeInTheDocument();
  });

  it('removes a member, but never the admin', async () => {
    const { fixture } = await renderAdmin();
    expect(screen.queryByRole('button', { name: 'Expulsar a Xosé Babío' })).not.toBeInTheDocument();

    await confirm('Expulsar a Brais Otero', '¿Expulsar a Brais Otero?', 'Expulsar');
    const request = http.expectOne('/api/leagues/l1/members/u2');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    TestBed.tick();
    http.expectOne('/api/leagues/l1/members').flush(MEMBERS.filter((m) => m.userId !== 'u2'));
    await fixture.whenStable();

    expect(screen.queryByText('Brais Otero', { selector: 'li *' })).not.toBeInTheDocument();
    expect(await screen.findByText('Brais Otero ya no está en la liga')).toBeInTheDocument();
  });

  it('explains a conflict of the server', async () => {
    const { fixture } = await renderAdmin();

    await confirm('Expulsar a Brais Otero', '¿Expulsar a Brais Otero?', 'Expulsar');
    http
      .expectOne('/api/leagues/l1/members/u2')
      .flush({ status: 409, code: 'ADMIN_CANNOT_LEAVE' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(
      await screen.findByText('El administrador no puede salir de la liga ni ser expulsado.'),
    ).toBeInTheDocument();
  });
});
