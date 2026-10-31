import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchApi } from '../../core/api/match/match-api';
import { MatchRow } from '../../shared/match/match-row';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';

/** The calendar of the team (RF-12): a row per matchday with the opponent, the date and the result if it is closed. */
@Component({
  selector: 'bf-matches-page',
  imports: [EmptyState, List, ListItem, LoadError, MatchRow, RouterLink, Skeleton],
  templateUrl: './matches-page.html',
  styleUrl: './matches-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchesPage {
  readonly leagueId = input<string>();

  protected readonly matches = inject(MatchApi).matches(this.leagueId);
  protected readonly byMatchday = computed(() =>
    [...(this.matches.value() ?? [])].sort((a, b) => a.matchday - b.matchday),
  );
}
