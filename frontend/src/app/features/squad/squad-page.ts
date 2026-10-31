import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SquadApi } from '../../core/api/squad/squad-api';
import { PlayerRow } from '../../shared/player/player-row';
import { POSITION_GROUP_TEXTS } from '../../shared/position/position';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { squadGroups } from './squad-groups';

/** The squad of the team (RF-11): who plays, by position, with their value; each one opens their sheet. */
@Component({
  selector: 'bf-squad-page',
  imports: [EmptyState, List, ListItem, LoadError, PlayerRow, RouterLink, Skeleton],
  templateUrl: './squad-page.html',
  styleUrl: './squad-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SquadPage {
  readonly leagueId = input<string>();

  protected readonly players = inject(SquadApi).players(this.leagueId);
  protected readonly groups = computed(() => squadGroups(this.players.value() ?? []));
  protected readonly groupTexts = POSITION_GROUP_TEXTS;
}
