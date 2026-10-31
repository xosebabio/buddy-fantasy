import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { RatingApi, type RatingWindowResponse } from '../../core/api/rating/rating-api';
import { onVisible } from '../../core/http/visible';
import { Badge } from '../../ui/badge/badge';
import { Countdown } from '../../ui/countdown/countdown';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';

/** The open windows first, the soonest to close at the top, and then the closed ones, the latest first. */
export function sortWindows(windows: readonly RatingWindowResponse[]): RatingWindowResponse[] {
  return [...windows].sort((a, b) =>
    a.status === b.status
      ? a.status === 'OPEN'
        ? a.closesAt.localeCompare(b.closesAt)
        : b.matchday - a.matchday
      : a.status === 'OPEN'
        ? -1
        : 1,
  );
}

/**
 * The rating windows of the league (RF-17): the open ones first, with their countdown and how many
 * players the user has rated, and then the closed ones. Each row opens the match to rate.
 */
@Component({
  selector: 'bf-ratings-page',
  imports: [Badge, Countdown, EmptyState, List, ListItem, LoadError, RouterLink, Skeleton],
  templateUrl: './ratings-page.html',
  styleUrl: './ratings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingsPage {
  readonly leagueId = input<string>();

  protected readonly windows = inject(RatingApi).windows(this.leagueId);
  protected readonly sorted = computed(() => sortWindows(this.windows.value() ?? []));

  constructor() {
    onVisible(() => {
      this.windows.reload();
    });
  }
}
