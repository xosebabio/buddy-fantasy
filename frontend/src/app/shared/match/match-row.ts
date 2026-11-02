import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import type { MatchSummaryResponse } from '../../core/api/match/match-api';
import { BfDatePipe } from '../time/date-pipe';
import { VENUE_TEXTS } from './venue';

/** What a row shows of a match: the summary of the calendar. */
export type MatchRowModel = Pick<
  MatchSummaryResponse,
  'matchday' | 'opponent' | 'venue' | 'kickoffAt' | 'result'
>;

/**
 * A match of the calendar as a row (FE-19): the matchday in figures, the opponent and, below, the
 * venue and the date. The result, if there is one, goes at the end with `[bfMatchRowTrailing]`
 * (a state, such as _Acta pendiente_) under it. It is the content of a `bfListItem`.
 */
@Component({
  selector: 'bf-match-row',
  imports: [BfDatePipe],
  template: `
    <span class="matchday" aria-hidden="true">
      {{ match().matchday }}
      <small i18n="@@shared.match.matchday">jornada</small>
    </span>
    <span class="body">
      <span class="name">
        <span class="sr-only" i18n="@@shared.match.matchdayContra">
          Jornada {{ match().matchday }}, contra
        </span>
        {{ match().opponent }}
      </span>
      <span class="meta"
        >{{ venueTexts[match().venue] }}, {{ match().kickoffAt | bfDate: 'dateTime' }}</span
      >
    </span>
    <span class="trailing">
      @if (match().result; as result) {
        <span class="result">{{ result.goalsFor }}–{{ result.goalsAgainst }}</span>
      }
      <ng-content select="[bfMatchRowTrailing]" />
    </span>
  `,
  styleUrl: './match-row.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchRow {
  readonly match = input.required<MatchRowModel>();

  protected readonly venueTexts = VENUE_TEXTS;
}
