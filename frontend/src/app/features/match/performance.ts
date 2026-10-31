import type { PlayerPerformanceResponse } from '../../core/api/match/match-api';

type Counts = Pick<
  PlayerPerformanceResponse,
  'goals' | 'penaltyGoals' | 'assists' | 'ownGoals' | 'penaltiesSaved' | 'penaltiesMissed'
>;

function count(amount: number, one: string, many: string): string[] {
  return amount === 0 ? [] : [`${String(amount)} ${amount === 1 ? one : many}`];
}

/**
 * What a player did in the match, as the sheet records it, in text: goals, assists, penalties and
 * cards. Nothing if they only played.
 */
export function performanceSummary(performance: PlayerPerformanceResponse): string[] {
  const counts: Counts = performance;
  return [
    ...count(
      counts.goals,
      $localize`:@@match.sheet.goal:gol`,
      $localize`:@@match.sheet.goals:goles`,
    ),
    ...count(
      counts.penaltyGoals,
      $localize`:@@match.sheet.penaltyGoal:gol de penalti`,
      $localize`:@@match.sheet.penaltyGoals:goles de penalti`,
    ),
    ...count(
      counts.assists,
      $localize`:@@match.sheet.assist:asistencia`,
      $localize`:@@match.sheet.assists:asistencias`,
    ),
    ...count(
      counts.ownGoals,
      $localize`:@@match.sheet.ownGoal:gol en propia puerta`,
      $localize`:@@match.sheet.ownGoals:goles en propia puerta`,
    ),
    ...count(
      counts.penaltiesSaved,
      $localize`:@@match.sheet.penaltySaved:penalti parado`,
      $localize`:@@match.sheet.penaltiesSaved:penaltis parados`,
    ),
    ...count(
      counts.penaltiesMissed,
      $localize`:@@match.sheet.penaltyMissed:penalti fallado`,
      $localize`:@@match.sheet.penaltiesMissed:penaltis fallados`,
    ),
    ...(performance.yellowCard ? [$localize`:@@match.sheet.yellow:tarjeta amarilla`] : []),
    ...(performance.doubleYellow ? [$localize`:@@match.sheet.doubleYellow:doble amarilla`] : []),
    ...(performance.redCard ? [$localize`:@@match.sheet.red:roja directa`] : []),
  ];
}
