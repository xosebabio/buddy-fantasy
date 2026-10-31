import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import type { MockInstance } from 'vitest';

import { LEAGUES, loadSession } from '../../core/auth/session.testing';
import { errorInterceptor } from '../../core/http/error-interceptor';
import { InvitationPage } from './invitation-page';

const NOT_FOUND = { status: 404, statusText: 'Not Found' };

describe('InvitationPage', () => {
  let http: HttpTestingController;
  let navigate: MockInstance<Router['navigate']>;
  let navigateByUrl: MockInstance<Router['navigateByUrl']>;

  beforeEach(() => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  /** Shows the page loading the invitation, and then with this preview or a `404`. */
  async function renderInvitation(preview: object | null) {
    const result = await render(InvitationPage, {
      inputs: { token: 'tok-1' },
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    await loadSession();
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const request = http.expectOne('/api/invitations/tok-1');
    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    if (preview === null) {
      request.flush({ status: 404 }, NOT_FOUND);
    } else {
      request.flush(preview);
    }
    await result.fixture.whenStable();
    return result;
  }

  it('shows the league and joins it', async () => {
    const { fixture } = await renderInvitation({ leagueName: 'Fútbol 7', alreadyMember: false });

    expect(screen.getByText('Fútbol 7')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Unirme a la liga' }));
    const join = http.expectOne('/api/invitations/tok-1/join');
    expect(join.request.method).toBe('POST');
    join.flush({ leagueId: 'l3' });
    http
      .expectOne('/api/me/leagues')
      .flush([...LEAGUES, { leagueId: 'l3', name: 'Fútbol 7', role: 'MANAGER' }]);
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l3']);
    expect(await screen.findByText('Te has unido a Fútbol 7')).toBeInTheDocument();
  });

  it('takes a member to the league', async () => {
    const { fixture } = await renderInvitation({
      leagueName: 'Os Domingueiros',
      alreadyMember: true,
    });

    expect(screen.getByText('Ya juegas en esta liga.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Ir a la liga' }));
    http.expectOne('/api/invitations/tok-1/join').flush({ leagueId: 'l1' });
    http.expectOne('/api/me/leagues').flush(LEAGUES);
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(['/leagues', 'l1']);
  });

  it('explains a link that is not valid, without leaving the page', async () => {
    await renderInvitation(null);

    expect(screen.getByText(/Este enlace de invitación ya no vale/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver tus ligas' })).toHaveAttribute('href', '/leagues');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('explains a link revoked before joining', async () => {
    const { fixture } = await renderInvitation({ leagueName: 'Fútbol 7', alreadyMember: false });

    await userEvent.click(screen.getByRole('button', { name: 'Unirme a la liga' }));
    http.expectOne('/api/invitations/tok-1/join').flush({ status: 404 }, NOT_FOUND);
    await fixture.whenStable();

    expect(screen.getByText(/Este enlace de invitación ya no vale/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Unirme a la liga' })).not.toBeInTheDocument();
  });
});
