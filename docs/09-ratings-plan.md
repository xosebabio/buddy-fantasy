# 09 · Valoraciones (`rating`) — plan

Los tres ítems de desarrollo de la **Fase 2 · Valoraciones** (`requirements.md`): *apertura y cierre de la ventana de valoración con job programado y ShedLock* (RF-17), *pantalla de valoración en picas o nota decimal, editable mientras siga abierta, y cancelación de las valoraciones de un manager por el admin* (RF-18 a RF-21 y RF-41; aquí su API, aún no hay frontend) y *combinación de valoración y estadísticas al cierre, con recálculo idempotente* (RF-22, RF-23), que incluye cambiar los parámetros de valoración de `league`. Cierra también los puntos por valoración de RF-30, que `scoring` dejó a 0, y el recálculo en `rating` del ítem de correcciones de la Fase 4. Se apoya en `match` (`MatchClosed`, `MatchCorrected`), en `league` (roles, miembros y parámetros) y modifica `scoring`. Cumple los contratos de `rating` del plan 07, §4.3. El ítem *prueba real con los amigos* queda fuera: necesita frontend y VPS.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Ventana (RF-17) | Se abre al cerrar el partido (`MatchClosed.occurredAt`) y se cierra a las `ratingWindowHours` (por defecto 24 h). El plazo lo marca el reloj del servidor: un voto con `now ≥ closesAt` se rechaza aunque el job aún no haya cerrado la ventana. El admin no puede cerrarla antes ni ampliarla (decisión 8). |
| Parámetros congelados | Al abrirse, la ventana guarda formato, conversión, mínimo de votos y nota por defecto de la liga, y los usa hasta el final, también en las correcciones. Editar los parámetros solo afecta a los partidos que se cierren después (decisión 6), como la tabla de puntos congelada de `scoring`. |
| Quién valora (RF-18) | Todo miembro actual de la liga: `MANAGER` y `ADMIN`. A **cualquier jugador del acta**, también a los de su alineación (decisión 2). Valorar es opcional y por jugador: se puede valorar a unos sí y a otros no. |
| Valores (RF-19) | Enteros: picas 0–4 o nota 0–10, según el formato congelado de la ventana (decisión 4). La definición de cada formato es una constante (`RatingFormat`), no configuración. |
| Edición (RF-20) | El manager reemplaza sus valoraciones del partido mientras la ventana siga abierta; quitar un voto también vale. Un miembro que sale de la liga conserva los votos que emitió (son anónimos). |
| Anonimato (RF-21) | Ninguna API devuelve votos de otro usuario, **tampoco al admin**. **Durante la ventana** cada manager ve solo los suyos; **al cerrar** se publican la media y el número de votos de cada jugador (decisión 3). |
| Cancelación (RF-41) | Con la ventana abierta, el admin cancela **todas** las valoraciones de un manager en un partido: se borran y el manager queda **excluido** de valorar ese partido. Vale también para quien aún no ha votado («no vio el partido»). Es **definitiva**: no se deshace. Con la ventana cerrada, `409`. El admin decide con información de fuera de la app: no ve votos ni desviaciones. El excluido ve que lo está, sin motivo; el aviso por notificación llega con la Fase 3 (decisiones 10 a 13). |
| Conversión (RF-22) | Lineal por formato: `puntos = factor × media + base`, con factor y base por liga y formato (picas 4 y −2; nota 1 y 0). Se calcula con la media exacta (suma / votos, sin redondeos intermedios) y el resultado se redondea al entero, **mitad alejándose del cero** (`RoundingMode.HALF_UP`: 8,5 → 9, −1,5 → −2; decisión 7). |
| Sin valoraciones (RF-23) | Con menos votos que `ratingMinVotes` (por defecto 1): la nota por defecto de la liga convertida igual, si la hay; si no, **0 puntos** (no los −2 de 0 picas). |
| Puntos y clasificación (RF-30) | `scoring` puntúa un partido cuando tiene el acta, las alineaciones bloqueadas **y las valoraciones**. Mientras la ventana está abierta, la jornada **no aparece** en clasificaciones ni historiales (decisión 5): `MatchdayScored` se publica ya con los puntos definitivos. |
| Correcciones (RF-16) | `MatchCorrected` actualiza los jugadores valorables. Los quitados del acta pierden sus votos, aunque la ventana esté cerrada (plan 05, decisión 14). Con la ventana cerrada no se reabre: un jugador añadido cuenta como sin valoraciones (decisión 9). Si cambia algún resultado ya publicado, `rating` publica `RatingsCorrected` y `scoring` recalcula. |

Fuera de alcance: frontend, avisos de «valoraciones abiertas» y «a punto de cerrar» (`notification`, Fase 3), valor de mercado (Fase 3), cerrar o ampliar una ventana a mano, recálculo forzado por el admin (Fase 4), vincular usuarios con jugadores de la plantilla, comentarios en las valoraciones, borrado o anonimización de votos al borrar la cuenta (Fase 4, RNF-10).

## 2. Modelo de datos (migraciones nuevas, no se edita V1–V12)

**V13__league_rating_conversion.sql** (módulo `league`)

- `league_settings`: quita `rating_points_per_unit` y añade `rating_spades_factor` y `rating_decimal_factor` NUMERIC(6, 2) NOT NULL `CHECK (… > 0)`, `rating_spades_base` y `rating_decimal_base` NUMERIC(6, 2) NOT NULL, y `rating_min_votes` INTEGER NOT NULL `CHECK (rating_min_votes >= 1)`. Las ligas existentes se rellenan con 4, 1, −2, 0 y 1 (`DEFAULT` solo para las filas ya existentes; luego se quita).

**V14__rating_create_tables.sql**

- `rating_window` (una por partido cerrado):
  - `match_id` UUID PK, `league_id` UUID NOT NULL, `matchday` SMALLINT NOT NULL, `kickoff_at` TIMESTAMPTZ NOT NULL (referencias lógicas, sin FK entre módulos).
  - `opened_at` TIMESTAMPTZ NOT NULL, `closes_at` TIMESTAMPTZ NOT NULL, `closed_at` TIMESTAMPTZ NULL (nulo mientras el job no la cierre).
  - Parámetros congelados: `format` VARCHAR NOT NULL `CHECK` sobre `RatingFormat`, `conversion_factor` y `conversion_base` NUMERIC(6, 2) NOT NULL, `min_votes` INTEGER NOT NULL, `default_rating` NUMERIC(4, 2) NULL.
  - `sheet_at` TIMESTAMPTZ NOT NULL: `occurredAt` del último `MatchClosed` o `MatchCorrected` aplicado.
  - Índice `(closes_at) WHERE closed_at IS NULL` para el job; índice `(league_id, kickoff_at)` para el listado.
- `rating_player` (jugadores valorables, copia del acta, y su resultado al cerrar):
  - `match_id` UUID NOT NULL `REFERENCES rating_window ON DELETE CASCADE`, `player_id` UUID NOT NULL, `position` VARCHAR NOT NULL `CHECK` sobre `Position`. PK `(match_id, player_id)`.
  - `votes` INTEGER NULL, `average` NUMERIC(4, 2) NULL (solo para mostrar), `rating_points` INTEGER NULL: nulos hasta cerrar.
- `rating_vote`: `(match_id, player_id)` FK a `rating_player` **ON DELETE CASCADE** (quitar a un jugador del acta borra sus votos), `user_id` UUID NOT NULL, `value` SMALLINT NOT NULL `CHECK (value >= 0)`, `updated_at` TIMESTAMPTZ NOT NULL. PK `(match_id, player_id, user_id)`. Índice `(match_id, user_id)` para «mis votos».
- `rating_exclusion` (RF-41): `match_id` UUID NOT NULL `REFERENCES rating_window ON DELETE CASCADE`, `user_id` UUID NOT NULL, `excluded_by` UUID NOT NULL (el admin), `excluded_at` TIMESTAMPTZ NOT NULL. PK `(match_id, user_id)`. Es a la vez la exclusión y su registro de auditoría: quién canceló a quién y cuándo.

**V15__scoring_ratings.sql** (módulo `scoring`)

- `scoring_match`: añade `ratings_at` TIMESTAMPTZ NULL (`occurredAt` del último `RatingWindowClosed` o `RatingsCorrected` aplicado). Sustituye el `CHECK` de `scored_at` por uno que exige también `ratings_at`. Los partidos ya puntuados se rellenan con `ratings_at = scored_at` (sin valoraciones: 0 puntos).
- `scoring_player_rating` (copia de los puntos por valoración): `match_id` UUID NOT NULL `REFERENCES scoring_match ON DELETE CASCADE`, `player_id` UUID NOT NULL, `rating_points` INTEGER NOT NULL. PK `(match_id, player_id)`. Separada de `scoring_player_score` porque puede llegar con jugadores que la copia del acta aún no tiene, o al revés (eventos desordenados).

**Por qué el resultado se guarda:** al cerrar, la media y los puntos se congelan; las vistas los leen sin agregar votos, y una corrección compara contra lo publicado para saber si debe publicar `RatingsCorrected`.

**Orden y reenvíos:** como en `scoring`, cada copia guarda el `occurredAt` del evento que la produjo y solo la reemplaza uno más reciente: un `MatchClosed` reenviado tras un `MatchCorrected` no deshace la corrección, y un `RatingWindowClosed` reenviado tras un `RatingsCorrected` tampoco.

## 3. Relación con otros módulos

- **`datadefinition`:** recibe `RatingFormat` (hoy en `league/domain`), que ahora usan `league` y `rating`: enum con el valor máximo de cada formato como dato (`SPADES` 4, `DECIMAL` 10; el mínimo es 0).
- **`league`:**
  - `LeagueSettings` sustituye `ratingPointsPerUnit` por `ratingSpadesFactor`, `ratingSpadesBase`, `ratingDecimalFactor`, `ratingDecimalBase` (factores > 0) y añade `ratingMinVotes` (≥ 1). Se guardan las dos conversiones: cambiar de formato no pierde la del otro.
  - `league.defaults.rating` en `application.yml`: quita `points-per-unit` y añade `conversion.spades: { factor: 4, base: -2 }`, `conversion.decimal: { factor: 1, base: 0 }` y `min-votes: 1`. `LeagueDefaultsProperties`, `LeagueDefaultsMapper`, `LeagueSettingsBody` y su mapper, igual.
  - `LeagueSettingsProvider` gana `RatingSettings ratingSettings(UUID leagueId)` (una consulta). Record público `RatingSettings(int windowHours, RatingFormat format, BigDecimal factor, BigDecimal base, int minVotes, BigDecimal defaultRating)` con la conversión **del formato vigente** ya elegida.
  - `LeagueAccess.requireMember` en todos los casos de uso.
- **`match`:** consume `MatchClosed` y `MatchCorrected` (no hacen falta `MatchDeleted` ni `MatchRescheduled`: un partido cerrado no se borra ni se aplaza). `MatchApi.findMatches` para distinguir un partido de la liga aún sin cerrar (`409`) de uno de otra liga (`404`).
- **Eventos publicados** (paquete raíz de `rating`):
  - `RatingWindowClosed(leagueId, matchId, players, occurredAt)`, con `players` como lista de `PlayerRatingData(playerId, votes, average, ratingPoints)` (`average` nula sin votos), uno por jugador del acta. Una vez por ventana.
  - `RatingsCorrected(leagueId, matchId, players, occurredAt)`: misma forma, tras una corrección con la ventana cerrada que cambia el resultado. El consumidor reemplaza lo que tuviera de ese partido.
  - `RaterExcluded(leagueId, matchId, userId, occurredAt)` (RF-41): sin consumidores todavía; `notification` avisará al excluido en la Fase 3. No hace falta para los puntos: la ventana sigue abierta y el cierre ya no ve sus votos.
- **`scoring`:** consume `RatingWindowClosed` y `RatingsCorrected`. Pasa a depender de `rating`.
- **Dependencias de `rating`:** `league`, `match`, `datadefinition`, `shared`. Ninguno de ellos depende de `rating`; `scoring` sí (sin ciclos: `rating` no conoce a `scoring`).

## 4. Dominio (`rating/domain`, sin Spring ni JPA)

- `RatingRules` (VO): formato, factor, base, mínimo de votos y nota por defecto congelados. `requireValidVote(value)`: entero en `[0, format.maxRating]`.
- `RatingConversion` (servicio de dominio puro): `result(votes)` → `PlayerRatingResult(votes, average, ratingPoints)`. Con `votes.size() ≥ minVotes`: `(factor × Σ + base × n) / n`, redondeado `HALF_UP` a entero; si no, la nota por defecto convertida igual o 0, y `average` es la media de los votos que haya (nula sin votos). `average` se redondea a 2 decimales solo para mostrar.
- `RatingWindow` (aggregate del partido):
  - `open(match, sheet, rules, occurredAt)`: `closesAt = occurredAt + windowHours`.
  - `applySheet(players, occurredAt)`: si es más reciente que `sheetAt`, reemplaza los jugadores valorables (los quitados pierden sus votos). Con la ventana cerrada recalcula el resultado con los votos que quedan y registra `RatingsCorrected` si alguno cambia, aparece o desaparece.
  - `requireOpen(now)`: `now < closesAt` y sin `closedAt`; si no, `409` `RATING_WINDOW_CLOSED`.
  - `close(votesByPlayer, now)`: si ya está cerrada, no hace nada; si `now < closesAt`, tampoco. Calcula el resultado de cada jugador y registra `RatingWindowClosed`.
- `MyRatings` (VO): los votos de un manager en un partido; `replace(newVotes, window, excluded, now)` valida ventana abierta, que no esté excluido, jugadores del acta, sin repetidos y valores del formato.
- `RatingWindow.exclude(userId, adminId, now)` (RF-41): exige la ventana abierta y registra `RaterExcluded` si aún no lo estaba; excluir dos veces no hace nada.
- Errores con código `RatingError` (`ErrorCode`): `400` `INVALID_RATING` (fuera de rango), `PLAYER_NOT_IN_SHEET`, `DUPLICATE_PLAYER`; `409` `RATING_WINDOW_CLOSED`, `MATCH_NOT_CLOSED`, `RATER_EXCLUDED`; `404` para partidos de otra liga o un usuario que no es miembro.

## 5. Casos de uso y job (`rating/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `OpenRatingWindowCommand` | command interno | — | Desde `MatchClosed`. `insert … on conflict do nothing`: ejecutarlo dos veces no abre dos ventanas. Lee `LeagueSettingsProvider.ratingSettings` solo si la inserta. |
| `ApplyCorrectedSheetCommand` | command interno | — | Desde `MatchCorrected`. Bloquea la ventana (`for update`) y aplica la copia si es más reciente. Sin ventana (su `MatchClosed` falló y se reenviará), la abre con este evento como red de seguridad. |
| `CloseRatingWindowCommand` | command interno | — | Desde el job. Bloquea la ventana (`for update`), calcula los resultados y publica `RatingWindowClosed`. Idempotente. |
| `SaveMyRatingsCommand` | command | `requireMember` | RF-18 a RF-20. Reemplaza todos los votos del manager en el partido (`{ratings: [{playerId, value}]}`; los ausentes se borran). Toma la ventana `for share` y comprueba el plazo con el reloj del servidor. Excluido → `409` `RATER_EXCLUDED`. Devuelve lo mismo que `GetMatchRatingsQuery`. |
| `ExcludeRaterCommand` | command | `requireAdmin` | RF-41. Con la ventana abierta, borra los votos del manager en el partido y guarda su exclusión. El usuario debe ser miembro actual (`404` si no); el admin también puede excluirse a sí mismo. Idempotente: excluir a un excluido no cambia nada ni publica otro `RaterExcluded`. Toma la ventana `for update`. |
| `GetMatchRatingsQuery` | query | `requireMember` | Estado (`OPEN`/`CLOSED`), `closesAt`, formato, `excluded` (si yo estoy excluido) y, por jugador del acta: `playerId`, `position`, **mi voto**, y solo con la ventana cerrada `votes`, `average` y `ratingPoints`. Solo para el admin, `excludedUserIds`. Nunca votos de otros. `404` otra liga, `409` `MATCH_NOT_CLOSED`. |
| `ListRatingWindowsQuery` | query | `requireMember` | Ventanas de la liga (abiertas primero, luego por `kickoffAt` descendente): partido, jornada, `closesAt`, estado, jugadores valorables, cuántos he valorado y si estoy excluido. Para el aviso de «tienes valoraciones pendientes» en el frontend. Dos consultas. |

**Concurrencia:** un voto justo en el cierre y el job se serializan sobre la fila de `rating_window`: los votos la toman `for share` (pueden ir a la vez entre ellos) y el cierre `for update`. Un voto que empezó antes queda incluido; uno que espera al cierre ve `closed_at` y recibe `409`. `MatchCorrected` y el cierre también se serializan (`for update`). La exclusión toma `for update` para no cruzarse con un voto del excluido: si el voto va antes, la exclusión lo borra; si va después, ve la exclusión y recibe `409`. Es una operación rara, así que no importa que frene los votos del partido un instante.

**Job `CloseDueRatingWindowsJob`** (`@Scheduled` cada minuto + `@SchedulerLock`): pide las ventanas con `closes_at ≤ now` y sin `closed_at` (`FindDueRatingWindowsQuery`) y despacha `CloseRatingWindowCommand` para cada una en su propia transacción; un fallo se registra y se reintenta en la siguiente ejecución. Sin margen: el bloqueo de fila ya ordena votos y cierre. Cron en `rating.close-job.cron` (`RATING_CLOSE_JOB_CRON`, `-` lo desactiva; en los tests se ejecuta a mano), configuración técnica en su propio record de properties.

**Idempotencia:** reenviar `MatchClosed` no reabre ni mueve la ventana; reenviar `MatchCorrected` ya aplicado no cambia nada; cerrar una ventana cerrada no publica nada; una corrección que no cambia ningún resultado no publica `RatingsCorrected`. Los listeners despachan por el bus.

## 6. Cambios en `scoring`

- `MatchScore.applyRatings(ratingPointsByPlayer, occurredAt)`: reemplaza la copia si es más reciente que `ratingsAt`.
- `score(...)` exige acta, alineaciones **y** valoraciones. `ratingPoints` de cada jugador sale de la copia (0 si no está: un jugador añadido por una corrección hasta que llegue `RatingsCorrected`). Puntos = acciones + valoración, sin pesos.
- `ApplyRatingsCommand` desde `RatingWindowClosed` y `RatingsCorrected` (`RatingEventsListener`), con el mismo `lockOrCreate` + `for update` que los demás commands. Puede llegar un `RatingWindowClosed` reenviado antes que la copia del acta: se guarda y se puntúa cuando esté todo.
- `MatchdayScored` deja de tener puntos «provisionales»: se publica una vez con los definitivos y otra solo si un recálculo los cambia (sustituye la decisión 12 del plan 08). `market` (Fase 3) lo consume tal cual.
- Las vistas no cambian: ya filtran por `scored_at`. Durante la ventana, `GET /standings/{matchId}` responde `409` `MATCH_NOT_SCORED`, cuyo comentario se actualiza (falta el acta, las alineaciones o el cierre de las valoraciones).
- `allowedDependencies` de `scoring` añade `rating`.

## 7. API HTTP (`rating/adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `GET /api/leagues/{id}/ratings` | `ListRatingWindowsQuery` |
| `GET /api/leagues/{id}/ratings/{matchId}` | `GetMatchRatingsQuery` |
| `PUT /api/leagues/{id}/ratings/{matchId}` | `SaveMyRatingsCommand` → `200` con la vista |
| `PUT /api/leagues/{id}/ratings/{matchId}/exclusions/{userId}` | `ExcludeRaterCommand` → `204` (solo admin; sin `DELETE`: es definitiva) |

Todos exigen sesión (`401`), ser miembro (`403`; admin en las exclusiones) y CSRF en los `PUT`. Las fechas van en ISO-8601 (`Instant`, UTC). Cambia también `GET/PUT /api/leagues/{id}/settings`: `ratingPointsPerUnit` desaparece y llegan los factores, las bases y `ratingMinVotes` (no hay frontend que romper).

## 8. Tests

| Test | Qué verifica |
| --- | --- |
| Migraciones V13–V15 | Aplican sobre Postgres limpio y `ddl-auto: validate` pasa; relleno de las ligas y partidos existentes; cascada de `rating_vote` al quitar un jugador; líneas nuevas en `migration-checksums.txt`. |
| `LeagueSettings` y defaults (unitarios) | Factor ≤ 0 o mínimo de votos < 1 → error; `TestLeagueDefaults` coincide con los requisitos (picas 4/−2, nota 1/0, mínimo 1, sin nota por defecto). `LeagueSettingsProvider.ratingSettings` da la conversión del formato vigente, en una consulta. |
| `RatingConversion` (unitarios, RNF-11) | Picas: 0 → −2, 1 → 2, 2 → 6, 3 → 10, 4 → 14; media 2,5 → 8; 2,6 → 8; −1,5 → −2 (alejándose del cero); media periódica (1, 1, 2 → 3,33 → 3) sin redondeo intermedio. Nota: 7,5 → 8, 7,4 → 7. Sin votos → 0, no −2; menos votos que el mínimo → nota por defecto convertida o 0; conversión editada (no la por defecto). |
| `RatingWindow` (unitarios, `Clock` fijo) | Voto justo antes de `closesAt` válido y justo en `closesAt` → `409`; valor fuera del formato o no entero → `400`; jugador fuera del acta → `400`; cerrar dos veces → un evento; corrección con la ventana abierta quita votos y añade jugadores; con la ventana cerrada → `RatingsCorrected` solo si cambia algo; parámetros congelados aunque la liga cambie. |
| `RatingFlowTests` (integración, eventos reales, `MutableClock`) | `MatchClosed` abre la ventana con los parámetros de la liga; votar y editar; el job a las 24 h cierra y publica `RatingWindowClosed` una vez (`RecordedEvents`); ejecutar el job dos veces no cambia nada (`TableDigests`); corrección abierta y cerrada; formato decimal; mínimo de votos 2 con un solo voto. |
| `RatingEndpointsTests` | **Anonimato con dos managers y el admin**: nadie ve el voto de otro, ni durante ni después; sin media durante la ventana; media y votos al cerrar. Admin vota; no miembro → `403`; partido de otra liga → `404`; partido sin cerrar → `409`; ventana vencida sin job → `409`. Entran en `LeagueAuthorizationTests.rules()`, `ApiSecurityTests` y `ApiErrorsTests`. |
| `RatingExclusionTests` (RF-41) | El admin excluye a un manager con votos → se borran, el manager ve `excluded` y al votar recibe `409` `RATER_EXCLUDED`; al cerrar, la media no cuenta sus votos. Excluir a quien no votó le impide votar. Excluir dos veces → un `RaterExcluded`. Manager que excluye → `403`; usuario que no es miembro → `404`; ventana cerrada (o vencida sin job) → `409`. Solo el admin recibe `excludedUserIds`. La exclusión de un partido no afecta a otro. |
| `ScoringFlowTests` (ampliado) | Acta + alineaciones sin valoraciones → no puntúa ni publica; al cerrar la ventana → puntos = acciones + valoración y `MatchdayScored` una vez, en cualquier orden de los tres eventos; `RatingsCorrected` → recálculo; reenvíos sin efectos. |
| `ConcurrencyTests` | Votos y cierre a la vez, 10 rondas: cada voto queda dentro (incluido en el resultado) o fuera (`409`), nunca a medias. Voto del manager y su exclusión a la vez → nunca quedan votos de un excluido. `MatchClosed` y `MatchCorrected` a la vez → una ventana. Comprobado quitando el `for share`/`for update`. |
| `EventResubmissionTests` | Reenviar `MatchClosed`, `MatchCorrected`, `RatingWindowClosed` y `RatingsCorrected` ya procesados → ninguna tabla cambia y no hay eventos de más. |
| `SchedulingArchitectureTests` | `CloseDueRatingWindowsJob` lleva `@SchedulerLock`. |
| `ScreenStatementsTests` | Las dos pantallas de `rating` (listado y partido), mismas sentencias con la liga pequeña y con la temporada (sin N+1). |
| `MatchdayFlowTests` | Tras el cierre: clasificación de la jornada `409`; los dos managers valoran por HTTP; el reloj avanza 24 h, el job cierra; clasificación e historiales con los puntos por valoración; corrección posterior. |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde (`rating` depende de `league`, `match`, `datadefinition` y `shared`; `scoring` añade `rating`; sin ciclos). |

Cada test nuevo se comprueba quitando a mano la defensa que prueba.

## 9. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | `RatingFormat` a `datadefinition`; V13, conversión por formato y `ratingMinVotes` en `LeagueSettings`, defaults, persistencia y API de parámetros; `RatingSettings` y `LeagueSettingsProvider.ratingSettings`, con sus tests | Parámetros de valoración listos (parte de `league` del ítem de RF-22, RF-23) | ✅ |
| 2 | V14, `RatingRules`, `RatingConversion`, `RatingWindow`, `RatingWindowClosed`, `RatingsCorrected`, con tests unitarios; `allowedDependencies` de `rating` | Dominio verificado (RF-17, RF-19, RF-22, RF-23) | |
| 3 | Persistencia, listeners de `MatchClosed` y `MatchCorrected`, `CloseRatingWindowCommand` y `CloseDueRatingWindowsJob`; reenvíos | Ventana abierta y cerrada con job y ShedLock (RF-17) | |
| 4 | `SaveMyRatingsCommand`, `GetMatchRatingsQuery`, `ListRatingWindowsQuery` y sus endpoints; anonimato y concurrencia con el cierre | RF-18 a RF-21 | |
| 4b | `rating_exclusion` (en V14), `ExcludeRaterCommand`, `RaterExcluded` y su endpoint; exclusión en las vistas; concurrencia con el voto | RF-41 | |
| 5 | V15, `applyRatings` y puntuar con valoraciones en `scoring`, `RatingEventsListener`; ampliar `ScoringFlowTests` y reenvíos | RF-22, RF-23, RF-30 completos | |
| 6 | `ScreenStatementsTests`, `MatchdayFlowTests`, escaneos; `AGENTS.md` (estado, comandos con `RATING_CLOSE_JOB_CRON`), marcar los tres ítems en `requirements.md` y actualizar el de correcciones de la Fase 4 | Ítems de la Fase 2 cerrados | |

Cada tarea es un commit `feat(rating): … (RF-17)` (o `feat(league)`, `feat(scoring)`) en Conventional Commits.

## 10. Riesgos y notas

- **Clasificación oculta 24 h:** la jornada no se ve hasta cerrar la ventana, aunque el acta y las alineaciones bloqueadas sí son visibles. Es lo decidido; si en la prueba real se echa en falta, enseñar los puntos por acciones como provisionales es un cambio solo en `scoring`.
- **Votos interesados:** cualquiera puede valorar a sus alineados y, si también juega en la peña, a sí mismo. Se acepta (decisión 2); el mínimo de votos y la nota por defecto amortiguan el voto aislado. Si molesta, vincular usuario y jugador es un cambio de `squad` y `rating` para más adelante.
- **Cancelación a ciegas (RF-41):** el admin no ve votos ni desviaciones, así que solo puede actuar con lo que sepa fuera de la app, y solo durante la ventana. Si en la prueba real no basta, ver una desviación por manager sin valores concretos (o permitirlo tras el cierre con recálculo) es una ampliación de `rating` que no cambia este modelo. Un admin podría usarla contra alguien sin motivo; queda registrada con `excluded_by` y en un grupo de amigos se acepta.
- **Corrección justo antes del cierre:** con la ventana abierta, un jugador añadido puede quedarse con muy poco tiempo para votos; cuenta con los que reciba o como sin valoraciones.
- **Transitorio tras una corrección con la ventana cerrada:** `scoring` recalcula con `MatchCorrected` (un jugador añadido suma 0 por valoración) y otra vez con `RatingsCorrected` (la nota por defecto, si la hay). Puede publicar dos `MatchdayScored` seguidos; el consumidor se queda con el último.
- **Cambio de formato a mitad de temporada:** las ventanas abiertas siguen con el suyo; las medias de partidos con formatos distintos no son comparables, pero los puntos sí.
- **Borrado de cuenta (Fase 4, RNF-10):** los votos llevan `user_id` solo para que su autor los edite; tras el cierre podrían anonimizarse sin cambiar resultados.
- **Rendimiento:** ~15 jugadores × ~20 managers por partido. Votar son 3 consultas (ventana, votos, reemplazo); cerrar, una agregación por jugador. Cumple RNF-09.
- **Fase 0 pendiente:** Docker, CI de despliegue y VPS siguen sin hacer, y la prueba real de la Fase 2 los necesita, junto con el frontend.

## 11. Decisiones

Cerradas (vienen de los requisitos y de planes anteriores):

1. **Formato y conversión por defecto:** picas; 0 = −2, 1 = 2, 2 = 6, 3 = 10, 4 = 14; nota 1:1; mínimo de votos 1; sin valoraciones, 0 puntos (requisitos, decisiones abiertas cerradas).

Tomadas en este plan:

2. **Quién valora a quién:** todo miembro a todos los jugadores del acta, también a los de su alineación.
3. **Visibilidad:** durante la ventana, solo los votos propios; al cerrar, media y número de votos. Nunca votos individuales de otros.
4. **Valores:** enteros en los dos formatos (picas 0–4, nota 0–10).
5. **Sin puntos provisionales:** la jornada no aparece en clasificaciones ni historiales hasta cerrar la ventana; `MatchdayScored` sale ya definitivo (sustituye la decisión 12 del plan 08).
6. **Parámetros congelados al abrir la ventana**, también para las correcciones.
7. **Conversión lineal por formato** (`factor × media + base`, factor y base por liga para cada formato) y redondeo **alejándose del cero** (`HALF_UP`).
8. **Sin acciones del admin sobre la ventana:** ni cerrarla antes ni ampliarla.
9. **Corrección con la ventana cerrada:** no se reabre; un jugador añadido cuenta como sin valoraciones y uno quitado pierde sus votos.
10. **Cancelación (RF-41), alcance:** todas las valoraciones de un manager en un partido; excluye también a quien aún no ha votado.
11. **Cancelación, plazo:** solo con la ventana abierta; tras el cierre no se cancela ni se recalcula.
12. **Cancelación, efecto:** definitiva; los votos se borran y el manager no puede volver a valorar ese partido.
13. **Cancelación y anonimato:** el admin no ve votos individuales ni desviaciones; el excluido ve que lo está, sin motivo.

No quedan decisiones abiertas para este paso.
