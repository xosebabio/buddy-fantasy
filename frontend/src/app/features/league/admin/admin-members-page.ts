import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { filter, type Observable, switchMap, tap } from 'rxjs';

import { LeagueApi, type MemberResponse } from '../../../core/api/league/league-api';
import { SessionStore } from '../../../core/auth/session-store';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { Button } from '../../../ui/button/button';
import { Confirm } from '../../../ui/confirm/confirm';
import { LoadError } from '../../../ui/load-error/load-error';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Toasts } from '../../../ui/toast/toasts';
import { InvitationLink } from '../invitation-link';
import { MemberList } from '../member-list';

/**
 * The admin's members page (RF-06, RF-08): the invitation link, which the backend only shows when
 * it is generated, so it is generated anew to share it (and the previous one stops working) or
 * revoked; and the members, to remove any but the admin.
 */
@Component({
  selector: 'bf-admin-members-page',
  imports: [Button, InvitationLink, LoadError, MemberList, Skeleton],
  templateUrl: './admin-members-page.html',
  styleUrl: './admin-members-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminMembersPage {
  readonly leagueId = input<string>();

  private readonly leagueApi = inject(LeagueApi);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);

  protected readonly session = inject(SessionStore);
  protected readonly online = inject(Online);
  protected readonly members = this.leagueApi.members(this.leagueId);
  protected readonly league = computed(() => {
    const leagueId = this.leagueId();
    return leagueId === undefined ? undefined : this.session.league(leagueId);
  });
  /** The link generated on this page, the only time it can be seen. */
  protected readonly token = signal<string | null>(null);
  /** The action under way, which disables the others. */
  protected readonly busy = signal<'regenerate' | 'revoke' | 'remove' | null>(null);

  protected regenerate(): void {
    this.act(
      'regenerate',
      {
        title: $localize`:@@league.admin.members.regenerateTitle:¿Generar un enlace nuevo?`,
        message: $localize`:@@league.admin.members.regenerateMessage:El enlace anterior dejará de funcionar.`,
        confirm: $localize`:@@league.admin.members.regenerateConfirm:Generar enlace`,
      },
      (leagueId) =>
        this.leagueApi.regenerateInvitation(leagueId).pipe(
          tap(({ token }) => {
            this.token.set(token);
            this.toasts.show({
              message: $localize`:@@league.admin.members.regenerated:Enlace generado`,
            });
          }),
        ),
    );
  }

  protected revoke(): void {
    this.act(
      'revoke',
      {
        title: $localize`:@@league.admin.members.revokeTitle:¿Desactivar el enlace?`,
        message: $localize`:@@league.admin.members.revokeMessage:Nadie podrá unirse con él. Puedes generar otro cuando quieras.`,
        confirm: $localize`:@@league.admin.members.revokeConfirm:Desactivar enlace`,
        tone: 'danger',
      },
      (leagueId) =>
        this.leagueApi.revokeInvitation(leagueId).pipe(
          tap(() => {
            this.token.set(null);
            this.toasts.show({
              message: $localize`:@@league.admin.members.revoked:Enlace desactivado`,
            });
          }),
        ),
    );
  }

  protected remove(member: MemberResponse): void {
    const name = member.displayName ?? $localize`:@@league.admin.members.unknownName:este miembro`;
    this.act(
      'remove',
      {
        title: $localize`:@@league.admin.members.removeTitle:¿Expulsar a ${name}:name:?`,
        message: $localize`:@@league.admin.members.removeMessage:Dejará de ver la liga. Podrá volver si le pasas un enlace de invitación.`,
        confirm: $localize`:@@league.admin.members.removeConfirm:Expulsar`,
        tone: 'danger',
      },
      (leagueId) =>
        this.leagueApi.removeMember(leagueId, member.userId).pipe(
          tap(() => {
            this.members.reload();
            this.toasts.show({
              message: $localize`:@@league.admin.members.removed:${name}:name: ya no está en la liga`,
            });
          }),
        ),
    );
  }

  /** Asks to confirm and runs the action; a `409` explains itself in a toast. */
  private act(
    action: 'regenerate' | 'revoke' | 'remove',
    question: Parameters<Confirm['ask']>[0],
    run: (leagueId: string) => Observable<unknown>,
  ): void {
    const leagueId = this.leagueId();
    if (leagueId === undefined || this.busy() !== null) {
      return;
    }
    this.confirm
      .ask(question)
      .pipe(
        filter(Boolean),
        tap(() => {
          this.busy.set(action);
        }),
        switchMap(() => run(leagueId)),
      )
      .subscribe({
        next: () => {
          this.busy.set(null);
        },
        error: (error: unknown) => {
          this.busy.set(null);
          const code = toProblem(error)?.code;
          if (code) {
            this.toasts.show({ message: ERROR_TEXTS[code], tone: 'danger' });
          }
        },
      });
  }
}
