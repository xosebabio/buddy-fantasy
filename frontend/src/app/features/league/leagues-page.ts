import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucidePlus } from '@lucide/angular';

import { SessionStore } from '../../core/auth/session-store';
import { ROLE_TEXTS } from '../../core/layout/role-texts';
import { Badge } from '../../ui/badge/badge';
import { Button } from '../../ui/button/button';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { List, ListItem } from '../../ui/list/list';

/**
 * The user's leagues with their role (RF-08), and the way to create one (RF-05). They are the ones
 * of the session, which the pages that create, join or leave a league reload.
 */
@Component({
  selector: 'bf-leagues-page',
  imports: [Badge, Button, EmptyState, List, ListItem, LucidePlus, RouterLink],
  templateUrl: './leagues-page.html',
  styleUrl: './leagues-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaguesPage {
  protected readonly session = inject(SessionStore);
  protected readonly roles = ROLE_TEXTS;
}
