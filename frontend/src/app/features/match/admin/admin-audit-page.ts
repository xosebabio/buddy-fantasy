import { CdkAccordion } from '@angular/cdk/accordion';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LeagueApi } from '../../../core/api/league/league-api';
import { MatchApi } from '../../../core/api/match/match-api';
import { SquadApi } from '../../../core/api/squad/squad-api';
import { onVisible } from '../../../core/http/visible';
import { memberIndex, memberName, playerIndex } from '../../../shared/roster/roster';
import { BfDatePipe } from '../../../shared/time/date-pipe';
import { EmptyState } from '../../../ui/empty-state/empty-state';
import { List } from '../../../ui/list/list';
import { Disclosure } from '../../../ui/disclosure/disclosure';
import { LoadError } from '../../../ui/load-error/load-error';
import { Skeleton } from '../../../ui/skeleton/skeleton';
import { AUDIT_ACTION_TEXTS, auditChanges } from './audit-changes';

/**
 * The audit of a match (`admin/matches/:matchId/audit`, RF-16, RNF-04): what the admin changed,
 * the latest first, one row per change. The row says what was done, by whom and when; opened, it
 * tells in phrases what changed. The ids of players and users are crossed with the squad and the
 * members.
 */
@Component({
  selector: 'bf-admin-audit-page',
  imports: [BfDatePipe, CdkAccordion, Disclosure, EmptyState, List, LoadError, Skeleton],
  templateUrl: './admin-audit-page.html',
  styleUrl: './admin-audit-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAuditPage {
  readonly leagueId = input<string>();
  readonly matchId = input<string>();

  private readonly api = inject(MatchApi);

  protected readonly match = this.api.match(this.leagueId, this.matchId);
  protected readonly audit = this.api.audit(this.leagueId, this.matchId);
  private readonly squad = inject(SquadApi).players(this.leagueId);
  private readonly members = inject(LeagueApi).members(this.leagueId);

  protected readonly actionTexts = AUDIT_ACTION_TEXTS;
  protected readonly failed = computed(
    () =>
      this.match.error() !== undefined ||
      this.audit.error() !== undefined ||
      this.squad.error() !== undefined ||
      this.members.error() !== undefined,
  );
  protected readonly current = computed(() =>
    this.match.hasValue() ? this.match.value() : undefined,
  );
  protected readonly ready = computed(
    () =>
      this.current() !== undefined &&
      this.audit.hasValue() &&
      this.squad.hasValue() &&
      this.members.hasValue(),
  );

  /** The latest first, as the server sends them, with their phrases ready. */
  protected readonly rows = computed(() => {
    if (!this.audit.hasValue() || !this.squad.hasValue() || !this.members.hasValue()) {
      return [];
    }
    const players = playerIndex(this.squad.value());
    const members = memberIndex(this.members.value());
    const nameOf = (id: string) => players.get(id)?.name ?? id;
    return this.audit.value().map((entry) => ({
      action: entry.action,
      who: memberName(members, entry.actorUserId),
      at: entry.occurredAt,
      changes: auditChanges(entry.before, entry.after, nameOf),
    }));
  });

  constructor() {
    onVisible(() => {
      this.reload();
    });
  }

  protected reload(): void {
    this.match.reload();
    this.audit.reload();
    this.squad.reload();
    this.members.reload();
  }
}
