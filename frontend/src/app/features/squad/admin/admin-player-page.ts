import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { map } from 'rxjs';

import { SquadApi, type PlayerResponse } from '../../../core/api/squad/squad-api';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { onVisible } from '../../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../../core/layout/unsaved-changes';
import { Bib } from '../../../shared/bib/bib';
import { POSITION_TEXTS, POSITIONS, type Position } from '../../../shared/position/position';
import { PositionChip } from '../../../shared/position/position-chip';
import { Badge } from '../../../ui/badge/badge';
import { Button } from '../../../ui/button/button';
import { Confirm } from '../../../ui/confirm/confirm';
import { Field, FieldInput } from '../../../ui/field/field';
import { LoadError } from '../../../ui/load-error/load-error';
import { Notice } from '../../../ui/notice/notice';
import { Segmented, type SegmentedOption } from '../../../ui/segmented/segmented';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Toasts } from '../../../ui/toast/toasts';
import {
  EMPTY_PLAYER,
  invalidPlayerFields,
  type PlayerField,
  playerErrorField,
  samePlayer,
  toPlayerDraft,
  toPlayerRequest,
} from './player-form';

/**
 * Registers a player (`admin/squad/new`) or edits one (`admin/squad/:playerId`) (RF-09, RF-10), and
 * retires them from there. A retired player is shown read-only: there is no way back. The limits of
 * the fields come from `GET /players/limits`; the rest the server validates and its error goes in
 * the control.
 */
@Component({
  selector: 'bf-admin-player-page',
  imports: [
    Badge,
    Bib,
    Button,
    Field,
    FieldInput,
    LoadError,
    Notice,
    PositionChip,
    ReactiveFormsModule,
    Segmented,
    Skeleton,
  ],
  templateUrl: './admin-player-page.html',
  styleUrl: './admin-player-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminPlayerPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();
  /** Absent in `admin/squad/new`. */
  readonly playerId = input<string>();

  private readonly api = inject(SquadApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly player = this.api.player(this.leagueId, this.playerId);
  protected readonly limits = this.api.limits(this.leagueId);
  protected readonly form = inject(NonNullableFormBuilder).group({ ...EMPTY_PLAYER });
  protected readonly saving = signal(false);
  protected readonly fieldErrors = signal<Partial<Record<PlayerField, string>>>({});
  protected readonly notice = signal<string | null>(null);

  protected readonly positionTexts = POSITION_TEXTS;
  protected readonly positionOptions: readonly SegmentedOption<Position>[] = POSITIONS.map(
    (value) => ({ value, label: POSITION_TEXTS[value] }),
  );

  protected readonly isNew = computed(() => this.playerId() === undefined);
  protected readonly current = computed<PlayerResponse | undefined>(() =>
    this.player.hasValue() ? this.player.value() : undefined,
  );
  protected readonly failed = computed(
    () => this.player.error() !== undefined || this.limits.error() !== undefined,
  );
  protected readonly ready = computed(
    () => this.limits.hasValue() && (this.isNew() || this.current() !== undefined),
  );
  protected readonly retired = computed(() => (this.current()?.retiredAt ?? null) !== null);

  /** What the server has: the player, or an empty draft in the registration. */
  private readonly saved = computed(() => {
    const current = this.current();
    return current === undefined ? EMPTY_PLAYER : toPlayerDraft(current);
  });
  private readonly value = toSignal(
    this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: EMPTY_PLAYER },
  );
  private readonly done = signal(false);

  readonly unsaved = computed(() => !this.done() && !samePlayer(this.value(), this.saved()));

  protected readonly nameHint = computed(() =>
    this.limits.hasValue()
      ? $localize`:@@squad.admin.player.nameHint:Hasta ${this.limits.value().nameMaxLength}:max: caracteres`
      : null,
  );
  protected readonly shirtHint = computed(() =>
    this.limits.hasValue()
      ? $localize`:@@squad.admin.player.shirtHint:Opcional, entre ${this.limits.value().shirtNumberMin}:min: y ${this.limits.value().shirtNumberMax}:max:`
      : null,
  );

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      if (!this.unsaved()) {
        this.player.reload();
        this.limits.reload();
      }
    });
    // The form takes what the server has each time it arrives, which drops what was typed.
    effect(() => {
      this.form.reset(this.saved());
    });
    effect(() => {
      if (this.retired()) {
        this.form.disable({ emitEvent: false });
      }
    });
  }

  protected reload(): void {
    this.player.reload();
    this.limits.reload();
  }

  protected setPosition(position: Position): void {
    this.form.controls.position.setValue(position);
    this.form.controls.position.markAsDirty();
  }

  protected submit(): void {
    const leagueId = this.leagueId();
    const limits = this.limits.hasValue() ? this.limits.value() : undefined;
    if (leagueId === undefined || limits === undefined || this.saving() || this.retired()) {
      return;
    }
    const draft = this.form.getRawValue();
    const invalid = invalidPlayerFields(draft, limits);
    if (invalid.length > 0) {
      this.fieldErrors.set(
        Object.fromEntries(
          invalid.map((field) => [
            field,
            field === 'shirtNumber'
              ? $localize`:@@squad.admin.player.shirtNumberInvalid:Escribe un dorsal entre ${limits.shirtNumberMin}:min: y ${limits.shirtNumberMax}:max:.`
              : $localize`:@@squad.admin.player.notNumber:Escribe un número.`,
          ]),
        ),
      );
      this.notice.set(this.invalidNotice());
      return;
    }
    this.saving.set(true);
    this.fieldErrors.set({});
    this.notice.set(null);
    const playerId = this.playerId();
    const body = toPlayerRequest(draft);
    const request =
      playerId === undefined
        ? this.api.register(leagueId, body)
        : this.api.update(leagueId, playerId, body);
    request.subscribe({
      next: () => {
        this.finish(
          playerId === undefined
            ? $localize`:@@squad.admin.player.registered:Jugador dado de alta`
            : $localize`:@@squad.admin.player.saved:Jugador guardado`,
        );
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.fail(error);
      },
    });
  }

  protected retire(): void {
    const leagueId = this.leagueId();
    const playerId = this.playerId();
    const current = this.current();
    if (
      leagueId === undefined ||
      playerId === undefined ||
      current === undefined ||
      this.saving()
    ) {
      return;
    }
    this.confirm
      .ask({
        title: $localize`:@@squad.admin.player.retireTitle:Dar de baja a ${current.name}:name:`,
        message: $localize`:@@squad.admin.player.retireMessage:Sigue en las actas, los puntos y los historiales. Sale de las alineaciones, y sustituirlo no gasta cambio. No se puede reactivar.`,
        confirm: $localize`:@@squad.admin.player.retire:Dar de baja`,
        tone: 'danger',
      })
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.saving.set(true);
        this.api.retire(leagueId, playerId).subscribe({
          next: () => {
            this.finish($localize`:@@squad.admin.player.retired:Jugador dado de baja`);
          },
          error: (error: unknown) => {
            this.saving.set(false);
            this.fail(error);
          },
        });
      });
  }

  /** Back to the squad with the toast; the draft is no longer pending, so the guard lets it go. */
  private finish(message: string): void {
    this.done.set(true);
    this.toasts.show({ message });
    void this.router.navigate(['/leagues', this.leagueId(), 'admin', 'squad']);
  }

  /** A `409` by state reloads the view and drops the edits; the other codes go in their control or the notice. */
  private fail(error: unknown): void {
    const code = toProblem(error)?.code;
    if (!code) {
      return;
    }
    if (code === 'PLAYER_RETIRED') {
      this.toasts.show({ message: ERROR_TEXTS[code] });
      this.reload();
      return;
    }
    const field = playerErrorField(code);
    if (field === null) {
      this.notice.set(ERROR_TEXTS[code]);
    } else {
      this.fieldErrors.set({ [field]: ERROR_TEXTS[code] });
      this.notice.set(this.invalidNotice());
    }
  }

  private invalidNotice(): string {
    return $localize`:@@squad.admin.player.invalid:No se pudo guardar el jugador. Revisa los campos marcados.`;
  }
}
