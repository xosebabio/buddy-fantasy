import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';

import { LeagueApi } from '../../core/api/league/league-api';
import { LineupApi, type MyLineupResponse } from '../../core/api/lineup/lineup-api';
import { SquadApi } from '../../core/api/squad/squad-api';
import { ERROR_TEXTS } from '../../core/http/error-texts';
import { Online } from '../../core/http/online';
import { toProblem } from '../../core/http/problem';
import { onVisible } from '../../core/http/visible';
import { type HasUnsavedChanges, warnBeforeUnload } from '../../core/layout/unsaved-changes';
import { BfMoneyPipe, formatMoney } from '../../shared/format/money';
import { LineupList } from '../../shared/lineup/lineup-list';
import { type LineupSpot, spotKey } from '../../shared/lineup/lineup-spots';
import { Pitch } from '../../shared/lineup/pitch';
import { POSITION_GROUP_TEXTS } from '../../shared/position/position';
import { playerIndex } from '../../shared/roster/roster';
import { BfDatePipe } from '../../shared/time/date-pipe';
import { Button } from '../../ui/button/button';
import { Countdown } from '../../ui/countdown/countdown';
import { Figure } from '../../ui/figure/figure';
import { LoadError } from '../../ui/load-error/load-error';
import { Notice } from '../../ui/notice/notice';
import { Segmented, type SegmentedOption } from '../../ui/segmented/segmented';
import { Sheet } from '../../ui/sheet/sheet';
import { Skeleton } from '../../ui/skeleton/skeleton';
import { Toasts } from '../../ui/toast/toasts';
import { candidateGroups } from './candidates';
import { type CandidateChoice, CandidatesSheet, type CandidatesData } from './candidates-sheet';
import { checkDraft, draftRules, lineupPlayer, sameLineup } from './draft';

type View = 'field' | 'list';

/**
 * The manager's lineup for the next match to lock (RF-24 to RF-29): a local draft that is edited by
 * touching a bib or a free place, in the pitch or in a list, and saved whole. Size, formation and
 * budget are shown as the draft changes, but only the server decides (decision 10 of plan 12).
 */
@Component({
  selector: 'bf-lineup-page',
  imports: [
    BfDatePipe,
    BfMoneyPipe,
    Button,
    Countdown,
    Figure,
    LineupList,
    LoadError,
    Notice,
    Pitch,
    Segmented,
    Skeleton,
  ],
  templateUrl: './lineup-page.html',
  styleUrl: './lineup-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LineupPage implements HasUnsavedChanges {
  readonly leagueId = input<string>();

  private readonly lineupApi = inject(LineupApi);
  private readonly sheet = inject(Sheet);
  private readonly toasts = inject(Toasts);

  protected readonly online = inject(Online);
  protected readonly lineup = this.lineupApi.myLineup(this.leagueId);
  protected readonly settings = inject(LeagueApi).settings(this.leagueId);
  protected readonly squad = inject(SquadApi).players(this.leagueId);

  /** The players being edited; `null` while the draft is the saved lineup. */
  private readonly edited = signal<string[] | null>(null);
  protected readonly view = signal<View>('field');
  protected readonly viewOptions: readonly SegmentedOption<View>[] = [
    { value: 'field', label: $localize`:@@lineup.lineup.viewField:Campo` },
    { value: 'list', label: $localize`:@@lineup.lineup.viewList:Lista` },
  ];
  protected readonly selected = signal<string | null>(null);
  protected readonly saving = signal(false);

  private readonly index = computed(() => playerIndex(this.squad.value() ?? []));
  private readonly ids = computed(
    () => this.edited() ?? this.lineup.value()?.players.map((player) => player.id) ?? [],
  );
  protected readonly players = computed(() =>
    this.ids().flatMap((id) => {
      const player = this.index().get(id);
      return player === undefined ? [] : [lineupPlayer(player)];
    }),
  );
  protected readonly check = computed(() => {
    const settings = this.settings.value();
    const lineup = this.lineup.value();
    if (settings === undefined || lineup === undefined) {
      return undefined;
    }
    const retired = new Set(
      [...this.index().values()].filter((p) => p.retiredAt !== null).map((p) => p.id),
    );
    return checkDraft(this.players(), draftRules(settings, lineup.budget), retired);
  });
  protected readonly groupTexts = POSITION_GROUP_TEXTS;
  protected readonly budgetLabel = $localize`:@@lineup.lineup.budget:Presupuesto`;
  protected readonly changesLabel = $localize`:@@lineup.lineup.changes:Cambios`;
  protected readonly noLimit = $localize`:@@lineup.lineup.noLimit:Sin límite`;
  protected readonly viewLabel = $localize`:@@lineup.lineup.view:Vista`;
  protected readonly lineupLabel = $localize`:@@lineup.lineup.label:Tu alineación`;

  readonly unsaved = computed(() => {
    const edited = this.edited();
    const saved = this.lineup.hasValue() ? this.lineup.value().players.map((p) => p.id) : [];
    return edited !== null && !sameLineup(edited, saved);
  });

  /** The match the lineup counts for when the page last looked, to tell when it locks under the user. */
  private target: MyLineupResponse['targetMatch'] = null;

  constructor() {
    warnBeforeUnload(this.unsaved);
    onVisible(() => {
      this.reload();
    });
    effect(() => {
      if (this.lineup.hasValue()) {
        this.noteTarget(this.lineup.value().targetMatch);
      }
    });
  }

  protected reload(): void {
    this.lineup.reload();
    this.squad.reload();
    this.settings.reload();
  }

  protected edit(spot: LineupSpot): void {
    const lineup = this.lineup.value();
    if (lineup === undefined) {
      return;
    }
    const changing = spot.kind === 'player' ? this.index().get(spot.player.id) : undefined;
    const check = this.check();
    const total = check?.total ?? 0;
    const left =
      lineup.budget === null ? null : lineup.budget - total + (changing?.currentValue ?? 0);
    const data: CandidatesData = {
      title:
        changing === undefined
          ? $localize`:@@lineup.lineup.addTitle:Añadir un jugador`
          : $localize`:@@lineup.lineup.changeTitle:Cambiar a ${changing.name}:name:`,
      left:
        left === null
          ? null
          : changing === undefined
            ? $localize`:@@lineup.lineup.leftWith:Te quedan ${formatMoney(left)}:left:`
            : $localize`:@@lineup.lineup.leftWithout:Sin él te quedan ${formatMoney(left)}:left:`,
      groups: candidateGroups(
        this.squad.value() ?? [],
        this.ids(),
        changing,
        spot.kind === 'free' ? spot.position : null,
        left,
      ),
      removable: changing !== undefined,
    };
    this.selected.set(spotKey(spot));
    this.sheet
      .open<CandidateChoice | undefined, CandidatesData>(CandidatesSheet, { data })
      .closed.subscribe((choice) => {
        this.selected.set(null);
        if (choice !== undefined) {
          this.apply(spot, choice);
        }
      });
  }

  private apply(spot: LineupSpot, choice: CandidateChoice): void {
    const ids = this.ids();
    const replaced = spot.kind === 'player' ? spot.player.id : null;
    if (choice.kind === 'remove') {
      this.edited.set(ids.filter((id) => id !== replaced));
    } else if (replaced === null) {
      this.edited.set([...ids, choice.playerId]);
    } else {
      this.edited.set(ids.map((id) => (id === replaced ? choice.playerId : id)));
    }
  }

  protected discard(): void {
    this.edited.set(null);
  }

  protected save(): void {
    const leagueId = this.leagueId();
    const edited = this.edited();
    if (leagueId === undefined || edited === null || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.lineupApi.save(leagueId, { playerIds: edited }).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.edited.set(null);
        this.lineup.set(saved);
        this.toasts.show({ message: $localize`:@@lineup.lineup.saved:Alineación guardada` });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const code = toProblem(error)?.code;
        if (code) {
          this.toasts.show({ message: ERROR_TEXTS[code], tone: 'danger' });
          this.reload();
        }
      },
    });
  }

  /** Explains it when the match the lineup counts for is no longer the one the user was looking at. */
  private noteTarget(next: MyLineupResponse['targetMatch']): void {
    const previous = this.target;
    this.target = next;
    if (previous === null || previous.id === next?.id) {
      return;
    }
    const locked = previous.matchday;
    this.toasts.show({
      message:
        next === null
          ? $localize`:@@lineup.lineup.lockedNone:La jornada ${locked}:locked: ya se ha bloqueado.`
          : $localize`:@@lineup.lineup.lockedNext:La jornada ${locked}:locked: ya se ha bloqueado. Lo que guardes cuenta para la ${next.matchday}:next:.`,
    });
  }
}
