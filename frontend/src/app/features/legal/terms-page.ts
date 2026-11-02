import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LegalLayout } from './legal-layout';

/** The terms of service (RNF-10): a fixed text, readable with or without a session. */
@Component({
  selector: 'bf-terms-page',
  imports: [LegalLayout, RouterLink],
  templateUrl: './terms-page.html',
  styleUrl: './terms-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsPage {}
