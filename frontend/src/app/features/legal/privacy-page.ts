import { ChangeDetectionStrategy, Component } from '@angular/core';

import { LegalLayout } from './legal-layout';

/** The privacy policy (RNF-10, art. 13 RGPD): a fixed text, readable with or without a session. */
@Component({
  selector: 'bf-privacy-page',
  imports: [LegalLayout],
  templateUrl: './privacy-page.html',
  styleUrl: './privacy-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPage {}
