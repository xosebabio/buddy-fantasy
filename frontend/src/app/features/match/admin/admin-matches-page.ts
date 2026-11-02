import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatchApi } from '../../../core/api/match/match-api';
import { onVisible } from '../../../core/http/visible';
import { MatchRow } from '../../../shared/match/match-row';
import { MATCH_PHASE_TEXTS, matchPhase } from '../../../shared/match/match-phase';
import { Badge } from '../../../ui/badge/badge';
import { Button } from '../../../ui/button/button';
import { EmptyState } from '../../../ui/empty-state/empty-state';
import { List, ListItem } from '../../../ui/list/list';
import { LoadError } from '../../../ui/load-error/load-error';
import { Skeleton } from '../../../ui/skeleton/skeleton';

/**
 * The calendar for the admin (RF-12): the one of every member, newest matchday first, with the
 * state of each match (_Programado_, _Acta pendiente_ or _Cerrado_) and a link to administer it.
 * The state uses the browser's clock, so it is a guide (FE-09).
 */
@Component({
  selector: 'bf-admin-matches-page',
  imports: [Badge, Button, EmptyState, List, ListItem, LoadError, MatchRow, RouterLink, Skeleton],
  templateUrl: './admin-matches-page.html',
  styleUrl: './admin-matches-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminMatchesPage {
  readonly leagueId = input<string>();

  protected readonly matches = inject(MatchApi).matches(this.leagueId);
  protected readonly phaseTexts = MATCH_PHASE_TEXTS;
  protected readonly rows = computed(() => {
    const now = new Date();
    return [...(this.matches.value() ?? [])]
      .sort((a, b) => b.matchday - a.matchday)
      .map((match) => ({ match, phase: matchPhase(match, now) }));
  });

  constructor() {
    onVisible(() => {
      this.matches.reload();
    });
  }
}
