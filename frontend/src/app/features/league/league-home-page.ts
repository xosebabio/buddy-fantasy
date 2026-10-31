import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideUserPlus } from '@lucide/angular';

import { LeagueApi } from '../../core/api/league/league-api';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Button } from '../../ui/button/button';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';

/**
 * The start of a league, provisional until the matchday arrives with its screens: the league and
 * an empty state, with the way to invite the team for its admin.
 */
@Component({
  selector: 'bf-league-home-page',
  imports: [BfDatePipe, Button, EmptyState, LoadError, LucideUserPlus, RouterLink, Skeleton],
  templateUrl: './league-home-page.html',
  styleUrl: './league-home-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeagueHomePage {
  readonly leagueId = input<string>();

  protected readonly league = inject(LeagueApi).league(this.leagueId);
}
