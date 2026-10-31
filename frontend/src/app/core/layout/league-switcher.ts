import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  TemplateRef,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideCheck, LucideChevronDown } from '@lucide/angular';

import type { MyLeagueResponse } from '../api/league/league-api';
import { LEAGUES_ROUTE } from '../auth/home-redirect';
import { SessionStore } from '../auth/session-store';
import { List, ListItem } from '../../ui/list/list';
import { Sheet, SheetClose, SheetTitle } from '../../ui/sheet/sheet';
import { ROLE_TEXTS } from './role-texts';

/**
 * The current league's name, which opens a bottom sheet to go to another of the user's leagues
 * (RF-08, FE-03), or to all of them.
 */
@Component({
  selector: 'bf-league-switcher',
  imports: [List, ListItem, LucideCheck, LucideChevronDown, RouterLink, SheetClose, SheetTitle],
  template: `
    <button
      type="button"
      class="switcher"
      aria-haspopup="dialog"
      [attr.aria-label]="label()"
      (click)="open()"
    >
      <span class="name">{{ league().name }}</span>
      <svg lucideChevronDown class="chevron" [size]="20"></svg>
    </button>

    <ng-template #leagues>
      <h2 bfSheetTitle i18n="@@core.leagueSwitcher.title">Cambiar de liga</h2>
      <ul bfList bleed>
        @for (option of session.leagues(); track option.leagueId) {
          @let current = option.leagueId === league().leagueId;
          <li>
            <button bfListItem type="button" [current]="current" [bfSheetClose]="option.leagueId">
              {{ option.name }}
              <span bfListItemMeta>{{ roles[option.role] }}</span>
              @if (current) {
                <svg lucideCheck bfListItemTrailing class="check"></svg>
              }
            </button>
          </li>
        }
      </ul>
      <a class="all" [routerLink]="leaguesRoute" i18n="@@core.leagueSwitcher.all">
        Ver todas tus ligas
      </a>
    </ng-template>
  `,
  styleUrl: './league-switcher.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeagueSwitcher {
  readonly league = input.required<MyLeagueResponse>();

  /** Starts with the visible name, as WCAG 2.5.3 asks, and says what the button does. */
  protected readonly label = computed(
    () => $localize`:@@core.leagueSwitcher.open:${this.league().name}:league:, cambiar de liga`,
  );

  protected readonly session = inject(SessionStore);
  protected readonly roles = ROLE_TEXTS;
  protected readonly leaguesRoute = LEAGUES_ROUTE;

  private readonly sheet = inject(Sheet);
  private readonly router = inject(Router);
  private readonly leagues = viewChild.required<TemplateRef<unknown>>('leagues');

  protected open(): void {
    this.sheet.open<string>(this.leagues()).closed.subscribe((leagueId) => {
      if (leagueId !== undefined && leagueId !== this.league().leagueId) {
        void this.router.navigate([LEAGUES_ROUTE, leagueId]);
      }
    });
  }
}
