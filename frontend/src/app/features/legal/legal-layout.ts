import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TopBar } from '../../ui/top-bar/top-bar';
import { Wordmark } from '../../ui/top-bar/wordmark';

/** The two legal texts; each one links to the other from its foot. */
export type LegalPage = 'privacy' | 'terms';

/**
 * The frame of the legal pages (plan 14 bis §4): the logotype at the top, back to the start, the
 * text in the middle and the link to the other page at the foot. It has no shell around it.
 */
@Component({
  selector: 'bf-legal-layout',
  imports: [RouterLink, TopBar, Wordmark],
  templateUrl: './legal-layout.html',
  styleUrl: './legal-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalLayout {
  readonly current = input.required<LegalPage>();
}
