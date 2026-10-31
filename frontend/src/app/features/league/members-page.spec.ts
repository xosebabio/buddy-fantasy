import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { LEAGUES, loadSession } from '../../core/auth/session.testing';
import { SessionStore } from '../../core/auth/session-store';
import { MembersPage } from './members-page';
import { MEMBERS } from './members.testing';

describe('MembersPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  async function renderMembers(leagueId: string) {
    const result = await render(MembersPage, {
      inputs: { leagueId },
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    await loadSession();
    http = TestBed.inject(HttpTestingController);
    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    http.expectOne(`/api/leagues/${leagueId}/members`).flush(MEMBERS);
    await result.fixture.whenStable();
    return result;
  }

  async function confirmLeave() {
    await userEvent.click(screen.getByRole('button', { name: 'Salir de la liga' }));
    const dialog = await screen.findByRole('alertdialog', { name: '¿Salir de Veteranos do Sar?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Salir de la liga' }));
  }

  it('lists the members with their role and since when they play', async () => {
    await renderMembers('l2');

    const rows = within(screen.getByRole('list')).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Xosé Babío');
    expect(rows[0]).toHaveTextContent('(tú)');
    expect(rows[0]).toHaveTextContent('Admin');
    expect(rows[1]).toHaveTextContent('Brais Otero');
    expect(rows[1]).toHaveTextContent('Manager');
    expect(rows[1]).toHaveTextContent('Desde el 1 de febrero de 2026');
    expect(rows[2]).toHaveTextContent('Usuario sin nombre');
    expect(screen.queryByRole('button', { name: /Expulsar/ })).not.toBeInTheDocument();
  });

  it('lets a manager leave the league once they confirm', async () => {
    const { fixture } = await renderMembers('l2');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await confirmLeave();
    const leave = http.expectOne('/api/leagues/l2/members/me');
    expect(leave.request.method).toBe('DELETE');
    leave.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne('/api/me/leagues').flush([LEAGUES[0]]);
    await fixture.whenStable();

    expect(TestBed.inject(SessionStore).league('l2')).toBeUndefined();
    expect(navigate).toHaveBeenCalledWith('/leagues');
    expect(await screen.findByText('Has salido de Veteranos do Sar')).toBeInTheDocument();
  });

  it('stays when the manager cancels', async () => {
    await renderMembers('l2');

    await userEvent.click(screen.getByRole('button', { name: 'Salir de la liga' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));

    http.expectNone('/api/leagues/l2/members/me');
  });

  it('explains a conflict of the server', async () => {
    const { fixture } = await renderMembers('l2');

    await confirmLeave();
    http
      .expectOne('/api/leagues/l2/members/me')
      .flush({ status: 409, code: 'ADMIN_CANNOT_LEAVE' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(
      await screen.findByText('El administrador no puede salir de la liga ni ser expulsado.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Salir de la liga' })).toBeEnabled();
  });

  it('sends the admin to manage the members, who cannot leave', async () => {
    await renderMembers('l1');

    expect(screen.getByRole('link', { name: 'Invitar y gestionar miembros' })).toHaveAttribute(
      'href',
      '/leagues/l1/admin/members',
    );
    expect(screen.getByText('Como administrador, no puedes salir de la liga.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Salir de la liga' })).not.toBeInTheDocument();
  });
});
