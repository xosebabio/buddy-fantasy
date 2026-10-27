# 06 · Alineaciones (`lineup`) — plan

Cuarto ítem de la **Fase 1 · Núcleo jugable** (`requirements.md`): *alineaciones: edición, bloqueo X horas antes y límite de cambios* (RF-24 a RF-29). Se apoya en `league` (roles y parámetros por liga), en `squad` (jugadores, posición y valor) y en `match` (calendario y eventos de aplazamiento y cierre). Publica las alineaciones bloqueadas para que `scoring` calcule los puntos del manager (RF-31) en el siguiente plan.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Quién alinea | Todo miembro de la liga: `MANAGER` y `ADMIN` (el admin siempre juega como manager). |
| Exclusividad | **Ninguna**: varios managers pueden alinear al mismo jugador. En el futuro será un parámetro de la liga; ahora no se modela. |
| Alineación de trabajo | Cada manager tiene **una única alineación viva** por liga, que evoluciona con el tiempo. Cada guardado crea una **versión** inmutable con su instante (`saved_at`). Si el manager no la toca, la misma alineación sigue valiendo para el partido siguiente (RF-28, se arrastra sola). |
| Tamaño | Exactamente `lineupSize` jugadores (RF-24), sin repetir, todos **activos** y de la liga. |
| Formación (RF-24) | Si `lineupFormationEnabled`, la alineación debe tener **un mínimo de jugadores por posición**, configurable por liga (por defecto 1 portero, 1 defensa, 1 medio y 1 delantero). Sin máximo. Cuenta la posición actual del jugador en `squad` al guardar. |
| Bloqueo (RF-25, RF-27) | La alineación de un partido es la última versión guardada **antes de** `lockAt = kickoffAt − lineupLockHours`. Lo que se guarda después cuenta para el partido siguiente: **se puede preparar enseguida la jornada N+1** aunque el partido N aún no se haya jugado. El plazo lo marca el reloj del servidor; nunca se rechaza un guardado por llegar tarde, simplemente cuenta para el siguiente partido. |
| Cambios (RF-26) | Se cuenta la **diferencia final**: cambios = jugadores de la alineación nueva que no estaban en la alineación bloqueada del partido anterior. Poner y quitar a alguien no gasta cambio. Sustituir a un jugador dado de baja es **gratis**, y también lo son los huecos si ha crecido `lineupSize`. La **primera** alineación de un manager (sin alineación bloqueada previa) no tiene límite. |
| Presupuesto (RF-29) | Si `lineupBudgetEnabled`, la suma de valores vigentes no puede superar `lineupBudget` **al guardar** (`400`). **Al bloquear** se vuelve a comprobar con el valor vigente: si la supera, la alineación queda bloqueada pero **no puntúa esa jornada** (0 puntos; lo aplica `scoring`). Mientras `market` no exista, el valor vigente es el inicial, que el admin puede editar. |
| Jugador dado de baja | Una alineación que lo contiene se arrastra igual y él no puntúa. Para guardar cambios hay que sustituirlo (la alineación nueva solo admite activos) y esa sustitución es gratis. |
| Visibilidad | Cada manager ve solo **su** alineación de trabajo. Las alineaciones de los demás se ven **solo cuando están bloqueadas** (para el detalle de RF-33). |
| Aplazamiento | Si el partido se aplaza y el nuevo `lockAt` queda en el futuro, sus alineaciones se **desbloquean** (decisión 13 del plan 05) y se vuelven a bloquear con la nueva fecha. Los cambios ya hechos siguen contando: es la misma diferencia contra la misma alineación anterior. |

Fuera de alcance: frontend, cálculo de puntos y clasificaciones (`scoring`), valor de mercado dinámico (`market`), aviso de «alineación a punto de bloquearse» (`notification`), exclusividad de jugadores como parámetro, capitán o suplentes, alineaciones distintas preparadas para varias jornadas futuras.

## 2. Modelo de datos (migraciones nuevas, no se edita V1–V8)

**V9__league_lineup_formation.sql** (módulo `league`)

- `league_settings`: añade `lineup_min_goalkeepers`, `lineup_min_defenders`, `lineup_min_midfielders`, `lineup_min_forwards` SMALLINT NOT NULL DEFAULT 1 `CHECK (… >= 0)`.
- Añade `CHECK (lineup_lock_hours <= 168)`: el bloqueo no puede ser de más de una semana antes del partido. Así el job sabe qué partidos le interesan (§5).

**V10__lineup_create_tables.sql**

- `lineup_version` (historial de la alineación de trabajo):
  - `id` UUID PK, `league_id` UUID NOT NULL, `user_id` UUID NOT NULL (referencias lógicas, sin FK entre módulos).
  - `saved_at` TIMESTAMPTZ NOT NULL. Índice `(league_id, user_id, saved_at DESC)`.
- `lineup_version_player`: `version_id` UUID NOT NULL `REFERENCES lineup_version (id) ON DELETE CASCADE`, `player_id` UUID NOT NULL. PK `(version_id, player_id)`.
- `lineup_match_lock` (bloqueo de un partido, materializado por el job):
  - `match_id` UUID PK, `league_id` UUID NOT NULL, `matchday` SMALLINT NOT NULL, `kickoff_at` TIMESTAMPTZ NOT NULL, `lock_at` TIMESTAMPTZ NOT NULL, `locked_at` TIMESTAMPTZ NOT NULL.
  - Índice `(league_id, kickoff_at)`.
- `lineup_locked_lineup`: `match_id` UUID NOT NULL `REFERENCES lineup_match_lock (match_id) ON DELETE CASCADE`, `user_id` UUID NOT NULL, `version_id` UUID NOT NULL `REFERENCES lineup_version (id)`, `total_value` NUMERIC(10, 2) NOT NULL, `budget` NUMERIC(10, 2) NULL (nulo si el presupuesto está desactivado), `over_budget` BOOLEAN NOT NULL. PK `(match_id, user_id)`.
- `lineup_locked_player`: `(match_id, user_id)` FK a `lineup_locked_lineup` ON DELETE CASCADE, `player_id` UUID NOT NULL, `value` NUMERIC(10, 2) NOT NULL (valor vigente al bloquear). PK `(match_id, user_id, player_id)`.

**Por qué versiones y no una fila por jornada:** el bloqueo es una función del tiempo (la última versión antes de `lockAt`), así que el arrastre (RF-28), el aplazamiento y el cambio de orden de los partidos salen solos, sin copiar alineaciones ni recalcular a qué jornada pertenece cada guardado. El volumen es pequeño: unos 15 managers × unos 30 partidos × pocos guardados por jornada. Un guardado igual a la versión actual no crea una versión nueva.

**Por qué materializar el bloqueo:** congela el valor vigente y el resultado del presupuesto en el instante del bloqueo (luego `market` cambiará los valores), da a la visibilidad un estado claro (bloqueada o no) y es el punto donde se publica `LineupsLocked` una sola vez.

## 3. Relación con otros módulos

- **`league`:**
  - Nueva API pública `LeagueSettingsProvider` (paquete raíz, prevista en el plan 03): `LineupSettings lineupSettings(UUID leagueId)` y `Map<UUID, LineupSettings> lineupSettings(Collection<UUID> leagueIds)` (para el job, una consulta). Record `LineupSettings(size, lockHours, maxChanges, formationEnabled, minGoalkeepers, minDefenders, minMidfielders, minForwards, budgetEnabled, budget)`.
  - `LeagueAccess` gana `Set<UUID> memberIds(UUID leagueId)`: al bloquear solo cuentan las alineaciones de quien sigue siendo miembro.
  - `LeagueSettings` añade los cuatro mínimos (por defecto 1), valida `lockHours ≤ 168` y, si la formación está activa, que la suma de mínimos no supere `lineupSize`.
- **`squad`:** `SquadPlayer` añade `initialValue`. `lineup` no lo usa directamente: su puerto de salida `PlayerCatalog` devuelve `LineupPlayer(id, position, active, currentValue)`, con un adaptador sobre `SquadApi` que hoy toma `currentValue = initialValue`. El plan de `market` cambia solo ese adaptador.
- **`match` → nueva API pública `MatchApi`** (paquete raíz, prevista en el plan 05):
  - `List<MatchSchedule> findMatches(UUID leagueId)`: todos los partidos de la liga (unos 30) en una consulta, ordenados por `kickoffAt`.
  - `List<MatchSchedule> findScheduledKickingOffBefore(Instant until)`: partidos `SCHEDULED` de todas las ligas, para el job.
  - Record `MatchSchedule(UUID id, UUID leagueId, int matchday, Instant kickoffAt, boolean closed)`.
- **Eventos consumidos** (`lineup/adapter/in/event`, `@ApplicationModuleListener`):
  - `MatchClosed`: bloquea el partido si aún no lo estaba (red de seguridad si el job no llegó a correr o el partido se cargó ya jugado).
  - `MatchRescheduled`: si el nuevo `lockAt` es futuro, borra el bloqueo y publica `LineupsUnlocked`.
  - `MatchDeleted`: borra el bloqueo, sin evento.
- **Eventos publicados** (paquete raíz de `lineup`):
  - `LineupsLocked(leagueId, matchId, matchday, kickoffAt, lineups, occurredAt)`, con `lineups` como lista de `LockedLineupData(userId, playerIds, totalValue, overBudget)`. **El evento lleva todas las alineaciones** (unas 15): `scoring` guarda su propia copia y calcula cuando tenga `MatchClosed` y `LineupsLocked` del mismo partido, lleguen en el orden que lleguen. Si `overBudget`, el manager suma 0 esa jornada.
  - `LineupsUnlocked(leagueId, matchId, occurredAt)`: `scoring` descarta su copia.
- **Dependencias de `lineup`:** `league`, `squad`, `match`, `datadefinition`, `shared`. Ninguno de ellos depende de `lineup`.

## 4. Dominio (`lineup/domain`, sin Spring ni JPA)

- `LineupVersion` (VO): `leagueId`, `userId`, `savedAt` y el conjunto de `playerIds`.
- `LineupTimeline` (servicio de dominio puro): recibe los partidos de la liga (`MatchSchedule`), `lockHours`, las versiones del manager y `now`, y responde:
  - `lockAt(match)` = `kickoffAt − lockHours`.
  - `targetMatch(now)`: primer partido no cerrado con `lockAt > now` (puede no haber).
  - `baseline(now)`: alineación bloqueada del último partido con `lockAt ≤ now`, es decir, la última versión con `savedAt < lockAt` de ese partido (puede no haber).
  - `lineupFor(match)`: la última versión con `savedAt < lockAt(match)`.
- `LineupRules` (servicio de dominio puro): valida una alineación nueva contra `LineupSettings` y los `LineupPlayer`:
  - tamaño exacto y sin repetidos; todos activos y de la liga;
  - mínimos por posición si la formación está activa;
  - suma de valores ≤ presupuesto si está activo;
  - `changes(baseline, nueva, retirados, size)` = `max(0, |nueva \ baseline| − gratis)`, con `gratis` = jugadores retirados de `baseline` + `max(0, size − |baseline|)`. Sin `baseline`, no hay límite.
- `MatchLineupLock` (aggregate del bloqueo): `lock(match, lockAt, lineups, now)` calcula `totalValue` y `overBudget` de cada alineación y publica `LineupsLocked`; `unlock(now)` publica `LineupsUnlocked`.
- Errores: `DomainValidationException` (`400`) por tamaño, repetidos, jugador inactivo o ajeno, formación o presupuesto; `ConflictException` (`409`) por superar el límite de cambios.

## 5. Casos de uso y job (`lineup/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `SaveLineupCommand` | command | `requireMember` | Reemplaza la alineación de trabajo (RF-24, RF-26, RF-29). Carga el calendario (`MatchApi`), los parámetros, las versiones del manager y los jugadores (`PlayerCatalog`), valida con `LineupRules` y guarda una versión con `saved_at = now`. Si es igual a la actual, no hace nada. Devuelve el mismo estado que `GetMyLineupQuery`. |
| `GetMyLineupQuery` | query | `requireMember` | Alineación de trabajo, partido objetivo (jornada, `kickoffAt`, `lockAt`), cambios usados y restantes, valor total y presupuesto, y avisos: `OVER_BUDGET` (con los valores vigentes ya no cabe), `RETIRED_PLAYERS` y `SIZE_MISMATCH`. Sin alineación devuelve una vacía. |
| `GetLockedLineupsQuery` | query | `requireMember` | Alineaciones bloqueadas de un partido, con el valor de cada jugador al bloquear y `overBudget`. Partido sin bloquear → `409`; partido de otra liga → `404`. |

**Bloqueo:** el componente `LineupLocker.lock(leagueId, matchId)` es idempotente. Inserta la fila de `lineup_match_lock` con `ON CONFLICT DO NOTHING` y solo quien la inserta guarda las alineaciones y publica el evento. Por cada miembro actual (`LeagueAccess.memberIds`) toma `lineupFor(match)`; quien no tiene versión antes de `lockAt` no tiene alineación en ese partido. El presupuesto se comprueba con los valores vigentes en ese momento.

**Job `LockDueLineupsJob`** (`@Scheduled` cada minuto + `@SchedulerLock`):

1. Pide `MatchApi.findScheduledKickingOffBefore(now + 168 h)` y los parámetros de esas ligas.
2. Bloquea los partidos con `lockAt ≤ now − 1 min` que aún no estén en `lineup_match_lock`.

El margen de un minuto garantiza que un guardado con `saved_at < lockAt` ya está confirmado cuando se lee. Ejecutarlo dos veces no duplica nada.

## 6. API HTTP (`lineup/adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `GET /api/leagues/{id}/lineup` | `GetMyLineupQuery` |
| `PUT /api/leagues/{id}/lineup` | `SaveLineupCommand` (`{playerIds: [...]}`) → `200` con el estado |
| `GET /api/leagues/{id}/lineups/{matchId}` | `GetLockedLineupsQuery` |

Todos exigen sesión (`401`) y CSRF en el `PUT`. Autorización en el handler (`403` si no es miembro). `400` por alineación inválida (con código: `SIZE`, `DUPLICATE_PLAYER`, `INACTIVE_PLAYER`, `UNKNOWN_PLAYER`, `FORMATION`, `OVER_BUDGET`); `409` `MAX_CHANGES_EXCEEDED` o partido sin bloquear. Las fechas van en ISO-8601 (`Instant`, UTC).

## 7. Tests

| Test | Qué verifica |
| --- | --- |
| Migraciones V9 y V10 | Aplican sobre Postgres limpio y `ddl-auto: validate` pasa; los mínimos por defecto valen 1; `lineup_lock_hours > 168` se rechaza. |
| `LeagueSettings` (unitarios) | Mínimos negativos, suma de mínimos mayor que el tamaño con la formación activa, o `lockHours > 168` → error. |
| `LineupTimeline` (unitarios, `Clock` fijo) | Guardado justo antes de `lockAt` cuenta para ese partido; justo en `lockAt`, para el siguiente; sin tocar, se arrastra la del partido anterior (RF-28); con un aplazamiento cambia el `lockAt` y la versión que cuenta; partidos ordenados por `kickoffAt` y no por número de jornada. |
| `LineupRules` (unitarios) | Tamaño, repetidos, inactivos; mínimos por posición; presupuesto justo en el límite (válido) y por encima (error); cambios por diferencia final (poner y quitar no cuenta), sustituir a un retirado es gratis, sin `baseline` no hay límite, Y+1 cambios → error. |
| `SaveLineupCommand` | Manager y admin guardan; no miembro → `403`; jugador de otra liga → `400`; guardar lo mismo no crea versión; tras el bloqueo del partido N los cambios cuentan contra la alineación de N. |
| Bloqueo y job | El job bloquea solo partidos con `lockAt ≤ now − 1 min`; ejecutarlo dos veces publica `LineupsLocked` una vez; `MatchClosed` bloquea si el job no llegó; un partido cargado ya jugado se bloquea con las versiones previas a su `lockAt`; un exmiembro no aparece; presupuesto superado al bloquear → `overBudget = true`. |
| Aplazamiento y borrado | `MatchRescheduled` con `lockAt` futuro desbloquea y publica `LineupsUnlocked`; con `lockAt` aún pasado no cambia nada; al volver a bloquear se usan las versiones guardadas antes del nuevo `lockAt`; `MatchDeleted` borra el bloqueo. |
| Visibilidad y aislamiento | Un manager no ve la alineación de trabajo de otro; las bloqueadas sí; partido de la liga A por la ruta de la B → `404`. |
| `MatchApi`, `LeagueSettingsProvider`, `memberIds` | Devuelven solo datos de la liga pedida y en una consulta. |
| Seguridad | Endpoints sin sesión → `401`; `PUT` sin CSRF → `403` (ampliar `ApiSecurityTests`). |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde (`lineup` depende de `league`, `squad`, `match`, `datadefinition` y `shared`; sin ciclos). |

## 8. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | V9, mínimos por posición y tope de `lockHours` en `LeagueSettings` (dominio, persistencia y API de parámetros); `LeagueSettingsProvider` y `memberIds` | Parámetros de formación configurables | ✅ |
| 2 | `MatchApi` en `match` e `initialValue` en `SquadPlayer`, con sus tests | Calendario y valores consultables | |
| 3 | V10, `LineupTimeline`, `LineupRules`, `MatchLineupLock` y eventos, con tests unitarios; `allowedDependencies` de `lineup` | Dominio verificado | |
| 4 | Persistencia, `PlayerCatalog`, `SaveLineupCommand`, `GetMyLineupQuery` y sus endpoints | RF-24, RF-26, RF-28, RF-29 | |
| 5 | `LineupLocker`, `LockDueLineupsJob` y listeners de `MatchClosed`, `MatchRescheduled` y `MatchDeleted` | RF-25, RF-27 y `LineupsLocked` | |
| 6 | `GetLockedLineupsQuery` y visibilidad | Alineaciones bloqueadas visibles | |
| 7 | Tests de seguridad y arquitectura; `AGENTS.md` (estado) y marcar el ítem en `requirements.md` | Ítem de Fase 1 cerrado | |

Cada tarea es un commit `feat(lineup): … (RF-24)` en Conventional Commits.

## 9. Riesgos y notas

- **Valor inicial editable:** mientras `market` no exista, el admin puede subir el valor inicial y dejar alineaciones fuera de presupuesto, que al bloquear no puntuarían. `GetMyLineupQuery` lo avisa con `OVER_BUDGET`. El bloqueo de la edición del valor inicial tras la primera jornada cerrada (decisión confirmada 1 del plan 04) sigue pendiente para `market`.
- **Presupuesto y mercado:** cuando `market` mueva los valores tras cada jornada, una alineación arrastrada puede quedar fuera de presupuesto sin que el manager haga nada. La regla es no puntuar (decidido). Conviene que `notification` avise de `OVER_BUDGET` antes del bloqueo.
- **Cambios con aplazamientos:** si un partido se aplaza a después del siguiente, el orden cronológico cambia y la alineación anterior de referencia puede ser otra. Los guardados ya hechos no se invalidan con efecto retroactivo; el límite se aplica en el siguiente guardado.
- **Parámetros que cambian a mitad de temporada:** el bloqueo usa los parámetros vigentes al bloquear. Una alineación arrastrada con otro tamaño o sin cumplir la formación se bloquea tal cual (solo se vuelve a comprobar el presupuesto); el aviso `SIZE_MISMATCH` lo indica.
- **Miembros que salen:** su alineación deja de contar en los partidos que se bloqueen después; las versiones se conservan hasta el borrado de cuenta (Fase 4, RNF-10), que deberá borrarlas.
- **Job en un VPS:** `findScheduledKickingOffBefore` recorre los partidos programados de todas las ligas; con pocas ligas es trivial. En cloud con muchas ligas, paginar o indexar por `kickoff_at`.
- **Rendimiento:** guardar son ~4 consultas (calendario, parámetros, versiones, jugadores) y un insert; consultar la propia alineación, igual. Sin N+1. Cumple RNF-09.

## 10. Decisiones

Cerradas:

1. **Exclusividad:** varios managers pueden alinear al mismo jugador; en el futuro será un parámetro de la liga.
2. **El admin juega:** el admin es también manager de su liga y alinea como cualquier otro.
3. **Cambios:** diferencia final contra la alineación bloqueada del partido anterior; sustituir retirados y rellenar huecos es gratis; la primera alineación no tiene límite.
4. **Tras el bloqueo:** se puede preparar la jornada siguiente enseguida; lo guardado después de `lockAt` cuenta para el partido siguiente.
5. **Formación:** mínimo de jugadores por posición configurable por liga (por defecto 1 portero y 1 por línea), sin máximo.
6. **Presupuesto al bloquear:** si la alineación supera el presupuesto con el valor vigente al bloquear, no puntúa esa jornada.
7. **Modelo:** una alineación viva versionada por manager; el bloqueo se materializa por partido y se publica en `LineupsLocked` con todas las alineaciones.
8. **Visibilidad:** las alineaciones de los demás solo se ven bloqueadas.

No quedan decisiones abiertas para este paso.
