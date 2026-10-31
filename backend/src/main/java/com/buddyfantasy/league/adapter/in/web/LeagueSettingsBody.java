package com.buddyfantasy.league.adapter.in.web;

import java.math.BigDecimal;
import java.util.Map;

import com.buddyfantasy.datadefinition.Position;
import com.buddyfantasy.datadefinition.RatingFormat;
import com.buddyfantasy.datadefinition.ScoringAction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Wire format of a league's parameters, used by GET and PUT. Money values are in millions. The rating conversion of
 * each format is {@code points = factor × average + base}; the league uses the one of {@code ratingFormat}.
 * They are stored configuration: consumer modules read them as they arrive.
 */
@Schema(description = """
        Parámetros de una liga (RF-07), con los que nace desde `league.defaults`. Importes en millones. El `PUT` los \
        recibe todos, como los devuelve el `GET`.""")
record LeagueSettingsBody(
        @Schema(description = "Horas que dura la ventana de valoración desde el cierre del partido. Mínimo 1.",
                minimum = "1", example = "24")
        int ratingWindowHours,
        @Schema(description = "Formato de las valoraciones: `SPADES` (picas de 0 a 4) o `DECIMAL` (nota de 0 a 10).",
                example = "SPADES")
        RatingFormat ratingFormat,
        @Schema(description = "Factor de la conversión de picas: puntos = factor × media + base. Mayor que 0, con "
                + "hasta 4 cifras enteras y 2 decimales.", example = "4")
        BigDecimal ratingSpadesFactor,
        @Schema(description = "Base de la conversión de picas, con hasta 4 cifras enteras y 2 decimales.",
                example = "-2")
        BigDecimal ratingSpadesBase,
        @Schema(description = "Factor de la conversión de nota decimal: puntos = factor × media + base. Mayor que 0, "
                + "con hasta 4 cifras enteras y 2 decimales.", example = "1")
        BigDecimal ratingDecimalFactor,
        @Schema(description = "Base de la conversión de nota decimal, con hasta 4 cifras enteras y 2 decimales.",
                example = "0")
        BigDecimal ratingDecimalBase,
        @Schema(description = "Votos mínimos para que un jugador puntúe por valoración (RF-23). Mínimo 1.",
                minimum = "1", example = "1")
        int ratingMinVotes,
        @Schema(description = "Nota que se aplica a quien no llega al mínimo de votos, en el formato de la liga (de 0 "
                + "al máximo del formato); null para no aplicar ninguna (puntúa 0 por valoración).", nullable = true,
                example = "2")
        BigDecimal defaultRating,
        @Schema(description = "Horas antes del partido en que se bloquean las alineaciones. De 0 a "
                + "`league.limits.lineup-lock-hours-max`.", minimum = "0", example = "2")
        int lineupLockHours,
        @Schema(description = "Cambios por jornada contra la alineación bloqueada anterior. Mínimo 0.", minimum = "0",
                example = "2")
        int lineupMaxChanges,
        @Schema(description = "Jugadores de cada alineación. Mínimo 1.", minimum = "1", example = "7")
        int lineupSize,
        @Schema(description = "Si se exigen los mínimos por posición.", example = "true")
        boolean lineupFormationEnabled,
        @Schema(description = "Porteros mínimos, si se exige la formación. Mínimo 0; los cuatro mínimos no suman más "
                + "que `lineupSize`.", minimum = "0", example = "1")
        int lineupMinGoalkeepers,
        @Schema(description = "Defensas mínimos, si se exige la formación. Mínimo 0.", minimum = "0", example = "1")
        int lineupMinDefenders,
        @Schema(description = "Medios mínimos, si se exige la formación. Mínimo 0.", minimum = "0", example = "1")
        int lineupMinMidfielders,
        @Schema(description = "Delanteros mínimos, si se exige la formación. Mínimo 0.", minimum = "0", example = "1")
        int lineupMinForwards,
        @Schema(description = "Si las alineaciones tienen presupuesto máximo.", example = "true")
        boolean lineupBudgetEnabled,
        @Schema(description = "Presupuesto máximo de una alineación, en millones. Mayor que 0, aunque esté "
                + "desactivado.", exclusiveMinimum = true, minimum = "0", example = "100")
        BigDecimal lineupBudget,
        @Schema(description = "Sensibilidad `k` del valor de mercado a los puntos de la jornada. Mayor que 0.",
                exclusiveMinimum = true, minimum = "0", example = "0.05")
        BigDecimal marketK,
        @Schema(description = "Variación máxima del valor de mercado por jornada, en porcentaje. Mayor que 0 y como "
                + "mucho 100.", exclusiveMinimum = true, minimum = "0", maximum = "100", example = "10")
        BigDecimal marketMaxChangePct,
        @Schema(description = "Valor de mercado mínimo de un jugador, en millones. Mayor que 0.",
                exclusiveMinimum = true, minimum = "0", example = "1")
        BigDecimal marketMinValue,
        @Schema(description = "Puntos por acción y posición: la acción (`ScoringAction`) y después la posición como "
                + "claves. Tabla completa: las 9 acciones con las 4 posiciones cada una.", example = """
                {"GOAL": {"GOALKEEPER": 6, "DEFENDER": 5, "MIDFIELDER": 4, "FORWARD": 3},
                 "PENALTY_GOAL": {"GOALKEEPER": 3, "DEFENDER": 3, "MIDFIELDER": 3, "FORWARD": 3},
                 "PENALTY_SAVED": {"GOALKEEPER": 3, "DEFENDER": 3, "MIDFIELDER": 3, "FORWARD": 3},
                 "PENALTY_MISSED": {"GOALKEEPER": -2, "DEFENDER": -2, "MIDFIELDER": -2, "FORWARD": -2},
                 "ASSIST": {"GOALKEEPER": 1, "DEFENDER": 1, "MIDFIELDER": 1, "FORWARD": 1},
                 "OWN_GOAL": {"GOALKEEPER": -3, "DEFENDER": -3, "MIDFIELDER": -3, "FORWARD": -3},
                 "YELLOW_CARD": {"GOALKEEPER": -1, "DEFENDER": -1, "MIDFIELDER": -1, "FORWARD": -1},
                 "DOUBLE_YELLOW": {"GOALKEEPER": -3, "DEFENDER": -3, "MIDFIELDER": -3, "FORWARD": -3},
                 "RED_CARD": {"GOALKEEPER": -5, "DEFENDER": -5, "MIDFIELDER": -5, "FORWARD": -5}}""")
        Map<ScoringAction, Map<Position, Integer>> scoringRules) {
}
