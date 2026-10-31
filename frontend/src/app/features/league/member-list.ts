import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { LucideUserX } from '@lucide/angular';

import type { MemberResponse } from '../../core/api/league/league-api';
import { ROLE_TEXTS } from '../../core/layout/role-texts';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Avatar } from '../../ui/avatar/avatar';
import { Badge } from '../../ui/badge/badge';
import { IconButton } from '../../ui/button/icon-button';
import { List, ListItem } from '../../ui/list/list';

const UNKNOWN_NAME = $localize`:@@league.members.unknownName:Usuario sin nombre`;

/**
 * The members of a league with their role and since when they play (RF-08). With `removable`, each
 * one but the admin has a button to remove them, which emits `remove`.
 */
@Component({
  selector: 'bf-member-list',
  imports: [Avatar, Badge, BfDatePipe, IconButton, List, ListItem, LucideUserX],
  template: `
    <ul bfList bleed>
      @for (member of members(); track member.userId) {
        @let name = member.displayName ?? unknownName;
        <li bfListItem size="compact">
          <bf-avatar bfListItemLeading [name]="name" [src]="member.avatarUrl" />
          {{ name }}
          @if (member.userId === currentUserId()) {
            <span class="you" i18n="@@league.members.you">(tú)</span>
          }
          @if (member.role === 'ADMIN') {
            <bf-badge bfListItemMeta tone="accent">{{ roles.ADMIN }}</bf-badge>
          } @else {
            <span bfListItemMeta>{{ roles.MANAGER }}</span>
          }
          <span bfListItemMeta i18n="@@league.members.since"
            >Desde el {{ member.joinedAt | bfDate: 'day' }}</span
          >
          @if (removable() && member.role !== 'ADMIN') {
            <button
              bfIconButton
              bfListItemTrailing
              type="button"
              [label]="removeLabel(name)"
              [disabled]="disabled()"
              (click)="remove.emit(member)"
            >
              <svg lucideUserX></svg>
            </button>
          }
        </li>
      }
    </ul>
  `,
  styleUrl: './member-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberList {
  readonly members = input.required<readonly MemberResponse[]>();
  /** The user with the session, marked as «tú». */
  readonly currentUserId = input<string | null>(null);
  readonly removable = input(false);
  readonly disabled = input(false);
  readonly remove = output<MemberResponse>();

  protected readonly roles = ROLE_TEXTS;
  protected readonly unknownName = UNKNOWN_NAME;

  protected removeLabel(name: string): string {
    return $localize`:@@league.members.remove:Expulsar a ${name}:name:`;
  }
}
