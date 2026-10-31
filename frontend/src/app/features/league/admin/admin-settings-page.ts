import { CdkAccordion } from '@angular/cdk/accordion';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';

import { LeagueApi, type LeagueSettingsBody } from '../../../core/api/league/league-api';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { onVisible } from '../../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../../core/layout/unsaved-changes';
import { BfPointsPipe } from '../../../shared/format/points';
import { PositionChip } from '../../../shared/position/position-chip';
import {
  POSITION_CLASSES,
  POSITION_TEXTS,
  POSITIONS,
  type Position,
} from '../../../shared/position/position';
import { type ScoringAction } from '../../../shared/scoring/scoring-action';
import { Button } from '../../../ui/button/button';
import { Disclosure } from '../../../ui/disclosure/disclosure';
import { Field, FieldInput } from '../../../ui/field/field';
import { List } from '../../../ui/list/list';
import { LoadError } from '../../../ui/load-error/load-error';
import { Notice } from '../../../ui/notice/notice';
import { Segmented, type SegmentedOption } from '../../../ui/segmented/segmented';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Stepper } from '../../../ui/stepper/stepper';
import { Switch } from '../../../ui/switch/switch';
import { Toasts } from '../../../ui/toast/toasts';
import {
  type RatingFormat,
  sameDraft,
  type SettingsDecimalKey,
  type SettingsDraft,
  type SettingsField,
  type SettingsNumberKey,
  settingsErrorField,
  toSettingsBody,
  toSettingsDraft,
  unparsableDecimals,
  withPoints,
} from './settings-form';

/** The highest mark of each format, fixed by the contract (`PlayerRatingRequest`), not by the league. */
const MAX_MARK: Record<RatingFormat, number> = { SPADES: 4, DECIMAL: 10 };

/**
 * The admin's settings and points table (RF-07, RF-15): a draft that is saved whole, as the
 * `PUT` replaces everything. The limits come from the server (decision 10 of plan 13) and so does
 * the validation: a `400` is painted under its control (decision 11).
 */
@Component({
  selector: 'bf-admin-settings-page',
  imports: [
    BfPointsPipe,
    Button,
    CdkAccordion,
    Disclosure,
    Field,
    FieldInput,
    List,
    LoadError,
    Notice,
    PositionChip,
    Segmented,
    Skeleton,
    Stepper,
    Switch,
  ],
  templateUrl: './admin-settings-page.html',
  styleUrl: './admin-settings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSettingsPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();

  private readonly leagueApi = inject(LeagueApi);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly settings = this.leagueApi.settings(this.leagueId);
  protected readonly limits = this.leagueApi.limits(this.leagueId);
  protected readonly saving = signal(false);
  /** The control errors and the notice of the last `400`, until the next edit. */
  protected readonly fieldErrors = signal<Partial<Record<SettingsField, string>>>({});
  protected readonly notice = signal<string | null>(null);

  protected readonly positions = POSITIONS;
  protected readonly positionClasses = POSITION_CLASSES;
  protected readonly positionTexts = POSITION_TEXTS;
  protected readonly formatOptions: readonly SegmentedOption<RatingFormat>[] = [
    { value: 'SPADES', label: $localize`:@@league.admin.settings.spades:Picas` },
    { value: 'DECIMAL', label: $localize`:@@league.admin.settings.decimal:Nota` },
  ];

  /** What the server has: the last answer to a save, or else the resource. */
  private readonly savedBody = signal<LeagueSettingsBody | null>(null);
  private readonly base = computed(
    () => this.savedBody() ?? (this.settings.hasValue() ? this.settings.value() : undefined),
  );
  private readonly saved = computed(() => {
    const base = this.base();
    return base === undefined ? undefined : toSettingsDraft(base);
  });
  private readonly edited = signal<SettingsDraft | null>(null);

  protected readonly draft = computed(() => this.edited() ?? this.saved());
  protected readonly failed = computed(
    () => this.settings.error() !== undefined || this.limits.error() !== undefined,
  );
  protected readonly lockMax = computed(() =>
    this.limits.hasValue() ? this.limits.value().lineupLockHoursMax : null,
  );
  protected readonly maxMark = computed(() => MAX_MARK[this.draft()?.ratingFormat ?? 'SPADES']);

  readonly unsaved = computed(() => {
    const edited = this.edited();
    const saved = this.saved();
    return edited !== null && saved !== undefined && !sameDraft(edited, saved);
  });

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      if (!this.unsaved()) {
        this.settings.reload();
        this.limits.reload();
      }
    });
  }

  protected reload(): void {
    this.settings.reload();
    this.limits.reload();
  }

  protected setNumber(key: SettingsNumberKey, value: number | null): void {
    if (value !== null) {
      this.change({ [key]: value });
    }
  }

  protected setDecimal(key: SettingsDecimalKey, event: Event): void {
    if (event.target instanceof HTMLInputElement) {
      this.change({ [key]: event.target.value });
    }
  }

  protected setFormation(enabled: boolean): void {
    this.change({ formationEnabled: enabled });
  }

  protected setBudget(enabled: boolean): void {
    this.change({ budgetEnabled: enabled });
  }

  /** A mark is in the scale of its format, so changing the format drops it. */
  protected setFormat(ratingFormat: RatingFormat): void {
    this.change({ ratingFormat, defaultRating: null });
  }

  protected setDefaultRating(enabled: boolean): void {
    this.change({ defaultRating: enabled ? 0 : null });
  }

  protected setMark(value: number | null): void {
    this.change({ defaultRating: value });
  }

  protected setPoints(action: ScoringAction, position: Position, value: number | null): void {
    const draft = this.draft();
    if (draft !== undefined && value !== null) {
      this.update(withPoints(draft, action, POSITIONS.indexOf(position), value));
    }
  }

  protected save(): void {
    const leagueId = this.leagueId();
    const draft = this.draft();
    const base = this.base();
    if (leagueId === undefined || draft === undefined || base === undefined || this.saving()) {
      return;
    }
    const invalid = unparsableDecimals(draft);
    if (invalid.length > 0) {
      this.showErrors(
        Object.fromEntries(
          invalid.map((field) => [
            field,
            $localize`:@@league.admin.settings.notNumber:Escribe un número.`,
          ]),
        ),
      );
      return;
    }
    this.saving.set(true);
    this.fieldErrors.set({});
    this.notice.set(null);
    this.leagueApi.updateSettings(leagueId, toSettingsBody(draft, base)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.savedBody.set(saved);
        this.edited.set(null);
        this.toasts.show({
          message: $localize`:@@league.admin.settings.saved:Parámetros guardados`,
        });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const code = toProblem(error)?.code;
        if (!code) {
          return;
        }
        const field = settingsErrorField(code);
        if (field === null) {
          this.notice.set(ERROR_TEXTS[code]);
        } else {
          this.showErrors({ [field]: ERROR_TEXTS[code] });
        }
      },
    });
  }

  private showErrors(errors: Partial<Record<SettingsField, string>>): void {
    this.fieldErrors.set(errors);
    this.notice.set(
      $localize`:@@league.admin.settings.invalid:No se pudieron guardar los parámetros. Revisa los campos marcados.`,
    );
  }

  private change(patch: Partial<SettingsDraft>): void {
    const draft = this.draft();
    if (draft !== undefined) {
      this.update({ ...draft, ...patch });
    }
  }

  private update(draft: SettingsDraft): void {
    this.edited.set(draft);
    this.fieldErrors.set({});
    this.notice.set(null);
  }
}
