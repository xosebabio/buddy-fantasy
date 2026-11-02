import type {
  MatchAuditEntryResponse,
  MatchSnapshotResponse,
  PlayerPerformanceResponse,
} from '../../../core/api/match/match-api';
import { formatInstant } from '../../../core/time/madrid-time';
import { VENUE_TEXTS } from '../../../shared/match/venue';
import { performanceSummary } from '../../../shared/performance/performance';

/** One thing that changed, as a phrase: `text`, with the previous value struck out and the new one after it. */
export interface AuditChange {
  readonly text: string;
  readonly before: string | null;
  readonly after: string | null;
}

type Action = MatchAuditEntryResponse['action'];

/** What each kind of change is called in the list (FE-23: a new action breaks the build). */
export const AUDIT_ACTION_TEXTS: Record<Action, string> = {
  SCHEDULED: $localize`:@@match.admin.audit.scheduled:Programó el partido`,
  DETAILS_UPDATED: $localize`:@@match.admin.audit.detailsUpdated:Cambió el rival o el campo`,
  RESCHEDULED: $localize`:@@match.admin.audit.rescheduled:Aplazó el partido`,
  SHEET_RECORDED: $localize`:@@match.admin.audit.sheetRecorded:Guardó el acta`,
  CLOSED: $localize`:@@match.admin.audit.closed:Cerró el partido`,
  CORRECTED: $localize`:@@match.admin.audit.corrected:Corrigió el acta`,
  DELETED: $localize`:@@match.admin.audit.deleted:Borró el partido`,
};

type StatKey =
  'goals' | 'penaltyGoals' | 'assists' | 'ownGoals' | 'penaltiesSaved' | 'penaltiesMissed';
type CardKey = 'yellowCard' | 'doubleYellow' | 'redCard';

const STAT_LABELS: Record<StatKey, string> = {
  goals: $localize`:@@match.admin.audit.goals:goles`,
  penaltyGoals: $localize`:@@match.admin.audit.penaltyGoals:goles de penalti`,
  assists: $localize`:@@match.admin.audit.assists:asistencias`,
  ownGoals: $localize`:@@match.admin.audit.ownGoals:goles en propia`,
  penaltiesSaved: $localize`:@@match.admin.audit.penaltiesSaved:penaltis parados`,
  penaltiesMissed: $localize`:@@match.admin.audit.penaltiesMissed:penaltis fallados`,
};

const CARD_LABELS: Record<CardKey, string> = {
  yellowCard: $localize`:@@match.admin.audit.yellow:amarilla`,
  doubleYellow: $localize`:@@match.admin.audit.doubleYellow:doble amarilla`,
  redCard: $localize`:@@match.admin.audit.red:roja directa`,
};

const STAT_KEYS: readonly StatKey[] = [
  'goals',
  'penaltyGoals',
  'assists',
  'ownGoals',
  'penaltiesSaved',
  'penaltiesMissed',
];
const CARD_KEYS: readonly CardKey[] = ['yellowCard', 'doubleYellow', 'redCard'];

const YES = $localize`:@@match.admin.audit.yes:sí`;
const NO = $localize`:@@match.admin.audit.no:no`;
const NO_RESULT = $localize`:@@match.admin.audit.noResult:sin resultado`;

function scoreOf(snapshot: MatchSnapshotResponse): string {
  return snapshot.result === null
    ? NO_RESULT
    : `${String(snapshot.result.goalsFor)}–${String(snapshot.result.goalsAgainst)}`;
}

function change(text: string, before: string | null = null, after: string | null = null) {
  return { text, before, after };
}

/** Every fact of a snapshot, for the creation and the deletion, which have nothing to compare to. */
function describe(
  snapshot: MatchSnapshotResponse,
  nameOf: (playerId: string) => string,
): AuditChange[] {
  return [
    change($localize`:@@match.admin.audit.matchday:Jornada: ${snapshot.matchday}:matchday:`),
    change(
      $localize`:@@match.admin.audit.kickoff:Hora: ${formatInstant(snapshot.kickoffAt, 'dateTime')}:kickoff:`,
    ),
    change($localize`:@@match.admin.audit.opponent:Rival: ${snapshot.opponent}:opponent:`),
    change($localize`:@@match.admin.audit.venue:Campo: ${VENUE_TEXTS[snapshot.venue]}:venue:`),
    ...(snapshot.result === null
      ? []
      : [change($localize`:@@match.admin.audit.result:Resultado: ${scoreOf(snapshot)}:score:`)]),
    ...snapshot.performances.map((performance) => {
      const summary = performanceSummary(performance).join(', ');
      return change(
        summary === ''
          ? $localize`:@@match.admin.audit.played:${nameOf(performance.playerId)}:name: jugó`
          : `${nameOf(performance.playerId)}: ${summary}`,
      );
    }),
  ];
}

function playerChanges(
  before: PlayerPerformanceResponse,
  after: PlayerPerformanceResponse,
  name: string,
): AuditChange[] {
  return [
    ...STAT_KEYS.filter((key) => before[key] !== after[key]).map((key) =>
      change(`${name}: ${STAT_LABELS[key]}`, String(before[key]), String(after[key])),
    ),
    ...CARD_KEYS.filter((key) => before[key] !== after[key]).map((key) =>
      change(`${name}: ${CARD_LABELS[key]}`, before[key] ? YES : NO, after[key] ? YES : NO),
    ),
  ];
}

/**
 * What a change did, in phrases (RF-16, RNF-04): the creation and the deletion tell the whole state;
 * any other change tells only what differs between `before` and `after`: the matchday, the time,
 * the opponent, the venue, the score, who comes in or out of the sheet and the stats that changed.
 * `nameOf` names a player of the sheet.
 */
export function auditChanges(
  before: MatchSnapshotResponse | null,
  after: MatchSnapshotResponse | null,
  nameOf: (playerId: string) => string,
): AuditChange[] {
  if (before === null || after === null) {
    const snapshot = before ?? after;
    return snapshot === null ? [] : describe(snapshot, nameOf);
  }
  const previous = new Map(before.performances.map((p) => [p.playerId, p]));
  const next = new Map(after.performances.map((p) => [p.playerId, p]));
  const changes: AuditChange[] = [];
  if (before.matchday !== after.matchday) {
    changes.push(
      change(
        $localize`:@@match.admin.audit.matchdayChange:Jornada`,
        String(before.matchday),
        String(after.matchday),
      ),
    );
  }
  if (before.kickoffAt !== after.kickoffAt) {
    changes.push(
      change(
        $localize`:@@match.admin.audit.kickoffChange:Hora`,
        formatInstant(before.kickoffAt, 'dateTime'),
        formatInstant(after.kickoffAt, 'dateTime'),
      ),
    );
  }
  if (before.opponent !== after.opponent) {
    changes.push(
      change($localize`:@@match.admin.audit.opponentChange:Rival`, before.opponent, after.opponent),
    );
  }
  if (before.venue !== after.venue) {
    changes.push(
      change(
        $localize`:@@match.admin.audit.venueChange:Campo`,
        VENUE_TEXTS[before.venue],
        VENUE_TEXTS[after.venue],
      ),
    );
  }
  if (scoreOf(before) !== scoreOf(after)) {
    changes.push(
      change(
        $localize`:@@match.admin.audit.resultChange:Resultado`,
        scoreOf(before),
        scoreOf(after),
      ),
    );
  }
  for (const [playerId, performance] of next) {
    const was = previous.get(playerId);
    if (was === undefined) {
      changes.push(
        change($localize`:@@match.admin.audit.enters:${nameOf(playerId)}:name: entra en el acta`),
      );
    } else {
      changes.push(...playerChanges(was, performance, nameOf(playerId)));
    }
  }
  for (const playerId of previous.keys()) {
    if (!next.has(playerId)) {
      changes.push(
        change($localize`:@@match.admin.audit.leaves:${nameOf(playerId)}:name: sale del acta`),
      );
    }
  }
  return changes;
}
