import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap, tap } from 'rxjs';

import { LeagueApi } from '../../core/api/league/league-api';
import { LEAGUES_ROUTE } from '../../core/auth/home-redirect';
import { SessionStore } from '../../core/auth/session-store';
import { ERROR_TEXTS } from '../../core/http/error-texts';
import { Online } from '../../core/http/online';
import { toProblem } from '../../core/http/problem';
import { Button } from '../../ui/button/button';
import { Confirm } from '../../ui/confirm/confirm';
import { LoadError } from '../../ui/load-error/load-error';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Toasts } from '../../ui/toast/toasts';
import { MemberList } from './member-list';

/**
 * The members of the league (RF-08) and the way out of it; its admin cannot leave, and goes from
 * here to invite and remove members.
 */
@Component({
  selector: 'bf-members-page',
  imports: [Button, LoadError, MemberList, RouterLink, Skeleton],
  templateUrl: './members-page.html',
  styleUrl: './members-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MembersPage {
  readonly leagueId = input<string>();

  private readonly leagueApi = inject(LeagueApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);

  protected readonly session = inject(SessionStore);
  protected readonly online = inject(Online);
  protected readonly members = this.leagueApi.members(this.leagueId);
  protected readonly league = computed(() => {
    const leagueId = this.leagueId();
    return leagueId === undefined ? undefined : this.session.league(leagueId);
  });
  protected readonly leaving = signal(false);

  protected leave(): void {
    const league = this.league();
    if (league === undefined || this.leaving()) {
      return;
    }
    this.confirm
      .ask({
        title: $localize`:@@league.members.leaveTitle:¿Salir de ${league.name}:league:?`,
        message: $localize`:@@league.members.leaveMessage:Dejarás de ver la liga. Podrás volver si te pasan un enlace de invitación.`,
        confirm: $localize`:@@league.members.leaveConfirm:Salir de la liga`,
        tone: 'danger',
      })
      .pipe(
        filter(Boolean),
        tap(() => {
          this.leaving.set(true);
        }),
        switchMap(() => this.leagueApi.leave(league.leagueId)),
        switchMap(() => this.session.reloadLeagues()),
      )
      .subscribe({
        next: () => {
          this.toasts.show({
            message: $localize`:@@league.members.left:Has salido de ${league.name}:league:`,
          });
          void this.router.navigateByUrl(LEAGUES_ROUTE);
        },
        error: (error: unknown) => {
          this.leaving.set(false);
          const code = toProblem(error)?.code;
          if (code) {
            this.toasts.show({ message: ERROR_TEXTS[code], tone: 'danger' });
          }
        },
      });
  }
}
