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

import { LeagueApi } from '../../../core/api/league/league-api';
import { MatchApi, type MatchSummaryResponse, type Venue } from '../../../core/api/match/match-api';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { onVisible } from '../../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../../core/layout/unsaved-changes';
import { VENUE_TEXTS } from '../../../shared/match/venue';
import { Button } from '../../../ui/button/button';
import { Field, FieldInput } from '../../../ui/field/field';
import { LoadError } from '../../../ui/load-error/load-error';
import { Notice } from '../../../ui/notice/notice';
import { Segmented, type SegmentedOption } from '../../../ui/segmented/segmented';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Toasts } from '../../../ui/toast/toasts';
import {
  EMPTY_MATCH,
  invalidMatchFields,
  type MatchField,
  matchErrorField,
  postpones,
  proposeMatch,
  sameMatch,
  toMatchDraft,
  toMatchRequest,
} from './match-form';

/**
 * Schedules a match (`admin/matches/new`) or edits one (`admin/matches/:matchId/edit`), which
 * postpones it if the matchday or the time change (RF-12). A new match starts with the next
 * matchday and the time of the last one a week later, which are only suggestions. Day and time are
 * those of Madrid. Once the match is closed its matchday and time are read-only. The limits of the
 * fields come from `GET /matches/limits`; the rest the server validates and its error goes in the
 * control.
 */
@Component({
  selector: 'bf-admin-match-form-page',
  imports: [Button, Field, FieldInput, LoadError, Notice, ReactiveFormsModule, Segmented, Skeleton],
  templateUrl: './admin-match-form-page.html',
  styleUrl: './admin-match-form-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminMatchFormPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();
  /** Absent in `admin/matches/new`. */
  readonly matchId = input<string>();

  private readonly api = inject(MatchApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly isNew = computed(() => this.matchId() === undefined);
  protected readonly match = this.api.match(this.leagueId, this.matchId);
  /** The calendar, to suggest the next matchday; only when scheduling. */
  protected readonly matches = this.api.matches(() => (this.isNew() ? this.leagueId() : undefined));
  protected readonly limits = this.api.limits(this.leagueId);
  /** For the hint of the time; the form works without it. */
  protected readonly settings = inject(LeagueApi).settings(this.leagueId);
  protected readonly form = inject(NonNullableFormBuilder).group({ ...EMPTY_MATCH });
  protected readonly saving = signal(false);
  protected readonly fieldErrors = signal<Partial<Record<MatchField, string>>>({});
  protected readonly notice = signal<string | null>(null);

  protected readonly venueOptions: readonly SegmentedOption<Venue>[] = (
    ['HOME', 'AWAY'] as const
  ).map((value) => ({ value, label: VENUE_TEXTS[value] }));

  protected readonly current = computed<MatchSummaryResponse | undefined>(() =>
    this.match.hasValue() ? this.match.value() : undefined,
  );
  protected readonly closed = computed(() => this.current()?.status === 'CLOSED');
  protected readonly failed = computed(
    () =>
      this.match.error() !== undefined ||
      this.matches.error() !== undefined ||
      this.limits.error() !== undefined,
  );
  protected readonly ready = computed(() =>
    this.isNew()
      ? this.limits.hasValue() && this.matches.hasValue()
      : this.limits.hasValue() && this.current() !== undefined,
  );

  /** What the server has: the match, or the suggestion in the scheduling. */
  private readonly saved = computed(() => {
    const current = this.current();
    if (current !== undefined) {
      return toMatchDraft(current);
    }
    return this.matches.hasValue() ? proposeMatch(this.matches.value()) : EMPTY_MATCH;
  });
  private readonly value = toSignal(
    this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: EMPTY_MATCH },
  );
  private readonly done = signal(false);

  readonly unsaved = computed(() => !this.done() && !sameMatch(this.value(), this.saved()));
  protected readonly postponing = computed(
    () => !this.isNew() && !this.closed() && postpones(this.value(), this.saved()),
  );

  protected readonly opponentHint = computed(() =>
    this.limits.hasValue()
      ? $localize`:@@match.admin.form.opponentHint:Hasta ${this.limits.value().opponentMaxLength}:max: caracteres`
      : null,
  );
  protected readonly matchdayHint = computed(() =>
    this.closed()
      ? this.closedHint()
      : this.limits.hasValue()
        ? $localize`:@@match.admin.form.matchdayHint:Desde la jornada ${this.limits.value().matchdayMin}:min:`
        : null,
  );
  protected readonly timeHint = computed(() => {
    if (this.closed()) {
      return this.closedHint();
    }
    const lock = this.settings.hasValue() ? this.settings.value().lineupLockHours : undefined;
    return lock === undefined
      ? $localize`:@@match.admin.form.timeHint:Hora de Madrid.`
      : $localize`:@@match.admin.form.timeLockHint:Hora de Madrid. Las alineaciones se bloquean ${lock}:hours: h antes.`;
  });

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      if (!this.unsaved()) {
        this.reload();
      }
    });
    // The form takes what the server has each time it arrives, which drops what was typed.
    effect(() => {
      this.form.reset(this.saved());
    });
    effect(() => {
      const { matchday, date, time } = this.form.controls;
      for (const control of [matchday, date, time]) {
        if (this.closed()) {
          control.disable({ emitEvent: false });
        } else {
          control.enable({ emitEvent: false });
        }
      }
    });
  }

  protected reload(): void {
    this.match.reload();
    this.matches.reload();
    this.limits.reload();
    this.settings.reload();
  }

  protected setVenue(venue: Venue): void {
    this.form.controls.venue.setValue(venue);
    this.form.controls.venue.markAsDirty();
  }

  protected submit(): void {
    const leagueId = this.leagueId();
    const limits = this.limits.hasValue() ? this.limits.value() : undefined;
    if (leagueId === undefined || limits === undefined || this.saving()) {
      return;
    }
    const draft = this.form.getRawValue();
    const invalid = invalidMatchFields(draft, limits);
    if (invalid.length > 0) {
      this.fieldErrors.set(
        Object.fromEntries(
          invalid.map((field) => [
            field,
            field === 'matchday'
              ? $localize`:@@match.admin.form.matchdayInvalid:Escribe una jornada desde la ${limits.matchdayMin}:min:.`
              : $localize`:@@match.admin.form.required:Rellena este campo.`,
          ]),
        ),
      );
      this.notice.set(this.invalidNotice());
      return;
    }
    this.saving.set(true);
    this.fieldErrors.set({});
    this.notice.set(null);
    const matchId = this.matchId();
    const body = toMatchRequest(draft);
    const request =
      matchId === undefined
        ? this.api.schedule(leagueId, body)
        : this.api.update(leagueId, matchId, body);
    request.subscribe({
      next: (saved) => {
        this.done.set(true);
        this.toasts.show({
          message:
            matchId === undefined
              ? $localize`:@@match.admin.form.scheduled:Partido programado`
              : $localize`:@@match.admin.form.saved:Partido guardado`,
        });
        void this.router.navigate(['/leagues', leagueId, 'admin', 'matches', saved.id]);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.fail(error);
      },
    });
  }

  /** `MATCH_CLOSED` reloads the view and drops the edits; the other codes go in their control or the notice. */
  private fail(error: unknown): void {
    const code = toProblem(error)?.code;
    if (!code) {
      return;
    }
    if (code === 'MATCH_CLOSED') {
      this.toasts.show({ message: ERROR_TEXTS[code] });
      this.reload();
      return;
    }
    const field = matchErrorField(code);
    if (field === null) {
      this.notice.set(ERROR_TEXTS[code]);
    } else {
      this.fieldErrors.set({ [field]: ERROR_TEXTS[code] });
      this.notice.set(this.invalidNotice());
    }
  }

  private closedHint(): string {
    return $localize`:@@match.admin.form.closedHint:El partido está cerrado: la jornada y la hora ya no se pueden cambiar.`;
  }

  private invalidNotice(): string {
    return $localize`:@@match.admin.form.invalid:No se pudo guardar el partido. Revisa los campos marcados.`;
  }
}
