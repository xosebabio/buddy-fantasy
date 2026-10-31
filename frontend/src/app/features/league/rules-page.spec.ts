import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { render, screen, within } from '@testing-library/angular';

import { RulesPage } from './rules-page';
import { SETTINGS } from '../../shared/roster/settings.testing';

describe('RulesPage', () => {
  let http: HttpTestingController;

  afterEach(() => {
    http.verify();
  });

  async function renderRules() {
    const result = await render(RulesPage, {
      inputs: { leagueId: 'l2' },
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return result;
  }

  it('shows a skeleton while it loads', async () => {
    await renderRules();

    expect(screen.getByText('', { selector: '[aria-busy="true"]' })).toBeInTheDocument();
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
  });

  it('shows the parameters and the points table with a column per position', async () => {
    const { fixture } = await renderRules();
    http.expectOne('/api/leagues/l2/settings').flush(SETTINGS);
    await fixture.whenStable();

    expect(screen.getByText('Jugadores por alineación')).toBeInTheDocument();
    expect(screen.getByText('2 h antes del partido')).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Acción', 'Porteros', 'Defensas', 'Medios', 'Delanteros']);
    const goal = within(table).getByRole('row', { name: /^Gol \+10/ });
    expect(goal).toHaveTextContent('+10+6+5+4');
    expect(within(table).getByRole('row', { name: /Roja directa/ })).toHaveTextContent('−3');
    expect(screen.getByRole('region', { name: 'Puntos por acción' })).toBeInTheDocument();
  });

  it('offers to try again when it fails to load', async () => {
    const { fixture } = await renderRules();
    http
      .expectOne('/api/leagues/l2/settings')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
