import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LeagueApi } from '../../core/api/league/league-api';
import { POSITION_GROUP_TEXTS, POSITIONS } from '../../shared/position/position';
import { Points } from '../../shared/scoring/points';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { leagueRules, scoringRows } from './rules';

/** The rules of the league (RF-07, RF-15), read-only: how a lineup is made and how each action scores. */
@Component({
  selector: 'bf-rules-page',
  imports: [LoadError, Points, Skeleton],
  templateUrl: './rules-page.html',
  styleUrl: './rules-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RulesPage {
  readonly leagueId = input<string>();

  protected readonly settings = inject(LeagueApi).settings(this.leagueId);
  protected readonly rules = computed(() => {
    const settings = this.settings.value();
    return settings === undefined ? [] : leagueRules(settings);
  });
  protected readonly scoring = computed(() => {
    const settings = this.settings.value();
    return settings === undefined ? [] : scoringRows(settings.scoringRules);
  });
  protected readonly positions = POSITIONS;
  protected readonly positionTexts = POSITION_GROUP_TEXTS;
}
