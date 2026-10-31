import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TabLink, TabNav } from '../../ui/tabs/tabs';

/**
 * The route tabs of the points: _General_ and _Por jornada_, which opens the matchday `matchId`
 * (the latest one with points, or the one being seen). Without one there is nothing to choose, so
 * it shows no tabs.
 */
@Component({
  selector: 'bf-standings-nav',
  imports: [RouterLink, TabLink, TabNav],
  template: `
    @if (matchId(); as matchId) {
      <bf-tab-nav label="Clasificación" i18n-label="@@scoring.nav.label" bleed>
        <a
          bfTabLink
          [routerLink]="['/leagues', leagueId(), 'standings']"
          [routerLinkActiveOptions]="{ exact: true }"
          i18n="@@scoring.nav.general"
          >General</a
        >
        <a
          bfTabLink
          [routerLink]="['/leagues', leagueId(), 'standings', matchId]"
          i18n="@@scoring.nav.matchday"
          >Por jornada</a
        >
      </bf-tab-nav>
    }
  `,
  styles: ':host { display: block; }',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandingsNav {
  readonly leagueId = input.required<string>();
  readonly matchId = input<string | null>(null);
}
