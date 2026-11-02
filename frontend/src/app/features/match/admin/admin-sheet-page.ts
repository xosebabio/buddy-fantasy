import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';

import { MatchApi } from '../../../core/api/match/match-api';
import { SquadApi, type PlayerResponse } from '../../../core/api/squad/squad-api';
import { ERROR_TEXTS } from '../../../core/http/error-texts';
import { Online } from '../../../core/http/online';
import { toProblem } from '../../../core/http/problem';
import { onVisible } from '../../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../../core/layout/unsaved-changes';
import { performanceSummary } from '../../../shared/performance/performance';
import { PlayerRow } from '../../../shared/player/player-row';
import { POSITION_GROUP_TEXTS } from '../../../shared/position/position';
import { Badge } from '../../../ui/badge/badge';
import { Button } from '../../../ui/button/button';
import { Checkbox } from '../../../ui/checkbox/checkbox';
import { Confirm } from '../../../ui/confirm/confirm';
import { LoadError } from '../../../ui/load-error/load-error';
import { Notice } from '../../../ui/notice/notice';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { Stepper } from '../../../ui/stepper/stepper';
import { Toasts } from '../../../ui/toast/toasts';
import {
  type CardKey,
  cardDisabled,
  EMPTY_PERFORMANCE,
  EMPTY_SHEET,
  missingMark,
  playerIds,
  removedPlayerIds,
  sameSheet,
  setCard,
  setPlayed,
  setStat,
  sheetErrorField,
  sheetGroups,
  STAT_KEYS,
  type StatKey,
  toSheetDraft,
  toSheetRequest,
  type SheetDraft,
} from './sheet-form';

/** The label of each counter of the sheet. */
const STAT_LABELS: Record<StatKey, string> = {
  goals: $localize`:@@match.admin.sheet.goals:Goles`,
  penaltyGoals: $localize`:@@match.admin.sheet.penaltyGoals:Goles de penalti`,
  assists: $localize`:@@match.admin.sheet.assists:Asistencias`,
  ownGoals: $localize`:@@match.admin.sheet.ownGoals:Goles en propia`,
  penaltiesSaved: $localize`:@@match.admin.sheet.penaltiesSaved:Penaltis parados`,
  penaltiesMissed: $localize`:@@match.admin.sheet.penaltiesMissed:Penaltis fallados`,
};

const CARD_LABELS: Record<CardKey, string> = {
  yellowCard: $localize`:@@match.admin.sheet.yellow:Amarilla`,
  doubleYellow: $localize`:@@match.admin.sheet.doubleYellow:Doble amarilla`,
  redCard: $localize`:@@match.admin.sheet.red:Roja directa`,
};

/**
 * The sheet of a match for the admin (`admin/matches/:matchId/sheet`, RF-13, RF-14, RF-16): the
 * score on top and, below, the squad by position with a box for who played; checking it opens the
 * counters and the cards of the player. Once the match is closed, saving corrects the sheet and
 * recalculates the points. The limits come from `GET /matches/limits`, the server validates the
 * rest, and its errors, which do not say which player they are about, go in the notice.
 */
@Component({
  selector: 'bf-admin-sheet-page',
  imports: [Badge, Button, Checkbox, LoadError, Notice, PlayerRow, Skeleton, Stepper],
  templateUrl: './admin-sheet-page.html',
  styleUrl: './admin-sheet-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSheetPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly api = inject(MatchApi);
  private readonly router = inject(Router);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly match = this.api.match(this.leagueId, this.matchId);
  protected readonly limits = this.api.limits(this.leagueId);
  protected readonly squad = inject(SquadApi).players(this.leagueId);

  protected readonly draft = signal<SheetDraft>(EMPTY_SHEET);
  /** The player whose counters are open; one at a time keeps the list short. */
  protected readonly open = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly markError = signal<'goalsFor' | 'goalsAgainst' | null>(null);
  protected readonly resultError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly groupTexts = POSITION_GROUP_TEXTS;
  protected readonly statKeys = STAT_KEYS;
  protected readonly statLabels = STAT_LABELS;
  protected readonly cardLabels = CARD_LABELS;
  protected readonly cardKeys: readonly CardKey[] = ['yellowCard', 'doubleYellow', 'redCard'];
  protected readonly empty = EMPTY_PERFORMANCE;

  protected readonly current = computed(() =>
    this.match.hasValue() ? this.match.value() : undefined,
  );
  protected readonly closed = computed(() => this.current()?.status === 'CLOSED');
  protected readonly failed = computed(
    () =>
      this.match.error() !== undefined ||
      this.limits.error() !== undefined ||
      this.squad.error() !== undefined,
  );
  protected readonly ready = computed(
    () => this.current() !== undefined && this.limits.hasValue() && this.squad.hasValue(),
  );

  /** What the server has. */
  private readonly saved = computed(() => {
    const current = this.current();
    return current === undefined ? EMPTY_SHEET : toSheetDraft(current);
  });
  protected readonly groups = computed(() =>
    sheetGroups(this.squad.hasValue() ? this.squad.value() : [], this.saved()),
  );
  protected readonly count = computed(() => playerIds(this.draft()).length);
  protected readonly full = computed(
    () => this.limits.hasValue() && this.count() >= this.limits.value().sheetPlayersMax,
  );
  protected readonly retiredIds = computed(
    () =>
      new Set(
        (this.squad.hasValue() ? this.squad.value() : [])
          .filter((player) => player.retiredAt !== null)
          .map((player) => player.id),
      ),
  );
  private readonly done = signal(false);

  readonly unsaved = computed(() => !this.done() && !sameSheet(this.draft(), this.saved()));

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      if (!this.unsaved()) {
        this.reload();
      }
    });
    // The draft takes what the server has each time it arrives, which drops what was typed.
    effect(() => {
      this.draft.set(this.saved());
    });
  }

  protected reload(): void {
    this.match.reload();
    this.limits.reload();
    this.squad.reload();
  }

  protected performanceOf(playerId: string): SheetDraft['players'][string] | undefined {
    return this.draft().players[playerId];
  }

  protected summary(player: PlayerResponse): string {
    const performance = this.performanceOf(player.id);
    return performance === undefined
      ? ''
      : performanceSummary({ ...performance, playerId: player.id, position: player.position }).join(
          ' · ',
        );
  }

  protected setMark(mark: 'goalsFor' | 'goalsAgainst', value: number | null): void {
    this.draft.update((draft) => ({ ...draft, [mark]: value }));
    this.markError.set(null);
    this.resultError.set(null);
  }

  protected setPlayed(playerId: string, played: boolean): void {
    this.draft.update((draft) => setPlayed(draft, playerId, played));
    this.open.set(played ? playerId : null);
  }

  protected toggle(playerId: string): void {
    this.open.update((open) => (open === playerId ? null : playerId));
  }

  protected setStat(playerId: string, key: StatKey, value: number | null): void {
    if (value !== null) {
      this.draft.update((draft) => setStat(draft, playerId, key, value));
    }
  }

  protected setCard(playerId: string, card: CardKey, checked: boolean): void {
    this.draft.update((draft) => setCard(draft, playerId, card, checked));
  }

  protected cardDisabled(playerId: string, card: CardKey): boolean {
    const performance = this.performanceOf(playerId);
    return performance !== undefined && cardDisabled(performance, card);
  }

  protected submit(): void {
    const leagueId = this.leagueId();
    const matchId = this.matchId();
    if (leagueId === undefined || matchId === undefined || this.saving()) {
      return;
    }
    const draft = this.draft();
    const missing = missingMark(draft);
    if (missing !== null) {
      this.markError.set(missing);
      this.notice.set(null);
      return;
    }
    const removed = this.closed() ? removedPlayerIds(this.saved(), draft) : [];
    if (removed.length === 0) {
      this.save(leagueId, matchId, draft);
      return;
    }
    const names = this.names(removed);
    this.confirm
      .ask({
        title: $localize`:@@match.admin.sheet.removeTitle:¿Guardar la corrección?`,
        message:
          removed.length === 1
            ? $localize`:@@match.admin.sheet.removeOne:${names}:names: sale del acta y pierde sus valoraciones.`
            : $localize`:@@match.admin.sheet.removeMany:${names}:names: salen del acta y pierden sus valoraciones.`,
        confirm: $localize`:@@match.admin.sheet.saveCorrectionConfirm:Guardar corrección`,
        tone: 'danger',
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          this.save(leagueId, matchId, draft);
        }
      });
  }

  private save(leagueId: string, matchId: string, draft: SheetDraft): void {
    this.saving.set(true);
    this.resultError.set(null);
    this.notice.set(null);
    const corrected = this.closed();
    this.api.recordSheet(leagueId, matchId, toSheetRequest(draft)).subscribe({
      next: () => {
        this.done.set(true);
        this.toasts.show({
          message: corrected
            ? $localize`:@@match.admin.sheet.corrected:Corrección guardada`
            : $localize`:@@match.admin.sheet.saved:Acta guardada`,
        });
        void this.router.navigate(['/leagues', leagueId, 'admin', 'matches', matchId]);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const code = toProblem(error)?.code;
        if (!code) {
          return;
        }
        if (sheetErrorField(code) === 'result') {
          this.resultError.set(ERROR_TEXTS[code]);
        } else {
          this.notice.set(ERROR_TEXTS[code]);
        }
      },
    });
  }

  /** The names of some players, such as «Pablo y Hugo»; the ones the squad does not list are skipped. */
  private names(ids: readonly string[]): string {
    const squad = this.squad.hasValue() ? this.squad.value() : [];
    const names = ids.flatMap((id) => squad.find((player) => player.id === id)?.name ?? []);
    const last = names.pop();
    return names.length === 0
      ? (last ?? '')
      : `${names.join(', ')} ${$localize`:@@match.admin.sheet.and:y`} ${last ?? ''}`;
  }
}
