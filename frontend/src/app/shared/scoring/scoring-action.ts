import type { components } from '../../core/api/schema';

export type ScoringAction = components['schemas']['ScoringAction'];

/** What each action is called, in the order of the points table (FE-23: a new one breaks the build). */
export const SCORING_ACTION_TEXTS: Record<ScoringAction, string> = {
  GOAL: $localize`:@@shared.scoringAction.goal:Gol`,
  PENALTY_GOAL: $localize`:@@shared.scoringAction.penaltyGoal:Gol de penalti`,
  PENALTY_SAVED: $localize`:@@shared.scoringAction.penaltySaved:Penalti parado`,
  PENALTY_MISSED: $localize`:@@shared.scoringAction.penaltyMissed:Penalti fallado`,
  ASSIST: $localize`:@@shared.scoringAction.assist:Asistencia`,
  OWN_GOAL: $localize`:@@shared.scoringAction.ownGoal:Gol en propia puerta`,
  YELLOW_CARD: $localize`:@@shared.scoringAction.yellowCard:Tarjeta amarilla`,
  DOUBLE_YELLOW: $localize`:@@shared.scoringAction.doubleYellow:Doble amarilla`,
  RED_CARD: $localize`:@@shared.scoringAction.redCard:Roja directa`,
};

/**
 * Whether a key of an `actions` or `scoringRules` map is an action: the spec cannot type the keys of
 * a map (decision 9 of plan 12), so they are checked before looking up their text.
 */
export function isScoringAction(key: string): key is ScoringAction {
  return Object.hasOwn(SCORING_ACTION_TEXTS, key);
}

export interface ActionPoints {
  readonly action: ScoringAction;
  readonly points: number;
}

/**
 * The actions of a map of the API with their points, in the order of the points table. A key that
 * is not an action, which the generated types would not know either, is left out.
 */
export function actionPoints(actions: Readonly<Record<string, number>>): ActionPoints[] {
  return Object.keys(SCORING_ACTION_TEXTS)
    .filter(isScoringAction)
    .flatMap((action) => {
      const points = actions[action];
      return points === undefined ? [] : [{ action, points }];
    });
}
