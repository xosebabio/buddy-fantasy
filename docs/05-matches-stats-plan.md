# 05 · Partidos, convocatoria y estadísticas (`match`) — plan

Tercer ítem de la **Fase 1 · Núcleo jugable** (`requirements.md`): *partidos, convocatoria, resultado y estadísticas con su tabla de puntos* (RF-12 a RF-16). Se apoya en `league` (roles por liga, `LeagueAccess`), en `squad` (jugadores y su posición) y en `datadefinition` (`Position`). Es la base de los dos ítems que quedan en la fase: `lineup` necesita la fecha y la jornada del partido, y `scoring` necesita sus estadísticas y el evento de cierre.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Jornada | **1 jornada = 1 partido.** El partido lleva su número de jornada, único en la liga. No hay entidad `Matchday`. Un aplazamiento solo cambia la fecha; una semana de descanso no tiene partido. |
| Alta del partido | Solo el admin. Campos: rival, fecha y hora de inicio, local/visitante y jornada (RF-12). Se pueden crear partidos con fecha pasada (cargar el calendario a mitad de temporada). |
| Convocatoria | **Solo quién jugó**, tras el partido (RF-13). **Sin minutos**: RF-13 los hace opcionales y no intervienen en los puntos ni en la valoración. Solo esos jugadores pueden puntuar y, en la Fase 2, ser valorados. No hay convocatoria previa. |
| Acta | Resultado (goles a favor y en contra) + participantes con sus estadísticas, en **una sola operación** que reemplaza el acta completa (RF-13, RF-14). |
| Estadísticas | Por jugador: goles, goles de penalti, penaltis parados, penaltis fallados, asistencias, goles en propia puerta, amarilla, doble amarilla y roja directa. Son las 9 acciones de `ScoringAction` (RF-14 no nombra los penaltis parados y fallados, pero la tabla de puntos sí). |
| Tarjetas | Amarilla y roja directa **se suman** (−1 −5 = −6). La doble amarilla **excluye** a la amarilla simple (vale −3 en total) y a la roja directa (solo hay una expulsión). |
| Cálculo de puntos | **No es de `match`.** `match` guarda los hechos y los publica; `scoring` lee la tabla de `league` y calcula en el siguiente plan. RF-15 se cierra con el ítem *Cálculo de puntos por acción según posición y clasificaciones*. |
| Cierre | Acción explícita del admin («Cerrar partido»). Exige que haya empezado (`now ≥ kickoffAt`, reloj del servidor), un resultado y al menos un participante. Publica `MatchClosed`, que en la Fase 2 abrirá la ventana de valoración (RF-17). |
| Coherencia | **Aviso, sin bloqueo**: si goles + goles de penalti de los jugadores ≠ goles a favor, o si los goles en propia superan a los goles en contra, la API devuelve avisos, pero deja guardar y cerrar. En una peña pueden faltar autores. |
| Visibilidad | Los managers ven siempre rival, fecha, local/visitante, jornada y estado. **Resultado, participantes y estadísticas solo cuando el partido está cerrado.** El admin lo ve todo. |
| Aplazado o suspendido | Mientras no esté cerrado: se cambia la fecha (aplazado) o se borra (suspendido sin recuperar). Un partido cerrado no se borra. |
| Correcciones (RF-16) | **Ahora.** El admin puede reemplazar el acta de un partido cerrado. Se publica `MatchCorrected` para que `scoring` recalcule. La fecha y la jornada de un partido cerrado no se pueden cambiar; el rival y local/visitante sí (son solo informativos). |
| Auditoría (RNF-04) | **Ahora.** Cada mutación del admin sobre un partido (alta, edición, acta, cierre, corrección, borrado) deja una entrada en un log de solo inserción con autor, fecha y estado antes/después. |

Fuera de alcance: frontend, cálculo de puntos y clasificaciones (`scoring`), ventana de valoración (`rating`), alineaciones y su bloqueo (`lineup`), notificaciones, convocatoria previa, varios partidos por jornada, estado «cancelado», reapertura de un partido cerrado (se corrige directamente), API pública `MatchApi` (§3, se crea con `lineup`).

## 2. Modelo de datos (migración nueva, no se edita V1–V7)

**V8__match_create_tables.sql**

- `match_match`:
  - `id` UUID PK, `league_id` UUID NOT NULL (referencia lógica a `league_league`, sin FK entre módulos).
  - `matchday` SMALLINT NOT NULL `CHECK (matchday >= 1)`.
  - `opponent` VARCHAR(100) NOT NULL, `venue` VARCHAR(10) NOT NULL `CHECK (venue IN ('HOME', 'AWAY'))`.
  - `kickoff_at` TIMESTAMPTZ NOT NULL (UTC; la interfaz convierte a `Europe/Madrid`).
  - `status` VARCHAR(10) NOT NULL `CHECK (status IN ('SCHEDULED', 'CLOSED'))`.
  - `goals_for`, `goals_against` SMALLINT NULL `CHECK (… >= 0)`. `CHECK` de que van los dos o ninguno.
  - `created_at` TIMESTAMPTZ NOT NULL, `closed_at` TIMESTAMPTZ NULL. `CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL))`.
- `match_player_performance` (participantes del acta):
  - `match_id` UUID NOT NULL `REFERENCES match_match (id) ON DELETE CASCADE` (misma tabla de módulo, FK permitida), `player_id` UUID NOT NULL (referencia lógica a `squad_player`). PK `(match_id, player_id)`.
  - `position` VARCHAR(20) NOT NULL `CHECK` sobre `Position`: **foto de la posición del jugador cuando se registra el acta**. Si luego se cambia la posición en `squad`, los partidos pasados no varían y el recálculo da lo mismo.
  - `goals`, `penalty_goals`, `penalties_saved`, `penalties_missed`, `assists`, `own_goals` SMALLINT NOT NULL DEFAULT 0 `CHECK (… BETWEEN 0 AND 99)`.
  - `yellow_card`, `double_yellow`, `red_card` BOOLEAN NOT NULL DEFAULT FALSE. `CHECK (NOT (double_yellow AND (yellow_card OR red_card)))`.
- `match_audit_entry`:
  - `id` UUID PK, `league_id` UUID NOT NULL, `match_id` UUID NOT NULL (**sin FK**: la entrada sobrevive al borrado del partido).
  - `actor_user_id` UUID NOT NULL, `action` VARCHAR(30) NOT NULL `CHECK` sobre `MatchAuditAction`, `occurred_at` TIMESTAMPTZ NOT NULL.
  - `before`, `after` JSONB NULL (estado del partido serializado; `before` nulo en el alta, `after` nulo en el borrado).
- Índices: único `(league_id, matchday)`, único `(league_id, kickoff_at)`, `match_audit_entry (match_id, occurred_at)`.

**Orden cronológico:** la «jornada anterior» (RF-28) es el partido con `kickoff_at` inmediatamente anterior, no el de número de jornada menor (decisión 12). Por eso `kickoff_at` es único por liga: el equipo no juega dos partidos a la vez y así el orden es total, sin empates. El número de jornada queda como etiqueta para la interfaz.

**Por qué columnas fijas y no una fila por acción:** las 9 acciones son un enum cerrado de `datadefinition` y casi todas son contadores. Una fila por jugador y partido se lee y valida mejor (los `CHECK` de tarjetas) que un modelo EAV. Si se añade una acción, se añade una columna con su migración, igual que se amplía el `CHECK` de `league_scoring_rule`.

## 3. Relación con otros módulos

- **`squad` → nueva API pública `SquadApi`** (paquete raíz, D10): `List<SquadPlayer> findPlayers(UUID leagueId, Collection<UUID> playerIds)` con el record `SquadPlayer(UUID id, Position position, boolean active)`. Una sola consulta, sin N+1. `match` la usa al registrar el acta para validar que los jugadores son de la liga y tomar su posición.
- **Dependencias de `match`:** `league`, `squad`, `datadefinition`, `shared`. `squad` **no** puede depender de `match` (sería un ciclo).
- **Eventos publicados** (paquete raíz de `match`, `shared.domain.DomainEvent`):
  - `MatchClosed(leagueId, matchId, matchday, kickoffAt, performances, occurredAt)`.
  - `MatchCorrected(leagueId, matchId, matchday, kickoffAt, performances, occurredAt)` (`kickoffAt` lo añadió el plan 08 para que `scoring` trate igual las dos actas).
  - `MatchRescheduled(leagueId, matchId, matchday, previousKickoffAt, kickoffAt, occurredAt)` y `MatchDeleted(leagueId, matchId, matchday, occurredAt)`: contrato para `lineup` y `notification`, sin consumidores todavía. `lineup` usará `MatchRescheduled` para **desbloquear** las alineaciones de un partido aplazado (decisión 13).
  - `performances` es una lista de records `PlayerPerformanceData` con la posición y todas las estadísticas. **El evento lleva el acta completa** (son ~15 filas): `scoring` y `rating` guardan su propia copia sin consultar a `match`, y aplicar dos veces el mismo evento da el mismo resultado (los listeners son *at-least-once*). Así `match` se podría extraer a servicio sin cambiar el contrato.
- **`MatchApi`** (próximo partido, fecha de inicio por jornada) la necesitará `lineup` para el bloqueo. Se crea en ese plan, con su primer consumidor, como se hizo con `LeagueSettingsProvider`.

## 4. Dominio (`match/domain`, sin Spring ni JPA)

- `Match` (aggregate root):
  - `schedule(leagueId, matchday, opponent, venue, kickoffAt, now)`.
  - `updateDetails(opponent, venue)`: siempre permitido.
  - `reschedule(matchday, kickoffAt)`: solo `SCHEDULED`; publica `MatchRescheduled` si cambia algo.
  - `recordSheet(MatchSheet sheet, now)`: en `SCHEDULED` solo guarda; en `CLOSED` es una **corrección** y publica `MatchCorrected`. Si el acta no cambia, no publica nada.
  - `close(now)`: exige `now ≥ kickoffAt`, acta con resultado y al menos un participante; publica `MatchClosed`. Cerrar un partido ya cerrado no hace nada (idempotente, sin evento).
  - `delete(now)`: solo `SCHEDULED`; publica `MatchDeleted`.
- `MatchSheet` (VO): `MatchResult(goalsFor, goalsAgainst)` + lista de `PlayerPerformance`. Sin jugadores repetidos; como mucho 30 participantes. `warnings()` devuelve `MatchSheetWarning` (`GOALS_MISMATCH`, `OWN_GOALS_EXCEED_GOALS_AGAINST`) sin lanzar excepción.
- `PlayerPerformance` (VO): `playerId`, `position`, los seis contadores y las tres tarjetas. Invariantes: contadores 0–99, doble amarilla incompatible con amarilla y con roja directa.
- `MatchId`, `Matchday` (VO ≥ 1), `Venue` (`HOME`, `AWAY`), `MatchStatus` (`SCHEDULED`, `CLOSED`), `MatchAuditAction` (`SCHEDULED`, `DETAILS_UPDATED`, `RESCHEDULED`, `SHEET_RECORDED`, `CLOSED`, `CORRECTED`, `DELETED`).
- La unicidad de la jornada se garantiza con el índice único. El handler la comprueba antes para devolver un `409` limpio y captura la violación de la constraint como red de seguridad, igual que el dorsal en `squad`.

## 5. Casos de uso (`match/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `ScheduleMatchCommand` | command | `requireAdmin` | Alta (RF-12). Jornada repetida u otro partido a la misma hora → `409`. |
| `UpdateMatchCommand` | command | `requireAdmin` | Reemplaza rival, local/visitante, jornada y fecha. Si está cerrado, jornada y fecha deben coincidir con las actuales (`409` si no). |
| `DeleteMatchCommand` | command | `requireAdmin` | Solo `SCHEDULED` (`409` si está cerrado). Borrado físico con su entrada de auditoría. |
| `RecordMatchSheetCommand` | command | `requireAdmin` | Reemplaza el acta (RF-13, RF-14). Valida los jugadores con `SquadApi`: todos de la liga (`400` si no); un jugador **nuevo** en el acta debe estar activo, y uno que ya estaba se conserva aunque se haya dado de baja después. Toma la posición de `squad` para los nuevos y conserva la guardada para los que ya estaban. Devuelve los avisos. En un partido cerrado es la corrección de RF-16. |
| `CloseMatchCommand` | command | `requireAdmin` | Cierre. Antes de la hora de inicio o sin acta → `409`. |
| `ListMatchesQuery` | query | `requireMember` | Calendario por jornada. Resultado solo en los cerrados (o para el admin). Una consulta. |
| `GetMatchQuery` | query | `requireMember` | Detalle. Acta solo si está cerrado o si lo pide el admin; los avisos solo para el admin. Dos consultas (partido + participantes). |
| `ListMatchAuditQuery` | query | `requireAdmin` | Historial de cambios del partido, del más reciente al más antiguo. |

Cada handler recibe `userId` y `leagueId`, llama a `LeagueAccess` primero y carga el partido **por `(leagueId, matchId)`** para impedir acceso cruzado entre ligas (IDOR).

**Auditoría:** puerto `MatchAuditLog` en `application/port/out` (`append(MatchAuditEntry)`). Cada command handler, en la misma transacción que la mutación, guarda la entrada con el `userId`, la acción y el estado antes/después (record `MatchSnapshot` que el adaptador serializa a JSONB con Jackson). Una mutación que no cambia nada (cerrar dos veces, el mismo acta) no deja entrada.

## 6. API HTTP (`match/adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `POST /api/leagues/{id}/matches` | `ScheduleMatchCommand` → `201` |
| `GET /api/leagues/{id}/matches` | `ListMatchesQuery` |
| `GET /api/leagues/{id}/matches/{matchId}` | `GetMatchQuery` |
| `PUT /api/leagues/{id}/matches/{matchId}` | `UpdateMatchCommand` |
| `DELETE /api/leagues/{id}/matches/{matchId}` | `DeleteMatchCommand` → `204` |
| `PUT /api/leagues/{id}/matches/{matchId}/sheet` | `RecordMatchSheetCommand` → `200` con `{warnings}` |
| `POST /api/leagues/{id}/matches/{matchId}/close` | `CloseMatchCommand` → `200` |
| `GET /api/leagues/{id}/matches/{matchId}/audit` | `ListMatchAuditQuery` |

Las fechas se envían y devuelven en ISO-8601 (`Instant`, UTC). Todos los endpoints exigen sesión (`401`) y CSRF en los que mutan. Autorización en el handler (`403`); partido inexistente o de otra liga → `404`. `409` por jornada duplicada u otro partido a la misma hora, borrar o cambiar la fecha de un partido cerrado, o cerrar antes de tiempo o sin acta. `400` por datos inválidos (tarjetas incompatibles, contadores fuera de rango, jugador repetido, de otra liga o dado de baja y nuevo en el acta). Los avisos de coherencia no son errores. Los avisos se devuelven como códigos (`GOALS_MISMATCH`…); el frontend los traduce.

## 7. Tests

| Test | Qué verifica |
| --- | --- |
| Migración V8 | Aplica sobre Postgres limpio y `ddl-auto: validate` pasa; los `CHECK` de tarjetas, del resultado y de `closed_at` rechazan datos incoherentes; la jornada y la hora de inicio son únicas por liga. |
| `PlayerPerformance` (unitarios) | Contadores fuera de rango; doble amarilla con amarilla o con roja → error; amarilla + roja directa válida. |
| `MatchSheet` (unitarios) | Jugador repetido y más de 30 participantes → error; avisos `GOALS_MISMATCH` (cuenta goles + goles de penalti) y `OWN_GOALS_EXCEED_GOALS_AGAINST`; sin avisos cuando cuadra. |
| `Match` (unitarios, plazos con `Clock` fijo) | Cerrar antes de `kickoffAt` → error; justo en `kickoffAt` → cierra; sin resultado o sin participantes → error; cerrar dos veces publica `MatchClosed` una sola vez; acta en un partido cerrado publica `MatchCorrected`, y la misma acta no publica nada; no se cambia la fecha ni se borra un partido cerrado. |
| Alta y edición | Admin crea y edita; manager → `403`; admin de otra liga → `403`; jornada repetida u hora de inicio repetida → `409`; cambiar la fecha publica `MatchRescheduled` con la fecha anterior y la nueva. |
| Acta | Jugador de otra liga → `400`; jugador dado de baja nuevo en el acta → `400`, pero si ya estaba se conserva; la posición se toma de `squad` al añadirlo y no cambia si luego se edita el jugador. |
| Visibilidad | Un manager no ve el resultado ni el acta de un partido sin cerrar; sí después del cierre; el admin lo ve siempre, con avisos. |
| Aislamiento entre ligas | Leer, editar o cerrar un partido de la liga A usando la ruta de la B → `404`. |
| Auditoría | Cada mutación deja una entrada con autor, acción y antes/después; el borrado conserva el historial; operaciones sin cambios no dejan entrada; manager → `403` en el historial. |
| Eventos | `MatchClosed` y `MatchCorrected` llevan el acta completa con posiciones; cada uno se publica una vez por operación (`Scenario` de Spring Modulith). |
| `SquadApi` | Devuelve solo jugadores de la liga pedida, con posición y estado; una consulta. |
| Seguridad | Todos los endpoints sin sesión → `401`; mutaciones sin CSRF → `403` (ampliar `ApiSecurityTests`). |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde (`match` depende de `league`, `squad`, `datadefinition` y `shared`; sin ciclos). |

## 8. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | Migración V8, `Match`, `MatchSheet`, `PlayerPerformance` y eventos, con tests unitarios; `allowedDependencies` de `match` | Dominio verificado | ✅ |
| 2 | `SquadApi` en `squad` con su test | Jugadores consultables desde otros módulos | ✅ |
| 3 | Persistencia, puerto y adaptador de auditoría, `ScheduleMatchCommand`, `ListMatchesQuery`, `GetMatchQuery` y sus endpoints | RF-12 | ✅ |
| 4 | `UpdateMatchCommand` y `DeleteMatchCommand` | Aplazados y suspendidos | ✅ |
| 5 | `RecordMatchSheetCommand` (con avisos y visibilidad) y `CloseMatchCommand` | RF-13, RF-14 y `MatchClosed` | ✅ |
| 6 | Correcciones en partidos cerrados y `ListMatchAuditQuery` | RF-16 y RNF-04 | ✅ |
| 7 | Tests de seguridad y arquitectura; `AGENTS.md` (estado) y marcar el ítem en `requirements.md`, indicando que el cálculo de RF-15 llega con `scoring` | Ítem de Fase 1 cerrado | ✅ |

Cada tarea es un commit `feat(match): … (RF-12)` en Conventional Commits.

## 9. Riesgos y notas

- **Bloqueo del valor inicial** (decisión confirmada 1 del plan 04): debía bloquearse al cerrar la primera jornada, pero `squad` no puede escuchar `MatchClosed` sin crear un ciclo `squad ↔ match`. Se resuelve en el plan de `market`, que será dueño del valor vigente; hasta entonces no se aplica.
- **Aplazar tras el bloqueo:** al desbloquearse las alineaciones de un partido aplazado (decisión 13), un manager podría volver a cambiarlas. `lineup` decidirá si los cambios hechos antes del bloqueo siguen contando para el límite de la jornada (propuesta: sí; el aplazamiento no regala cambios).
- **Orden de jornadas y fechas:** con aplazamientos, la jornada 5 puede jugarse después de la 7. No se valida que la jornada siga el orden de las fechas, porque el orden que cuenta es el de `kickoff_at` (decisión 12).
- **Avisos de coherencia:** solo cuentan los goles de los jugadores propios. Un gol en propia de un jugador nuestro cuenta para el rival.
- **Auditoría y RGPD:** `actor_user_id` es un dato personal mínimo. Al implementar el borrado de cuenta (Fase 4, RNF-10) se anonimiza ese campo sin borrar la entrada.
- **Concurrencia:** solo hay un admin por liga; no se añade bloqueo optimista. Si dos pestañas guardan el acta a la vez, gana la última, y la auditoría deja ambos cambios.
- **Rendimiento:** ~30 partidos por temporada y ~15 participantes por partido. Listado en una consulta y detalle en dos; cumple RNF-09.

## 10. Decisiones

Cerradas:

1. **Jornada:** 1 jornada = 1 partido; número de jornada único por liga, sin entidad propia.
2. **Convocatoria:** solo quién jugó, tras el partido; sin minutos jugados.
3. **Tarjetas:** amarilla + roja directa suman (−6); la doble amarilla excluye a ambas (−3 en total).
4. **Cálculo de puntos:** en `scoring`, no en `match`; `match` publica el acta completa en `MatchClosed` y `MatchCorrected`.
5. **Visibilidad:** los managers ven el acta solo cuando el partido está cerrado.
6. **Coherencia:** avisos sin bloqueo si los goles no cuadran con el resultado.
7. **Aplazados y suspendidos:** se cambia la fecha o se borra mientras no esté cerrado; no hay estado «cancelado».
8. **Correcciones y auditoría:** ahora, con log de solo inserción (antes/después en JSONB) y evento `MatchCorrected`.
9. **Acta en una operación:** resultado y participantes se guardan juntos (un reemplazo, una entrada de auditoría, un evento de corrección).
10. **Posición en el acta:** foto de la posición al añadir al jugador; los cambios posteriores en `squad` no alteran partidos pasados.
11. **API de `squad`:** `SquadApi.findPlayers`; `MatchApi` se crea con `lineup`.

Para planes posteriores (`match` ya publica lo que necesitan):

12. **Jornada anterior (RF-28):** la que se jugó antes **cronológicamente** (`kickoff_at`), sin importar el número de jornada. Por eso `kickoff_at` es único por liga.
13. **Aplazar con alineaciones bloqueadas:** se **desbloquean**. `lineup` recalcula el bloqueo con la nueva fecha al recibir `MatchRescheduled`.
14. **Jugador quitado en una corrección:** sus valoraciones de ese partido **se descartan** (se borran). `rating` las elimina al recibir `MatchCorrected`, aunque la ventana ya esté cerrada, y la media se recalcula; `scoring` recalcula la jornada con el mismo evento.

No quedan decisiones abiertas para este paso.
