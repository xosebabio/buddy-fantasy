import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import type { PlayerMatchdayResponse } from '../../core/api/scoring/scoring-api';
import { Points } from './points';
import { actionPoints, SCORING_ACTION_TEXTS } from './scoring-action';

/** A player's points in a matchday, as `PlayerMatchdayResponse` and `LinedUpPlayerResponse` give them. */
export type PointsBreakdownModel = Pick<
  PlayerMatchdayResponse,
  'actions' | 'ratingPoints' | 'points'
>;

/**
 * Where a player's points in a matchday come from (FE-19): each action that scored, the rating and
 * the total, as a description list with the figures in a column. It goes in the panel of a
 * `bfDisclosure` whose row shows the total.
 */
@Component({
  selector: 'bf-points-breakdown',
  imports: [Points],
  template: `
    <dl>
      @for (line of actions(); track line.action) {
        <dt>{{ actionTexts[line.action] }}</dt>
        <dd><bf-points [value]="line.points" /></dd>
      }
      <dt i18n="@@shared.pointsBreakdown.rating">Valoración</dt>
      <dd><bf-points [value]="breakdown().ratingPoints" /></dd>
      <dt class="total" i18n="@@shared.pointsBreakdown.total">Total</dt>
      <dd class="total"><bf-points [value]="breakdown().points" /></dd>
    </dl>
  `,
  styleUrl: './points-breakdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PointsBreakdown {
  readonly breakdown = input.required<PointsBreakdownModel>();

  protected readonly actionTexts = SCORING_ACTION_TEXTS;
  protected readonly actions = computed(() => actionPoints(this.breakdown().actions));
}
