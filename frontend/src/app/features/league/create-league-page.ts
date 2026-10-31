import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { map, switchMap } from 'rxjs';

import { LeagueApi } from '../../core/api/league/league-api';
import { SessionStore } from '../../core/auth/session-store';
import { ERROR_TEXTS } from '../../core/http/error-texts';
import { Online } from '../../core/http/online';
import { toProblem, violates } from '../../core/http/problem';
import { Button } from '../../ui/button/button';
import { Field, FieldInput } from '../../ui/field/field';
import { Toasts } from '../../ui/toast/toasts';
import { InvitationLink } from './invitation-link';

interface CreatedLeague {
  readonly id: string;
  readonly name: string;
  readonly invitationToken: string;
}

/**
 * Creates a league with the user as its admin (RF-05) and then shows its invitation link, which
 * the backend only gives now and when it is regenerated (RF-06). The name is checked by the server.
 */
@Component({
  selector: 'bf-create-league-page',
  imports: [Button, Field, FieldInput, InvitationLink, ReactiveFormsModule, RouterLink],
  templateUrl: './create-league-page.html',
  styleUrl: './create-league-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateLeaguePage {
  private readonly leagueApi = inject(LeagueApi);
  private readonly session = inject(SessionStore);
  private readonly toasts = inject(Toasts);
  private readonly injector = inject(Injector);
  private readonly inviteTitle = viewChild<ElementRef<HTMLElement>>('inviteTitle');

  protected readonly online = inject(Online);
  protected readonly form = inject(NonNullableFormBuilder).group({ name: '' });
  protected readonly saving = signal(false);
  protected readonly nameError = signal<string | null>(null);
  protected readonly created = signal<CreatedLeague | null>(null);

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    const { name } = this.form.getRawValue();
    this.saving.set(true);
    this.nameError.set(null);
    this.leagueApi
      .create({ name })
      .pipe(
        switchMap((league) =>
          this.session
            .reloadLeagues()
            .pipe(map(() => ({ id: league.id, invitationToken: league.invitationToken }))),
        ),
      )
      .subscribe({
        next: ({ id, invitationToken }) => {
          this.saving.set(false);
          const league = this.session.league(id);
          this.created.set({ id, name: league?.name ?? name, invitationToken });
          this.toasts.show({ message: $localize`:@@league.create.created:Liga creada` });
          // The form is gone: the focus goes to what replaces it.
          afterNextRender(() => this.inviteTitle()?.nativeElement.focus(), {
            injector: this.injector,
          });
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.nameError.set(nameError(error));
        },
      });
  }
}

/** The text of a `400` on the name; other failures are the interceptor's (FE-08). */
function nameError(error: unknown): string | null {
  const problem = toProblem(error);
  if (problem?.code) {
    return ERROR_TEXTS[problem.code];
  }
  if (violates(problem, 'name')) {
    return $localize`:@@league.create.nameRequired:Escribe el nombre de la liga.`;
  }
  return null;
}
