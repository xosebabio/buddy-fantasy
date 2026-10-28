import type { components } from '../api/schema';

export type ErrorCode = components['schemas']['ErrorCode'];

/**
 * What the user reads for each error code of a `400` or `409` (FE-08). It is exhaustive over the
 * spec's enum: a new code breaks the build until it has its text.
 */
export const ERROR_TEXTS: Readonly<Record<ErrorCode, string>> = {
  ADMIN_CANNOT_LEAVE: $localize`:@@core.errors.adminCannotLeave:El administrador no puede salir de la liga ni ser expulsado.`,
  DUPLICATE_PLAYER: $localize`:@@core.errors.duplicatePlayer:Hay un jugador repetido.`,
  FORMATION: $localize`:@@core.errors.formation:La alineación no llega al mínimo de jugadores de alguna posición.`,
  INACTIVE_PLAYER: $localize`:@@core.errors.inactivePlayer:Hay un jugador dado de baja: cámbialo por otro.`,
  INVALID_CARDS: $localize`:@@core.errors.invalidCards:Un jugador con doble amarilla no puede tener también la amarilla ni la roja directa.`,
  INVALID_DEFAULT_RATING: $localize`:@@core.errors.invalidDefaultRating:La nota por defecto no es válida para el formato de valoración.`,
  INVALID_GOALS: $localize`:@@core.errors.invalidGoals:El número de goles no es válido.`,
  INVALID_INITIAL_VALUE: $localize`:@@core.errors.invalidInitialValue:El valor inicial no es válido.`,
  INVALID_LEAGUE_NAME: $localize`:@@core.errors.invalidLeagueName:El nombre de la liga está vacío o es demasiado largo.`,
  INVALID_LINEUP_BUDGET: $localize`:@@core.errors.invalidLineupBudget:El presupuesto de la alineación no es válido.`,
  INVALID_LINEUP_LOCK_HOURS: $localize`:@@core.errors.invalidLineupLockHours:Las horas de bloqueo de la alineación no son válidas.`,
  INVALID_LINEUP_MAX_CHANGES: $localize`:@@core.errors.invalidLineupMaxChanges:El máximo de cambios por jornada no es válido.`,
  INVALID_LINEUP_MINIMUMS: $localize`:@@core.errors.invalidLineupMinimums:Los mínimos por posición no son válidos o suman más que la alineación.`,
  INVALID_LINEUP_SIZE: $localize`:@@core.errors.invalidLineupSize:El número de jugadores de la alineación no es válido.`,
  INVALID_MARKET_K: $localize`:@@core.errors.invalidMarketK:El factor del mercado no es válido.`,
  INVALID_MARKET_MAX_CHANGE: $localize`:@@core.errors.invalidMarketMaxChange:La variación máxima de valor no es válida.`,
  INVALID_MARKET_MIN_VALUE: $localize`:@@core.errors.invalidMarketMinValue:El valor mínimo de mercado no es válido.`,
  INVALID_MATCHDAY: $localize`:@@core.errors.invalidMatchday:La jornada no es válida.`,
  INVALID_OPPONENT: $localize`:@@core.errors.invalidOpponent:El nombre del rival está vacío o es demasiado largo.`,
  INVALID_PHOTO_FORMAT: $localize`:@@core.errors.invalidPhotoFormat:El formato de la foto no es válido.`,
  INVALID_PHOTO_SIZE: $localize`:@@core.errors.invalidPhotoSize:La foto está vacía o es demasiado grande.`,
  INVALID_PLAYER_NAME: $localize`:@@core.errors.invalidPlayerName:El nombre del jugador está vacío o es demasiado largo.`,
  INVALID_RATING: $localize`:@@core.errors.invalidRating:La valoración no es válida para el formato de la liga.`,
  INVALID_RATING_CONVERSION: $localize`:@@core.errors.invalidRatingConversion:La conversión de la valoración a puntos no es válida.`,
  INVALID_RATING_FORMAT: $localize`:@@core.errors.invalidRatingFormat:El formato de valoración no es válido.`,
  INVALID_RATING_MIN_VOTES: $localize`:@@core.errors.invalidRatingMinVotes:El mínimo de votos no es válido.`,
  INVALID_RATING_WINDOW_HOURS: $localize`:@@core.errors.invalidRatingWindowHours:Las horas para valorar no son válidas.`,
  INVALID_SHIRT_NUMBER: $localize`:@@core.errors.invalidShirtNumber:El dorsal no es válido.`,
  INVALID_STAT_COUNT: $localize`:@@core.errors.invalidStatCount:Alguna estadística no es válida.`,
  KICKOFF_TAKEN: $localize`:@@core.errors.kickoffTaken:Ya hay otro partido a esa hora.`,
  LINEUPS_NOT_LOCKED: $localize`:@@core.errors.lineupsNotLocked:Las alineaciones se verán cuando se bloqueen.`,
  LOCK_NOT_DUE: $localize`:@@core.errors.lockNotDue:Aún no es hora de bloquear las alineaciones.`,
  MATCHDAY_TAKEN: $localize`:@@core.errors.matchdayTaken:Ya hay otro partido en esa jornada.`,
  MATCH_CLOSED: $localize`:@@core.errors.matchClosed:El partido está cerrado: no se puede cambiar su jornada ni su hora, ni borrarlo.`,
  MATCH_NOT_CLOSED: $localize`:@@core.errors.matchNotClosed:El partido aún no está cerrado.`,
  MATCH_NOT_SCORED: $localize`:@@core.errors.matchNotScored:El partido aún no tiene puntos.`,
  MATCH_NOT_STARTED: $localize`:@@core.errors.matchNotStarted:El partido aún no ha empezado.`,
  MATCH_SHEET_INCOMPLETE: $localize`:@@core.errors.matchSheetIncomplete:El acta necesita el resultado y al menos un jugador.`,
  MAX_CHANGES_EXCEEDED: $localize`:@@core.errors.maxChangesExceeded:Has pasado del máximo de cambios de esta jornada.`,
  OVER_BUDGET: $localize`:@@core.errors.overBudget:La alineación pasa del presupuesto.`,
  PHOTO_NOT_UPLOADED: $localize`:@@core.errors.photoNotUploaded:La foto no se ha subido. Vuelve a intentarlo.`,
  PLAYER_NOT_IN_SHEET: $localize`:@@core.errors.playerNotInSheet:Solo se puede valorar a los jugadores del acta.`,
  PLAYER_RETIRED: $localize`:@@core.errors.playerRetired:El jugador está dado de baja.`,
  RATER_EXCLUDED: $localize`:@@core.errors.raterExcluded:El administrador ha anulado tus valoraciones de este partido.`,
  RATING_WINDOW_CLOSED: $localize`:@@core.errors.ratingWindowClosed:El plazo para valorar ya ha terminado.`,
  SCORING_TABLE_INCOMPLETE: $localize`:@@core.errors.scoringTableIncomplete:Faltan los puntos de alguna acción o posición.`,
  SHEET_TOO_LARGE: $localize`:@@core.errors.sheetTooLarge:El acta tiene demasiados jugadores.`,
  SHIRT_NUMBER_TAKEN: $localize`:@@core.errors.shirtNumberTaken:Ya hay otro jugador con ese dorsal.`,
  SIZE: $localize`:@@core.errors.size:La alineación no tiene el número de jugadores de la liga.`,
  UNKNOWN_PLAYER: $localize`:@@core.errors.unknownPlayer:Hay un jugador que no es de la liga.`,
};

export function isErrorCode(value: unknown): value is ErrorCode {
  return typeof value === 'string' && Object.hasOwn(ERROR_TEXTS, value);
}
