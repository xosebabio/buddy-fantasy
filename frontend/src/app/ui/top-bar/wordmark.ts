import { ChangeDetectionStrategy, Component } from '@angular/core';

/** The logotype in text, «BuddyFantasy», with _Fantasy_ in the brand's green (ui/README.md). */
@Component({
  selector: 'bf-wordmark',
  template: `<span i18n="@@ui.wordmark">Buddy<span class="accent">Fantasy</span></span>`,
  styleUrl: './wordmark.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Wordmark {}
