import { DIALOG_DATA } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LucideCheck } from '@lucide/angular';

import { PlayerRow } from '../../shared/player/player-row';
import { POSITION_GROUP_TEXTS } from '../../shared/position/position';
import { Button } from '../../ui/button/button';
import { List, ListItem } from '../../ui/list/list';
import { SheetClose, SheetTitle } from '../../ui/sheet/sheet';
import type { CandidateGroup } from './candidates';

export interface CandidatesData {
  readonly title: string;
  /** _Sin él te quedan 21,0 M_; nothing if the league has no budget. */
  readonly left: string | null;
  readonly groups: CandidateGroup[];
  /** Whether the place has a player, who can be taken out. */
  readonly removable: boolean;
}

/** What the sheet closes with; dismissing it closes with nothing. */
export type CandidateChoice = { kind: 'pick'; playerId: string } | { kind: 'remove' };

/** The sheet that opens on touching a spot of the lineup: who can take the place (decision 7 of plan 12). */
@Component({
  selector: 'bf-candidates-sheet',
  imports: [Button, List, ListItem, LucideCheck, PlayerRow, SheetClose, SheetTitle],
  template: `
    <h2 bfSheetTitle>{{ data.title }}</h2>
    @if (data.left; as left) {
      <p class="left">{{ left }}</p>
    }
    @for (group of data.groups; track group.position) {
      <section [attr.aria-labelledby]="'candidates-' + group.position">
        <h3 [id]="'candidates-' + group.position">{{ groupTexts[group.position] }}</h3>
        <ul bfList>
          @for (candidate of group.candidates; track candidate.player.id) {
            <li>
              <button
                bfListItem
                type="button"
                size="compact"
                [bfSheetClose]="pick(candidate.player.id)"
              >
                <bf-player-row
                  [player]="candidate.player"
                  [value]="candidate.player.currentValue"
                  [showPosition]="false"
                >
                  @if (candidate.current) {
                    <span bfPlayerRowMeta class="current">
                      <svg lucideCheck [size]="16"></svg>
                      <ng-container i18n="@@lineup.candidates.current"
                        >En la alineación</ng-container
                      >
                    </span>
                  } @else if (!candidate.fits) {
                    <span bfPlayerRowMeta class="over" i18n="@@lineup.candidates.over"
                      >Supera el presupuesto</span
                    >
                  }
                </bf-player-row>
              </button>
            </li>
          }
        </ul>
      </section>
    }
    @if (data.removable) {
      <button
        bfButton
        type="button"
        block
        [bfSheetClose]="remove"
        i18n="@@lineup.candidates.remove"
      >
        Quitar de la alineación
      </button>
    }
  `,
  styleUrl: './candidates-sheet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidatesSheet {
  protected readonly data = inject<CandidatesData>(DIALOG_DATA);
  protected readonly groupTexts = POSITION_GROUP_TEXTS;
  protected readonly remove: CandidateChoice = { kind: 'remove' };

  protected pick(playerId: string): CandidateChoice {
    return { kind: 'pick', playerId };
  }
}
