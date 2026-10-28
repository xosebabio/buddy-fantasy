# 08 · Puntos y clasificaciones (`scoring`) — plan

Quinto y último ítem de la **Fase 1 · Núcleo jugable** (`requirements.md`): *cálculo de puntos por acción según posición y clasificaciones* (RF-30 a RF-33). Cierra también el cálculo de RF-15 (que `match` dejó pendiente), los puntos del manager de RF-31 (incluido el 0 de una alineación fuera de presupuesto, que `lineup` dejó pendiente) y la parte de historial de puntos de la ficha del jugador (RF-11). Se apoya en `match` (`MatchClosed`, `MatchCorrected`, `MatchDeleted`), en `lineup` (`LineupsLocked`, `LineupsUnlocked`), en `league` (roles, miembros y tabla de puntos) y en `squad` (jugadores). Cumple los contratos de `scoring` del plan 07, §4.3.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Jornada | 1 jornada = 1 partido (plan 05). Se puntúa por partido; la clasificación por jornada es la de ese partido. |
| Puntos por acciones (RF-15) | Para cada jugador **del acta**: Σ cantidad × puntos de la tabla de la liga para (acción, posición del acta). El acta ya separa goles y goles de penalti y excluye la amarilla simple cuando hay doble amarilla, así que el gol de penalti vale +3 sin sumar el gol normal y la doble amarilla −3 en total sin lógica especial. La posición es la foto que guarda el acta, no la actual de `squad`. |
| Puntos por valoración (RF-30) | **0 en esta fase.** El modelo guarda `action_points` y `rating_points` por separado; `rating` (Fase 2, RF-22, RF-23) rellenará los segundos. Sin pesos ni porcentajes: puntos = acciones + valoración. |
| Jugadores puntuados | Todos los del acta, estén alineados o no: hacen falta para la ficha (RF-11) y para la media de la jornada de `market` (RF-35). Quien no está en el acta no tiene fila y suma 0. |
| Puntos del manager (RF-31) | Suma de los puntos de los jugadores de su **alineación bloqueada** (la copia de `LineupsLocked`). Un alineado que no jugó suma 0, también si está dado de baja: los puntos salen solo del acta (decisión 11). Alineación `overBudget` → **0 puntos** esa jornada, con la marca visible. Sin alineación bloqueada → **0 puntos** y aparece en la clasificación de la jornada sin jugadores (decisión 9). |
| Cuándo se puntúa | Cuando `scoring` tiene **el acta (`MatchClosed`) y las alineaciones (`LineupsLocked`) del mismo partido**, lleguen en el orden que lleguen. Hasta entonces el partido no aparece en las clasificaciones. |
| Tabla de puntos | La de la liga (`league_scoring_rule`), leída por `LeagueSettingsProvider` al puntuar el partido por primera vez y **congelada** en `scoring_match.rules`: las correcciones la reutilizan y editar la tabla solo afecta a los partidos que se puntúen después (decisión 6). |
| Recálculo (RF-16) | **Solo automático:** `MatchCorrected` reemplaza la copia del acta y recalcula el partido con su tabla congelada (contrato del plan 07). Recalcular con los mismos datos da lo mismo y no publica nada. El recálculo forzado por el admin queda para la Fase 4 (decisión 7). |
| Clasificaciones (RF-32) | **Por jornada**: los miembros actuales que ya lo eran al empezar el partido (`joinedAt ≤ kickoffAt`), ordenados por puntos. **Acumulada**: los miembros actuales, con la suma de las jornadas puntuadas desde su `joinedAt`, el número de jornadas y los puntos de la última. Quien sale desaparece de las clasificaciones y, si vuelve, empieza de cero (decisión 10). Empatados, **misma posición** (1, 1, 3), sin desempate (decisión 8). |
| Historiales | **Manager (RF-33)**: por jornada, sus puntos, `overBudget` y cada jugador alineado con su posición, desglose por acción, valoración y total; incluye las jornadas de antes de salir y volver, marcadas como fuera de la clasificación. **Jugador (RF-11)**: sus puntos por jornada con el mismo desglose. |
| Visibilidad | Todo miembro ve clasificaciones e historiales de todos: los datos de partida (acta cerrada y alineaciones bloqueadas) ya son públicos para la liga. |
| Aplazado o borrado | `LineupsUnlocked` y `MatchDeleted` descartan la copia de alineaciones de ese partido. Solo pueden llegar antes del cierre, así que nunca borran puntos ya calculados. |

Fuera de alcance: frontend, valoraciones y su conversión (Fase 2), valor de mercado (Fase 3), aviso de «puntos de la jornada publicados» (`notification`), capitán o multiplicadores, puntos de equipo (resultado del partido) o por minutos jugados.

## 2. Modelo de datos (migración nueva, no se edita V1–V11)

**V12__scoring_create_tables.sql**

- `scoring_match` (estado de cálculo de un partido):
  - `match_id` UUID PK, `league_id` UUID NOT NULL, `matchday` SMALLINT NOT NULL, `kickoff_at` TIMESTAMPTZ NOT NULL (referencias lógicas, sin FK entre módulos).
  - `sheet_at` TIMESTAMPTZ NULL: `occurredAt` del último `MatchClosed` o `MatchCorrected` aplicado (nulo mientras no haya acta).
  - `lineups_at` TIMESTAMPTZ NULL: `occurredAt` del `LineupsLocked` aplicado; `unlocked_at` TIMESTAMPTZ NULL: `occurredAt` del último `LineupsUnlocked` (descarta un `LineupsLocked` anterior que llegue tarde por un reenvío).
  - `rules` JSONB NULL: la tabla de puntos congelada al puntuar por primera vez (decisión 6). `scored_at` TIMESTAMPTZ NULL.
  - Índice `(league_id, kickoff_at)`.
- `scoring_player_score` (copia del acta y puntos de cada jugador):
  - `match_id` UUID NOT NULL `REFERENCES scoring_match ON DELETE CASCADE`, `player_id` UUID NOT NULL, `league_id` UUID NOT NULL. PK `(match_id, player_id)`. Índice `(league_id, player_id)` para la ficha.
  - `position` VARCHAR NOT NULL `CHECK` sobre `Position`; los seis contadores (SMALLINT NOT NULL `CHECK (… >= 0)`) y las tres tarjetas (BOOLEAN NOT NULL).
  - `action_points` INTEGER NULL, `rating_points` INTEGER NOT NULL DEFAULT 0, `points` INTEGER NULL (nulos hasta puntuar).
- `scoring_lineup` (copia de las alineaciones bloqueadas y puntos del manager):
  - `match_id` UUID NOT NULL `REFERENCES scoring_match ON DELETE CASCADE`, `user_id` UUID NOT NULL, `league_id` UUID NOT NULL. PK `(match_id, user_id)`. Índice `(league_id, user_id)` para la acumulada y el historial.
  - `over_budget` BOOLEAN NOT NULL, `points` INTEGER NULL (nulo hasta puntuar).
- `scoring_lineup_player`: `(match_id, user_id)` FK a `scoring_lineup` ON DELETE CASCADE, `player_id` UUID NOT NULL. PK `(match_id, user_id, player_id)`.

**Por qué copias propias:** `match` y `lineup` publican el acta y las alineaciones completas en sus eventos para que `scoring` no les consulte (planes 05 y 06). Con la copia, recalcular no depende de nadie y el módulo se podría extraer a servicio sin cambiar contratos.

**Por qué el desglose no se guarda:** sale de la copia del acta × `rules` (la tabla con la que se puntuó), con una función pura. Guardarlo sería repetir datos que pueden divergir.

**Orden y reenvíos:** los listeners son *at-least-once* y pueden llegar desordenados. Cada copia guarda el `occurredAt` del evento que la produjo y solo se reemplaza con uno **más reciente**: un `MatchClosed` reenviado tras un `MatchCorrected` no deshace la corrección, y un `LineupsLocked` reenviado tras un `LineupsUnlocked` se ignora.

## 3. Relación con otros módulos

- **`league`:**
  - `LeagueSettingsProvider` gana `ScoringTable scoringRules(UUID leagueId)` (una consulta). Record público `ScoringTable(Map<ScoringAction, Map<Position, Integer>> points)` con `pointsFor(action, position)`, espejo de `ScoringRules` como `LineupSettings` lo es de `LeagueSettings`.
  - `LeagueAccess.requireMember` en todas las queries y `memberSince` para las clasificaciones: quién aparece y desde qué jornada cuenta (decisiones 9 y 10).
- **`match`:** consume `MatchClosed`, `MatchCorrected` y `MatchDeleted`; `MatchApi.findMatches` para distinguir en la clasificación por jornada un partido de la liga aún sin puntuar (`409`) de uno de otra liga (`404`).
- **`lineup`:** consume `LineupsLocked` y `LineupsUnlocked`.
- **`squad`:** `SquadApi.findPlayers` para el `404` de la ficha de puntos de un jugador de otra liga.
- **Evento publicado** (paquete raíz de `scoring`): `MatchdayScored(leagueId, matchId, matchday, kickoffAt, players, managers, occurredAt)`, con `players` como lista de `PlayerScoreData(playerId, position, actionPoints, ratingPoints, points)` y `managers` como lista de `ManagerScoreData(userId, points, overBudget)`. Se publica la primera vez que se puntúa un partido y **otra vez solo si un recálculo cambia algún punto**: el consumidor (`market` en la Fase 3, `notification`) reemplaza lo que tuviera de ese partido. Sin consumidores todavía.
- **Dependencias de `scoring`:** `league`, `squad`, `match`, `lineup`, `datadefinition`, `shared`. Ninguno de ellos depende de `scoring`; `market` dependerá de él.

## 4. Dominio (`scoring/domain`, sin Spring ni JPA)

- `PlayerPerformance` (VO): copia de una línea del acta (posición, contadores y tarjetas).
- `ActionScoring` (servicio de dominio puro): `breakdown(performance, table)` → `ActionBreakdown` (puntos por `ScoringAction`, solo las que suman o restan, y su total). Correspondencia fija acción ↔ estadística: `GOAL` ↔ `goals`, `PENALTY_GOAL` ↔ `penaltyGoals`, …, `YELLOW_CARD` ↔ `yellowCard ? 1 : 0`.
- `MatchScore` (aggregate del partido):
  - `applySheet(matchday, kickoffAt, performances, occurredAt)`, `applyLineups(lineups, occurredAt)`, `unlock(occurredAt)`: actualizan la copia si el evento es más reciente que el aplicado; si no, no hacen nada. El acta trae también la jornada y el `kickoffAt` definitivos (un aplazamiento que no desbloquea deja el `LineupsLocked` con los antiguos), así que `MatchCorrected` lleva también `kickoffAt`, como `MatchClosed`. Un partido ya puntuado ignora `unlock`.
  - `score(currentTable, now)`: si tiene acta y alineaciones, congela la tabla (un `Supplier`, leído solo la primera vez) si aún no tiene (si ya la tiene, usa la suya), calcula `action_points` y `points` de cada jugador y los puntos de cada manager (0 si `overBudget`), y registra `MatchdayScored` si es la primera vez o si algún punto cambió. Sin acta o sin alineaciones no hace nada.
- `Standings` (servicio de dominio puro): recibe los puntos por jornada de cada manager y los miembros con su `joinedAt`, suma solo lo de cada miembro actual desde su `joinedAt`, pone a 0 a quien no tenía alineación, ordena por puntos y da la misma posición a los empatados (1, 1, 3).
- Errores: `NotFoundException` (`404`) para partidos, managers o jugadores de otra liga; `ConflictException` (`409`, código `MATCH_NOT_SCORED`) para la clasificación de un partido de la liga aún sin puntuar.

## 5. Casos de uso (`scoring/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `ApplyMatchSheetCommand` | command interno | — | Desde `MatchClosed` y `MatchCorrected`. Aplica la copia y puntúa si puede. |
| `ApplyLockedLineupsCommand` | command interno | — | Desde `LineupsLocked`. Aplica la copia y puntúa si puede. |
| `DiscardLockedLineupsCommand` | command interno | — | Desde `LineupsUnlocked` (guarda `unlocked_at`) y `MatchDeleted` (borra el partido de `scoring` si no tiene acta). |
| `GetStandingsQuery` | query | `requireMember` | Clasificación acumulada (RF-32) de los miembros actuales: posición, `userId`, puntos desde su `joinedAt`, jornadas puntuadas y puntos de la última. Quien no ha puntuado aún sale con 0. Una consulta agregada más la de miembros. |
| `GetMatchdayScoresQuery` | query | `requireMember` | Clasificación del partido (RF-32), con 0 y sin jugadores para quien no tenía alineación, y puntos de todos los jugadores del acta. `404` si es de otra liga, `409` si aún no está puntuado. |
| `GetManagerHistoryQuery` | query | `requireMember` | RF-33: jornadas del manager con sus puntos y el detalle de cada jugador alineado; las anteriores a su `joinedAt` van marcadas como fuera de la clasificación. `404` si no es miembro actual de la liga. |
| `GetPlayerPointsQuery` | query | `requireMember` | RF-11: puntos del jugador por jornada con el desglose. `404` si el jugador es de otra liga. |

**Concurrencia:** `MatchClosed` y `LineupsLocked` llegan casi a la vez (el cierre provoca el bloqueo en `lineup`), en hilos distintos. Cada command hace `insert … on conflict do nothing` de `scoring_match` y luego `select … for update`: los dos se serializan sobre la fila y el segundo ve las dos copias y puntúa. Sin ese bloqueo, cada uno vería solo su mitad y nadie puntuaría.

**Idempotencia:** aplicar un evento ya aplicado no cambia la copia (mismo `occurredAt`), y puntuar con los mismos datos no cambia ningún punto ni publica `MatchdayScored`. Los listeners despachan por el bus, como en `lineup`.

**Usuarios:** las vistas devuelven `userId`, no nombres, igual que `GET /lineups/{matchId}`; el frontend los cruza con `GET /members`.

## 6. API HTTP (`scoring/adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `GET /api/leagues/{id}/standings` | `GetStandingsQuery` |
| `GET /api/leagues/{id}/standings/{matchId}` | `GetMatchdayScoresQuery` |
| `GET /api/leagues/{id}/managers/{userId}/history` | `GetManagerHistoryQuery` |
| `GET /api/leagues/{id}/players/{playerId}/points` | `GetPlayerPointsQuery` |

Todos exigen sesión (`401`) y ser miembro (`403`). Las fechas van en ISO-8601 (`Instant`, UTC). Son solo de lectura: el recálculo es automático y no hay endpoint para forzarlo (decisión 7).

## 7. Tests

| Test | Qué verifica |
| --- | --- |
| Migración V12 | Aplica sobre Postgres limpio y `ddl-auto: validate` pasa; cascadas al borrar el partido; línea nueva en `migration-checksums.txt`. |
| `ActionScoring` (unitarios, RNF-11) | Gol según posición (6/5/4/3); gol de penalti +3 en las cuatro posiciones sin sumar el gol; penalti parado y fallado; doble amarilla −3 en total; amarilla + roja −6; varios goles; tabla editada (no la por defecto, con `TestLeagueDefaults` para comprobar que coinciden con los requisitos). |
| `MatchScore` (unitarios) | Acta y alineaciones en cualquier orden → puntúa una vez; solo una de las dos → no puntúa; `overBudget` → 0; alineado que no jugó → 0; evento más antiguo que el aplicado → ignorado; recalcular igual → sin evento; corrección que cambia puntos → evento nuevo; la corrección usa la tabla congelada aunque se le pase otra. |
| `Standings` (unitarios) | Orden por puntos; empatados con la misma posición (1, 1, 3); miembro sin alineación con 0; jornadas anteriores al `joinedAt` no suman; ex-miembro ausente. |
| `ScoringFlowTests` (integración, por eventos reales) | Cierre + bloqueo → puntos de jugadores y managers y `MatchdayScored` una vez (`RecordedEvents`); con el bloqueo antes y después del cierre; tabla editada por la liga; tabla editada después de puntuar → la corrección sigue usando la congelada y el partido siguiente la nueva; jugador retirado antes del partido (0) y después (conserva sus puntos); corrección → recálculo; `LineupsUnlocked` y `MatchDeleted` descartan la copia; reenviar cada evento ya procesado no cambia nada (`TableDigests`). |
| Endpoints | Clasificación acumulada y por jornada, historial del manager y del jugador; miembro sin alineación con 0; expulsado que vuelve empieza de cero en la acumulada y conserva su historial; aislamiento entre ligas (`404`); partido sin puntuar (`409`); no miembro (`403`). Entran en `LeagueAuthorizationTests.rules()`, `ApiSecurityTests` y `ApiErrorsTests`. |
| `ConcurrencyTests` | `MatchClosed` y `LineupsLocked` del mismo partido a la vez, 10 rondas → se puntúa una vez y un solo `MatchdayScored`. Comprobado quitando el `for update`. |
| `EventResubmissionTests` | Reenviar `MatchClosed`, `MatchCorrected`, `LineupsLocked` y `LineupsUnlocked` ya procesados → ninguna tabla cambia y no hay `MatchdayScored` de más. |
| `ScreenStatementsTests` | Las cuatro pantallas nuevas, mismas sentencias con la liga pequeña y con la temporada (sin N+1). |
| `MatchdayFlowTests` | El flujo de la jornada continúa: tras el cierre, la clasificación y los historiales por HTTP; tras la corrección, los puntos recalculados. |
| `LeagueSettingsProvider.scoringRules` | Devuelve la tabla de la liga pedida, en una consulta. |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde (`scoring` depende de `league`, `squad`, `match`, `lineup`, `datadefinition` y `shared`; sin ciclos). |

Cada test nuevo se comprueba quitando a mano la defensa que prueba.

## 8. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | `ScoringTable` y `LeagueSettingsProvider.scoringRules` en `league`, con su test | Tabla de puntos consultable | ✅ |
| 2 | V12, `ActionScoring`, `MatchScore`, `Standings` y `MatchdayScored`, con tests unitarios; `allowedDependencies` de `scoring` | Dominio verificado (RF-15, RF-30, RF-31) | ✅ |
| 3 | Persistencia, commands internos y listeners de `MatchClosed`, `LineupsLocked`, `LineupsUnlocked` y `MatchDeleted`; concurrencia y reenvíos | Puntos calculados al cerrar y bloquear | ✅ |
| 4 | Recálculo con `MatchCorrected` y tabla congelada | RF-16 en `scoring` | ✅ |
| 5 | `GetStandingsQuery`, `GetMatchdayScoresQuery` y sus endpoints | RF-32 | ✅ |
| 6 | `GetManagerHistoryQuery`, `GetPlayerPointsQuery` y sus endpoints | RF-33 y RF-11 | |
| 7 | `ScreenStatementsTests`, `MatchdayFlowTests`, escaneos; `AGENTS.md` (estado), marcar el ítem en `requirements.md` y la parte de `scoring` del ítem de Fase 4 | Ítem de Fase 1 cerrado | |

Cada tarea es un commit `feat(scoring): … (RF-31)` en Conventional Commits.

## 9. Riesgos y notas

- **Fase 2:** `rating_points` vale 0 hasta que llegue `rating`. Entonces los puntos por acciones se seguirán publicando al cerrar y bloquear, como **provisionales**, y pasarán a definitivos al cerrar la ventana de valoración (`RatingWindowClosed`) con la valoración sumada (decisión 12). El plan de `rating` añadirá a `scoring_match` el estado provisional/definitivo y a `MatchdayScored` la marca correspondiente; `market` solo actuará con los definitivos. El modelo de esta fase ya separa las dos partes.
- **Parámetros de valoración:** la sustitución de `ratingPointsPerUnit` por la conversión por formato y el mínimo de votos (roadmap de la Fase 2) no toca este plan.
- **Valor de mercado:** `market` necesitará la media de la jornada (RF-35). `MatchdayScored` lleva los puntos de todos los jugadores del acta, sin decidir aún de quién es la media (todos los que jugaron o solo los alineados).
- **Cambio de la tabla a mitad de temporada:** con la tabla congelada por partido, la temporada puede acabar con jornadas puntuadas con tablas distintas. Es lo esperado (el plan 03, decisión 3, ya fijó que editar parámetros no recalcula solo). Aplicar una tabla nueva a jornadas pasadas será el recálculo forzado de la Fase 4, que sustituirá la tabla congelada por la vigente.
- **Miembros que salen:** sus filas de `scoring_lineup` se conservan (son historia de esas jornadas y del historial si vuelven). La clasificación por jornada de un partido pasado también deja de mostrarlos. El borrado de cuenta (Fase 4, RNF-10) deberá anonimizarlas o borrarlas sin romper las clasificaciones.
- **Miembro que entra a mitad de temporada:** no aparece en las clasificaciones de los partidos anteriores a su `joinedAt` y en la acumulada parte de 0.
- **Rendimiento:** ~30 partidos × ~15 jugadores × ~20 managers. La acumulada es una suma agrupada por `user_id` con el índice `(league_id, user_id)`; el historial del manager, dos consultas (alineaciones y jugadores). Cumple RNF-09.

## 10. Decisiones

Cerradas (vienen de planes anteriores):

1. **Cálculo en `scoring`:** `match` y `lineup` publican el acta y las alineaciones completas; `scoring` guarda su copia (planes 05 y 06).
2. **Sin pesos:** puntos del jugador = acciones + valoración, sin porcentajes (RF-22, RF-30).
3. **Fuera de presupuesto:** la alineación bloqueada `overBudget` suma 0 esa jornada (plan 06, decisión 6).
4. **Editar parámetros no recalcula solo** (plan 03, decisión 3).
5. **Jornada anterior y orden:** cronológico por `kickoff_at` (plan 05, decisión 12).

Tomadas en este plan:

6. **Tabla congelada:** se guarda la tabla al puntuar el partido por primera vez (`scoring_match.rules`) y las correcciones la reutilizan; editar la tabla solo afecta a los partidos que se puntúen después.
7. **Recálculo solo automático:** en esta fase, con `MatchCorrected`. El recálculo forzado por el admin (por ejemplo, para aplicar una tabla editada a jornadas pasadas) queda para la Fase 4.
8. **Empates:** misma posición para los empatados (1, 1, 3), sin desempate.
9. **Sin alineación bloqueada:** el miembro aparece en la clasificación de esa jornada con 0 puntos y sin jugadores.
10. **Ex-miembros:** las clasificaciones solo muestran a los miembros actuales. Quien vuelve empieza de cero también en puntos (como en la decisión D2 del plan 07): cuentan las jornadas desde su nuevo `joinedAt`, y las anteriores siguen en su historial.
11. **Jugador dado de baja:** los puntos salen solo del acta. Dado de baja antes del partido, no puede estar en el acta y suma 0; dado de baja después, conserva los puntos de los partidos que jugó.
12. **Fase 2, puntos provisionales:** los puntos por acciones se publican al cerrar y bloquear, como provisionales, y pasan a definitivos al cerrar la ventana de valoración. Lo implementa el plan de `rating`.

No quedan decisiones abiertas para este paso.
