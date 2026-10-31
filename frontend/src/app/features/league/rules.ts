import type { LeagueSettingsBody } from '../../core/api/league/league-api';
import { formatMoney } from '../../shared/format/money';
import { POSITIONS, type Position } from '../../shared/position/position';
import {
  type ScoringAction,
  SCORING_ACTION_TEXTS,
  isScoringAction,
} from '../../shared/scoring/scoring-action';

export interface Rule {
  readonly label: string;
  readonly value: string;
}

/** The parameters of the league that a manager needs to know, in the order of the rules page (RF-07). */
export function leagueRules(settings: LeagueSettingsBody): Rule[] {
  const minimums: Record<Position, number> = {
    GOALKEEPER: settings.lineupMinGoalkeepers,
    DEFENDER: settings.lineupMinDefenders,
    MIDFIELDER: settings.lineupMinMidfielders,
    FORWARD: settings.lineupMinForwards,
  };
  const formation = POSITIONS.map((position) => minimums[position]).join('-');
  return [
    {
      label: $localize`:@@league.rules.size:Jugadores por alineación`,
      value: String(settings.lineupSize),
    },
    {
      label: $localize`:@@league.rules.lock:Bloqueo de la alineación`,
      value: $localize`:@@league.rules.lockValue:${settings.lineupLockHours}:hours: h antes del partido`,
    },
    {
      label: $localize`:@@league.rules.changes:Cambios por jornada`,
      value: String(settings.lineupMaxChanges),
    },
    {
      label: $localize`:@@league.rules.budget:Presupuesto`,
      value: settings.lineupBudgetEnabled
        ? formatMoney(settings.lineupBudget)
        : $localize`:@@league.rules.budgetOff:Sin límite`,
    },
    {
      label: $localize`:@@league.rules.formation:Mínimos por posición`,
      value: settings.lineupFormationEnabled
        ? $localize`:@@league.rules.formationOn:${formation}:formation: (porteros, defensas, medios y delanteros)`
        : $localize`:@@league.rules.formationOff:Libre`,
    },
    {
      label: $localize`:@@league.rules.format:Valoraciones`,
      value:
        settings.ratingFormat === 'SPADES'
          ? $localize`:@@league.rules.formatSpades:Picas`
          : $localize`:@@league.rules.formatDecimal:Nota decimal`,
    },
    {
      label: $localize`:@@league.rules.window:Ventana de valoración`,
      value: $localize`:@@league.rules.windowValue:${settings.ratingWindowHours}:hours: h`,
    },
  ];
}

export interface ScoringRow {
  readonly action: ScoringAction;
  readonly text: string;
  /** The points per position, in the order of `POSITIONS`. */
  readonly points: number[];
}

/**
 * The points table, one row per action and one column per position (RF-15). The spec cannot type
 * the keys of the maps (decision 9 of plan 12), so the actions are checked and a missing cell is 0.
 */
export function scoringRows(rules: LeagueSettingsBody['scoringRules']): ScoringRow[] {
  return Object.keys(SCORING_ACTION_TEXTS)
    .filter(isScoringAction)
    .map((action) => ({
      action,
      text: SCORING_ACTION_TEXTS[action],
      points: POSITIONS.map((position) => rules[action]?.[position] ?? 0),
    }));
}
