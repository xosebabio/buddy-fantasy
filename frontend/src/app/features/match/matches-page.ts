import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchApi } from '../../core/api/match/match-api';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { VENUE_TEXTS } from './venue';

/** The calendar of the team (RF-12): a row per matchday with the opponent, the date and the result if it is closed. */
@Component({
  selector: 'bf-matches-page',
  imports: [BfDatePipe, EmptyState, List, ListItem, LoadError, RouterLink, Skeleton],
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
  protected readonly venueTexts = VENUE_TEXTS;
}
