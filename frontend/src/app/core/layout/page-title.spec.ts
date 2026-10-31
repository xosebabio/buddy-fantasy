import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, TitleStrategy } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { PageTitle } from './page-title';

@Component({
  selector: 'bf-test-page',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class Page {}

describe('PageTitle', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'titled', component: Page, title: 'Tus ligas' },
          { path: 'untitled', component: Page },
        ]),
        { provide: TitleStrategy, useClass: PageTitle },
      ],
    });
  });

  it('names the tab after the page and the app', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/titled');

    expect(document.title).toBe('Tus ligas – Buddy Fantasy');
  });

  it('names the tab after the app when the page has no title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/untitled');

    expect(document.title).toBe('Buddy Fantasy');
  });
});
