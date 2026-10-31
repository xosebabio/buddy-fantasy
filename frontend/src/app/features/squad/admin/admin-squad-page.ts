import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SquadApi } from '../../../core/api/squad/squad-api';
import { PlayerRow } from '../../../shared/player/player-row';
import { POSITION_GROUP_TEXTS } from '../../../shared/position/position';
import { Badge } from '../../../ui/badge/badge';
import { Button } from '../../../ui/button/button';
import { Disclosure } from '../../../ui/disclosure/disclosure';
import { EmptyState } from '../../../ui/empty-state/empty-state';
import { List, ListItem } from '../../../ui/list/list';
import { LoadError } from '../../../ui/load-error/load-error';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { retiredPlayers, squadGroups } from '../squad-groups';

/**
 * The squad for the admin (RF-09, RF-10): the one of every member, but each player opens their
 * edition, and the retired ones are apart and read-only because they cannot come back.
 */
@Component({
  selector: 'bf-admin-squad-page',
  imports: [
    Badge,
    Button,
    Disclosure,
    EmptyState,
    List,
    ListItem,
    LoadError,
    PlayerRow,
    RouterLink,
    Skeleton,
  ],
  templateUrl: './admin-squad-page.html',
  styleUrl: './admin-squad-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSquadPage {
  readonly leagueId = input<string>();

  protected readonly players = inject(SquadApi).players(this.leagueId);
  protected readonly groups = computed(() => squadGroups(this.players.value() ?? []));
  protected readonly retired = computed(() => retiredPlayers(this.players.value() ?? []));
  protected readonly groupTexts = POSITION_GROUP_TEXTS;
}
