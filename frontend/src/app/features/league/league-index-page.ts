import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SessionStore } from '../../core/auth/session-store';
import { List, ListItem } from '../../ui/list/list';

/**
 * The index of «Liga» (RF-08): where the squad, the calendar, the rules and the members are, and
 * for its admin, the pages that manage the league. It needs no request: the session has the league.
 */
@Component({
  selector: 'bf-league-index-page',
  imports: [List, ListItem, RouterLink],
  templateUrl: './league-index-page.html',
  styleUrl: './league-index-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeagueIndexPage {
  readonly leagueId = input<string>();

  private readonly session = inject(SessionStore);

  protected readonly league = computed(() => {
    const leagueId = this.leagueId();
    return leagueId === undefined ? undefined : this.session.league(leagueId);
  });
}
