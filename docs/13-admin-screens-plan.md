# 13 · Pantallas de admin — plan

Plan de los ítems de la **Fase 2** _Pantallas de admin: parámetros y tabla de puntos, miembros e invitación, jugadores y foto, partidos y acta, cancelación de valoraciones (RF-06, RF-07, RF-09, RF-10, RF-12 a RF-16, RF-41)_ y _E2E de la jornada completa en móvil (FE-34)_. Miembros e invitación (RF-06) ya están hechos en el plan 11. Aquí se construye lo demás que el administrador necesita para llevar una liga desde la interfaz, sin tocar el API: parámetros y tabla de puntos, plantilla, calendario, acta, cierre, correcciones con su auditoría y la cancelación de valoraciones. También el E2E en el que el admin hace todo eso desde la interfaz. El backend ya existe (planes 03, 04, 05 y 09). Solo se añade un endpoint de límites por módulo, para que los formularios validen antes de enviar sin repetir ningún límite en el cliente (§3). Las reglas son las de `docs/requirements.md` (RNF-01, RNF-12 a RNF-16, FE-01 a FE-35), resumidas en `docs/agents/frontend.md`, y la dirección visual de `frontend/src/app/ui/README.md`. Este plan no las repite: dice cómo y en qué orden se aplican.

## 1. Alcance

| Tema             | Decisión                                                                                                                                                                                                                                                                                                                                                                             |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Backend          | El API de admin ya existe y está en el spec. Se añaden los límites de `league`, `squad` y `match`, un endpoint por módulo (§3).                                                                                                                                                                                                                                                      |
| Contrato         | Los métodos de admin en `SquadApi`, `MatchApi` y `RatingApi`, y los límites en `LeagueApi`, `SquadApi` y `MatchApi`. `LeagueApi` ya tiene `updateSettings` (§5).                                                                                                                                                                                                                     |
| Diseño           | Boceto de las pantallas de admin con la skill `frontend-design`, revisado antes de construir. La composición se documenta en `ui/README.md` (§6).                                                                                                                                                                                                                                    |
| Componentes      | `bfCheckbox` y `bfSwitch` en `ui/`, `bf-stepper` sin límites obligatorios y la fila de partido extraída a `shared/` (§6.3, §6.4).                                                                                                                                                                                                                                                    |
| Pantallas        | Parámetros y tabla de puntos, plantilla (alta, edición y baja), calendario (programar, editar, aplazar y borrar), partido, acta con cierre y correcciones, auditoría y cancelación de valoraciones (§7).                                                                                                                                                                             |
| Accesos          | La sección _Administrar_ del índice _Liga_, enlaces de admin en las pantallas de miembro y una fila de admin en el inicio de la jornada (§4.2, §7).                                                                                                                                                                                                                                  |
| Calidad          | Tests de componentes, servicios, mapeos y páginas, y un E2E nuevo del admin en móvil con axe y el reloj de pruebas (§9).                                                                                                                                                                                                                                                             |
| Fuera de alcance | La interfaz de fotos de los jugadores (decisión 1). Los parámetros de mercado (decisión 3, Fase 3). El recálculo forzado por el admin (RF-16, Fase 4). Los minutos jugados (RF-13: el plan 05 los dejó fuera). Reactivar un jugador dado de baja y renombrar la liga (el API no lo permite). Las notificaciones (Fase 3). El Dockerfile, el Compose de producción y el VPS (Fase 0). |

## 2. Decisiones

Las marcadas como _Usuario_ se cerraron al redactar el plan. Las _Propuesta_ se revisaron con el usuario (tarea 1): todas se aceptaron tal cual salvo la 10, que el usuario cambió (exponer los límites en vez de dejar sin máximos los formularios).

| #   | Tema                                     | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Origen              |
| --- | ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- |
| 1   | Fotos                                    | Sin interfaz de fotos en v1: ni subida ni foto. Los endpoints de `squad` se quedan como están, pero `SquadApi` no los expone. Así no hacen falta el redimensionado a WebP en el navegador, el origen del bucket en la CSP ni SeaweedFS en el E2E de CI. Se corrige el texto del roadmap y el mapa de pantallas de `requirements.md` (§10). Cierra el riesgo _Fotos subidas sin mostrar_ del plan 12.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Usuario             |
| 2   | E2E                                      | Un spec nuevo, `e2e/admin-matchday.spec.ts`. El admin hace desde la interfaz todo lo que `matchday.spec.ts` prepara por API: crear la liga, parámetros, invitar, plantilla, partido, acta, cierre, cancelación de valoraciones y corrección. Lo del manager, que ya está probado, va por API (salvo unirse por el enlace). `matchday.spec.ts` no cambia, y entre los dos cubren la jornada completa de FE-34.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Usuario             |
| 3   | Parámetros de mercado                    | `marketK`, `marketMaxChangePct` y `marketMinValue` no salen en el formulario. Como el `PUT` reemplaza todo, se reenvían tal como llegaron. Se editarán en la Fase 3, con el mercado.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Usuario             |
| 4   | Atajos en el inicio                      | El admin ve una fila más en el inicio, debajo de lo del manager. _Acta de la jornada N · Pendiente_ si hay un partido que ya empezó sin cerrar, y enlaza a su acta. _Programar partido_ si no queda ninguno programado. Lo pendiente se calcula con la hora del navegador y es orientativo (FE-09): quien decide si se puede cerrar es el servidor.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Usuario             |
| 5   | Acta                                     | Arriba, el resultado con dos `bf-stepper`. Debajo, la plantilla activa agrupada por posición, cada jugador con una casilla _Jugó_. Al marcarla, la fila se abre con los contadores (goles, goles de penalti, asistencias, goles en propia, penaltis parados y fallados) y las tarjetas. Plegada, la fila resume lo que hizo con `performanceSummary` (`shared/performance`). Un jugador dado de baja que ya estaba en el acta sigue en ella, marcado _Dado de baja_. Si se quita, ya no se puede volver a añadir (lo impide el API).                                                                                                                                                                                                                                                                                                                                                    | Usuario             |
| 6   | Tabla de puntos                          | Una fila por acción con sus cuatro cifras (_Gol 6 · 5 · 4 · 3_), como en _Reglas_. Al tocarla, se abre con un `bf-stepper` por posición. Va en un `cdkAccordion`, así que solo hay una abierta a la vez.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Usuario             |
| 7   | Cancelar valoraciones                    | Una página propia, `admin/ratings/:matchId` (feature `rating`), con los miembros actuales. Cada uno tiene _Cancelar valoraciones_ (con confirmación, porque es definitivo), y los ya excluidos llevan _Cancelada_. Se llega desde la página del partido del admin y desde _Valorar_ (enlace solo para el admin).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Usuario             |
| 8   | Formularios                              | El jugador, el partido, el acta y los parámetros son páginas, no hojas. En iOS, el teclado se lleva mal con una hoja, el botón atrás funciona como se espera y `lazyUnsavedChangesGuard` vale tal cual. Las confirmaciones (baja, borrar, cerrar, cancelar) siguen siendo `Confirm`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Propuesta, aceptada |
| 9   | Tarjetas en el acta                      | Tres casillas: _Amarilla_, _Doble amarilla_ y _Roja directa_. Marcar _Doble amarilla_ desmarca y deshabilita las otras dos. No es un límite configurable, es la forma del contrato (`PlayerPerformanceRequest`: la doble amarilla excluye las otras), y el servidor la sigue validando (`INVALID_CARDS`). Amarilla y roja directa a la vez sí se pueden marcar.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Propuesta, aceptada |
| 10  | Topes en el cliente                      | Cada módulo publica sus límites configurados (`league.limits`, `squad.limits` y `match.limits`) en su propio endpoint (§3), y los formularios los leen del API para validar antes de enviar (FE-09): `maxlength` en los textos, `min`/`max` en el dorsal y la jornada, máximo en los steppers de goles y estadísticas, y el contador de jugadores del acta. El servidor sigue validando, y su `400` se pinta en el campo. Lo que no tiene límite configurado (cambios por jornada, tamaño de la alineación, tabla de puntos, que admite negativos) va sin máximo: `bf-stepper` pasa a admitir `min` y `max` nulos. Los mínimos fijos del dominio, que el spec publica como `minimum` (0 en los recuentos; 1 en el tamaño de la alineación, la duración de la ventana y los votos mínimos), no son configurables: son la forma del contrato y el cliente los usa como `min` del stepper. | Usuario             |
| 11  | Errores de los formularios               | Cada `code` de un campo se pinta en ese campo con una función pura y probada por formulario (`settingsErrorField`, `playerErrorField`, `matchErrorField`: un `Record` del subconjunto de `ErrorCode` al nombre del control). Los códigos sin campo van en un `bf-notice` sobre la acción principal. En el acta, los `400` (`INVALID_STAT_COUNT`, `INVALID_CARDS`, `DUPLICATE_PLAYER`…) no dicen de qué jugador son, así que van en ese aviso. `INVALID_GOALS` va en el resultado.                                                                                                                                                                                                                                                                                                                                                                                                       | Propuesta, aceptada |
| 12  | Programar un partido                     | El formulario propone la jornada siguiente (la mayor del calendario + 1) y, si hay partidos, la hora del último una semana después. Son datos, no reglas, y se pueden cambiar. La fecha y la hora van en dos campos nativos (`type="date"` y `type="time"`), en Madrid, con `madridToInstant` (y su inversa en `core/time` para editar, si falta).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Propuesta, aceptada |
| 13  | Cerrar el partido                        | En dos pasos. _Guardar acta_ guarda el borrador, que puede ir sin resultado. _Cerrar partido_ es la acción principal de la página del partido y pide confirmación: _Se bloquean las alineaciones que no lo estén, se publica el acta y se abre la valoración durante N h_ (N es `ratingWindowHours` de `GET /settings`; si ya hay ventana abierta, manda la congelada). El botón no se deshabilita por la hora: antes del inicio hay un aviso informativo, y `MATCH_NOT_STARTED` lo explica (FE-09).                                                                                                                                                                                                                                                                                                                                                                                    | Propuesta, aceptada |
| 14  | Correcciones                             | Con el partido cerrado, el acta se edita en la misma pantalla, con un aviso: _Partido cerrado: guardar corrige el acta y recalcula los puntos._ Si se quitan jugadores, _Guardar corrección_ pide confirmación y los nombra: _Pablo y Hugo salen del acta y pierden sus valoraciones._ Los avisos de coherencia (`GOALS_MISMATCH`, `OWN_GOALS_EXCEED_GOALS_AGAINST`) son `bf-notice` en ámbar, con sus textos en un `Record`, y no impiden guardar ni cerrar.                                                                                                                                                                                                                                                                                                                                                                                                                           | Propuesta, aceptada |
| 15  | Auditoría                                | Del cambio más reciente al más antiguo, una fila desplegable por cambio: acción (`Record<MatchAuditAction, string>`), quién (`memberName`, o _Antiguo miembro_) y cuándo. Al abrirla, lo que cambió en frases legibles, que salen de una función pura y probada (`auditChanges(before, after)`): jornada, hora, rival, campo, resultado, jugadores añadidos o quitados y estadísticas cambiadas. El alta y el borrado enseñan el estado entero.                                                                                                                                                                                                                                                                                                                                                                                                                                         | Propuesta, aceptada |
| 16  | Cancelación                              | La lista incluye al propio admin (_Tú_), porque el API le deja cancelarse a sí mismo. No dice quién ha votado: el API no lo da y FE-11 lo prohíbe. Con la ventana cerrada, la lista es de solo lectura y enseña solo a los cancelados. Con `MATCH_NOT_CLOSED`, es un estado propio de la vista: _La valoración se abre al cerrar el partido._                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Propuesta, aceptada |
| 17  | Bajas                                    | En la plantilla de admin, los dados de baja van en una sección _Dados de baja_, plegada y de solo lectura (no hay reactivar). _Dar de baja_ pide confirmación y explica que sigue en actas, puntos e historiales, que sale de las alineaciones y que sustituirlo no gasta cambio.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Propuesta, aceptada |
| 18  | Enlaces de admin en pantallas de miembro | Solo los ve el admin. _Reglas_ lleva _Cambiar parámetros_; la ficha del jugador, _Editar jugador_; el partido, _Administrar partido_; y _Valorar_, _Cancelar valoraciones de un manager_ (decisión 7). Cada uno es un enlace de ruta dentro de su feature, o a otra por ruta (FE-02).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Propuesta, aceptada |
| 19  | Fila de partido                          | La fila del calendario (jornada, rival, _En casa_/_Fuera_, fecha y resultado) aparece en el calendario de miembro y en el de admin, así que se extrae a `shared/match` como `bf-match-row`, con un hueco para el estado (FE-16). En el de admin, el estado es _Programado_, _Acta pendiente_ (ámbar) o _Cerrado_.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Propuesta, aceptada |

## 3. Trabajo de backend

### 3.1 Lo que ya existe

Lo que piden las pantallas ya está en el spec:

- `GET/PUT /settings`, con `scoringRules` y `ScoringAction` publicados (plan 12, §3.2).
- `POST/PUT/DELETE /players…`. Los de la foto quedan sin uso en la interfaz (decisión 1).
- `POST/PUT/DELETE /matches…`, `PUT …/sheet`, `POST …/close` y `GET …/audit`. `GET /matches/{matchId}` le da al admin el acta y sus avisos antes del cierre.
- `PUT /ratings/{matchId}/exclusions/{userId}`, y `excludedUserIds` en `GET /ratings/{matchId}` solo para el admin.
- El reloj de pruebas del perfil `e2e` (plan 12, §3.3).

Si una pantalla descubre que le falta algo, se añade en su tarea siguiendo _Cómo añadir o cambiar un endpoint_ de `frontend.md`, y se anota aquí.

### 3.2 Límites de cada módulo (decisión 10)

Cada módulo publica los suyos en su propio endpoint, leídos de su puerto `*LimitsRepository` (regla 2: nada de un adaptador que toque tres módulos). Son de solo lectura y globales (no cambian por liga), pero cuelgan de la liga para autorizar por rol y liga como cualquier otro endpoint (regla 8). Rol mínimo: administrador, porque solo los usan sus formularios.

| Endpoint                                | Módulo   | Respuesta                                                                                                 | De              |
| --------------------------------------- | -------- | --------------------------------------------------------------------------------------------------------- | --------------- |
| `GET /api/leagues/{id}/settings/limits` | `league` | `LeagueLimitsResponse`: `lineupLockHoursMax`                                                              | `league.limits` |
| `GET /api/leagues/{id}/players/limits`  | `squad`  | `SquadLimitsResponse`: `nameMaxLength`, `shirtNumberMin` y `shirtNumberMax`                               | `squad.limits`  |
| `GET /api/leagues/{id}/matches/limits`  | `match`  | `MatchLimitsResponse`: `opponentMaxLength`, `matchdayMin`, `goalsMax`, `statCountMax` y `sheetPlayersMax` | `match.limits`  |

- Quedan fuera `league.limits.name-max-length`, porque no hay pantalla que renombre la liga (crearla ya la valida el servidor), y `squad.limits.photo-max-size`, porque no hay interfaz de fotos (decisión 1). Si una pantalla los necesita, se añaden a su respuesta.
- Una consulta por módulo (`GetLeagueLimitsQuery`, `GetSquadLimitsQuery`, `GetMatchLimitsQuery`), documentada con springdoc (regla 5). Las descripciones de los campos de `LeagueSettingsBody`, `PlayerRequest`, `MatchRequest`, `MatchResultRequest` y `PlayerPerformanceRequest` que hoy citan `…limits.*` pasan a citar también el campo de su endpoint.
- `/players/limits` y `/matches/limits` conviven con `/players/{playerId}` y `/matches/{matchId}`: Spring prefiere el segmento literal, y un test lo comprueba.
- Tests: cada endpoint devuelve lo configurado (con un valor distinto del de `application.yml` en el test), un manager recibe `403` y un no miembro `403`. Se regeneran `api/openapi.json` y `schema.d.ts`.

## 4. Rutas y navegación

### 4.1 Pantallas

Rutas bajo `/leagues/:leagueId/admin`, con `adminGuard` (FE-03). Las del API cuelgan de `/api/leagues/{id}`.

| Pantalla                       | Ruta                           | Feature  | RF                  | Endpoints                                                                                                                        |
| ------------------------------ | ------------------------------ | -------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Parámetros y tabla de puntos   | `admin/settings`               | `league` | RF-07, RF-15        | `GET/PUT /settings`, `GET /settings/limits`                                                                                      |
| Plantilla (admin)              | `admin/squad`                  | `squad`  | RF-09, RF-10        | `GET /players`                                                                                                                   |
| Alta de jugador                | `admin/squad/new`              | `squad`  | RF-09, RF-10        | `POST /players`, `GET /players/limits`                                                                                           |
| Jugador (editar y dar de baja) | `admin/squad/:playerId`        | `squad`  | RF-09, RF-10        | `GET/PUT/DELETE /players/{playerId}`, `GET /players/limits`                                                                      |
| Calendario (admin)             | `admin/matches`                | `match`  | RF-12               | `GET /matches`                                                                                                                   |
| Programar partido              | `admin/matches/new`            | `match`  | RF-12               | `GET /matches`, `POST /matches`, `GET /matches/limits`                                                                           |
| Partido (admin)                | `admin/matches/:matchId`       | `match`  | RF-12 a RF-16       | `GET /matches/{matchId}`, `POST …/close`, `DELETE /matches/{matchId}`, `GET /ratings/{matchId}`, `GET /settings`, `GET /players` |
| Editar o aplazar partido       | `admin/matches/:matchId/edit`  | `match`  | RF-12               | `GET/PUT /matches/{matchId}`, `GET /matches/limits`                                                                              |
| Acta                           | `admin/matches/:matchId/sheet` | `match`  | RF-13, RF-14, RF-16 | `GET /matches/{matchId}`, `PUT …/sheet`, `GET /players`, `GET /matches/limits`                                                   |
| Auditoría                      | `admin/matches/:matchId/audit` | `match`  | RF-16, RNF-04       | `GET …/audit`, `GET /players`, `GET /members`                                                                                    |
| Cancelar valoraciones          | `admin/ratings/:matchId`       | `rating` | RF-41               | `GET /ratings/{matchId}`, `GET /members`, `PUT …/exclusions/{userId}`                                                            |
| Miembros e invitación          | `admin/members`                | `league` | RF-06               | Ya existe (plan 11)                                                                                                              |

Cada feature exporta sus rutas de admin en `<feature>.routes.ts` (`squadAdminRoutes`, `matchAdminRoutes`, `ratingAdminRoutes`, junto a `leagueAdminRoutes`), y `app.routes.ts` las une en `admin`. Cada una lleva su `title` con `$localize`. Las de formularios llevan `canDeactivate: [lazyUnsavedChangesGuard]`.

### 4.2 Navegación

- Los destinos no cambian: `admin` ya está entre los prefijos de _Liga_ (plan 12, §4.2).
- La sección _Administrar_ del índice _Liga_ pasa a tener _Parámetros_ (_Reglas, plazos y tabla de puntos_), _Jugadores_ (_Altas, cambios y bajas_), _Partidos_ (_Calendario, actas y cierres_) y _Miembros e invitación_.
- Más los enlaces contextuales de la decisión 18 y la fila del inicio de la decisión 4.

## 5. Contrato con el backend

- **Servicios** (FE-07). Los métodos se añaden a los servicios que ya existen, con sus alias (FE-06) y su test con `HttpTestingController`:
  - `SquadApi`: `register`, `update` y `retire`.
  - `MatchApi`: `schedule`, `update`, `delete`, `recordSheet`, `close` y `audit`. `audit` es un `httpResource`, porque depende del partido de la ruta; las escrituras devuelven `Observable`.
  - `RatingApi`: `exclude`.
  - `LeagueApi` ya tiene `settings` y `updateSettings`.
  - Límites (§3.2): `LeagueApi.limits`, `SquadApi.limits` y `MatchApi.limits`, como `httpResource` de la liga. Cada formulario pide solo los de su módulo; mientras cargan, el formulario está en _skeleton_, igual que con sus datos.
- **Modelos de vista** (FE-06), cada uno con su mapeo probado:
  - formulario de parámetros ↔ `LeagueSettingsBody`, conservando los de mercado (decisión 3) y la conversión del formato que no se edita;
  - borrador del acta ↔ `MatchSheetRequest` (los contadores a 0 y las tarjetas a `false` no hace falta enviarlos, pero se envían igual para que el diff con lo guardado sea simple);
  - formulario del partido ↔ `MatchRequest`, con fecha y hora de Madrid;
  - `auditChanges` (decisión 15) y `pendingMatch(matches, now)` (decisión 4).
- **Errores** (FE-08), tratados en su pantalla con el texto de `ERROR_TEXTS`:
  - parámetros: los `INVALID_*` de `PUT /settings`, en su campo, y `SCORING_TABLE_INCOMPLETE`, en el aviso (no debería pasar: el formulario siempre envía la tabla entera);
  - jugador: `INVALID_PLAYER_NAME`, `INVALID_SHIRT_NUMBER`, `SHIRT_NUMBER_TAKEN` e `INVALID_INITIAL_VALUE`, en su campo; `PLAYER_RETIRED`, recarga;
  - partido: `INVALID_MATCHDAY` y `MATCHDAY_TAKEN`, en la jornada; `INVALID_OPPONENT`, en el rival; `KICKOFF_TAKEN`, en la hora; `MATCH_CLOSED`, recarga;
  - acta: `INVALID_GOALS`, en el resultado; `INVALID_STAT_COUNT`, `INVALID_CARDS`, `SHEET_TOO_LARGE`, `DUPLICATE_PLAYER`, `UNKNOWN_PLAYER`, `INACTIVE_PLAYER` y `MATCH_SHEET_INCOMPLETE`, en el aviso (decisión 11);
  - cerrar: `MATCH_NOT_STARTED` y `MATCH_SHEET_INCOMPLETE`, con un toast y la vista recargada;
  - borrar: `MATCH_CLOSED`;
  - cancelar valoraciones: `RATING_WINDOW_CLOSED` y `MATCH_NOT_CLOSED`, con un toast y la vista recargada.
- **Anonimato** (FE-11): la pantalla de cancelación no usa nada que diga quién ha votado; solo `excludedUserIds`.

## 6. Diseño

### 6.1 Boceto (FE-28)

Primera tarea de UI, con la skill `.agents/skills/frontend-design` y sobre la dirección de `ui/README.md`, que no se reabre. El boceto se añade a `docs/design/admin-screens.html`, en los dos temas y en 360 px y `lg`, con estas pantallas: parámetros (con una fila de la tabla abierta), plantilla de admin, formulario de jugador, calendario de admin, partido (programado, pendiente y cerrado), acta (borrador y corrección), auditoría y cancelar valoraciones. Se revisa con el usuario antes de construir, y la composición final se documenta en _Composición_ de `ui/README.md`.

Lo que se le pide al boceto:

- **El acta es la pantalla del domingo.** Tiene que rellenarse con una mano, de pie, en un minuto: un toque por jugador que jugó y casi ninguno más. Los contadores a 0 no se ven plegados.
- **Lo que no se deshace se nota.** Baja, borrar, cerrar y cancelar valoraciones son `danger` o llevan confirmación que dice lo que pasa, con el mismo verbo que el botón (_Cerrar partido_ → _Partido cerrado_).
- **Admin sin otro aspecto.** Mismas filas, mismas cifras y mismo peto. Lo de admin se reconoce por el título y por `bf-badge` `accent` donde haga falta, no por otro tema.
- **Textos** según _Textos_ de `ui/README.md`. Cada botón dice lo que hace (_Guardar parámetros_, _Dar de alta jugador_, _Programar partido_, _Guardar acta_, _Guardar corrección_, _Cerrar partido_, _Cancelar valoraciones_) y su aviso usa el mismo verbo.

### 6.2 Composición propuesta

Puntos de partida para el boceto. Los cierra la tarea 2.

**Parámetros** (`admin/settings`):

```
Parámetros
ⓘ Los cambios valen para lo que venga: las ventanas abiertas
  y las jornadas ya puntuadas conservan los suyos.
── Alineaciones ─────────────
Jugadores por alineación      [−] 7 [+]
Se bloquea antes del partido  [−] 2 [+] h
Cambios por jornada           [−] 2 [+]
Presupuesto                   [● ] 100 M        bfSwitch + importe
Formación por posiciones      [  ○]             al activarla: 4 mínimos
── Valoraciones ─────────────
Formato                 (Picas | Nota)
Duración                      [−] 24 [+] h
Votos mínimos                 [−] 1 [+]
Nota sin votos                [  ○]             al activarla: bf-rating-input
Conversión     puntos = [ 4 ] × media + [ −2 ]  la del formato elegido
── Puntos por acción ────────
Gol             6 · 5 · 4 · 3   ▾               decisión 6
   Portero   [−] 6 [+]  …
Gol de penalti  3 · 3 · 3 · 3   ▸
…
[      Guardar parámetros      ]
```

**Plantilla de admin** (`admin/squad`): la plantilla agrupada por posición, como la de miembro, con `bf-player-row`. Cada fila lleva a `admin/squad/:playerId`. Al pie, _Dados de baja_ plegada (decisión 17), y como acción principal _Dar de alta jugador_.

**Jugador** (`admin/squad/new`, `admin/squad/:playerId`): nombre, dorsal (`inputmode="numeric"`, opcional), posición (`bf-segmented` con las cuatro) y valor inicial (`inputmode="decimal"`, en M). Acción principal: _Dar de alta jugador_ o _Guardar jugador_. En la edición, _Dar de baja_ (`danger`) al final. Un dado de baja se ve en solo lectura, con su `bf-badge`.

**Calendario de admin** (`admin/matches`): `bf-match-row` (decisión 19) con el estado a la derecha, y como acción principal _Programar partido_.

**Partido de admin** (`admin/matches/:matchId`):

```
Jornada 12                 [Acta pendiente]
Contra CD Arrabal · En casa
Domingo 2 de noviembre, 11:00            Editar ›
──────────────────────────────
Acta              3–1 · 9 jugadores    ›     al acta
⚠ Los goles de los jugadores no suman 3.     avisos de coherencia
──────────────────────────────
Valoraciones      Abierta · cierra en 20 h  › cancelar valoraciones (cerrado)
Auditoría         4 cambios                ›
──────────────────────────────
[ Borrar partido ]                             danger quiet, solo sin cerrar
[        Cerrar partido        ]               acción principal, solo sin cerrar
```

**Acta** (`admin/matches/:matchId/sheet`): la de la decisión 5. Acción principal _Guardar acta_ (o _Guardar corrección_ con el partido cerrado, decisión 14). En `lg`, el resultado y los avisos a la derecha.

**Auditoría** y **Cancelar valoraciones**: listas de filas (`bfDisclosure` en la auditoría y `bfListItem` con botón en la cancelación), con su estado vacío.

### 6.3 Componentes de `ui/`

Cada uno con su test y en `/_ui` con todos sus estados en los dos temas:

| Componente                         | Uso                                                                                                                                                           |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `input[type=checkbox][bfCheckbox]` | Casilla nativa con la etiqueta como objetivo de 44 px; marcada en lavanda. _Jugó_ y las tarjetas del acta.                                                    |
| `button[bfSwitch]`                 | Interruptor (`role="switch"`, `aria-checked`) para activar y desactivar presupuesto, formación y nota sin votos; `[(checked)]`.                               |
| `bf-stepper`                       | `min` y `max` pasan a ser opcionales (`null`, sin límite) para lo que no tiene límite configurado (decisión 10); con valores negativos, el menos tipográfico. |

### 6.4 Componentes de `shared/`

| Componente        | Uso                                                                                                                                                                                                                                 |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `bf-match-row`    | La fila del calendario extraída de `features/match` (decisión 19): jornada, rival, campo, fecha y resultado, con un hueco `bfMatchRowTrailing` para el estado o el resultado.                                                       |
| Textos de `match` | `MATCH_WARNING_TEXTS` (`Record` del enum de avisos) en `shared/match`. Los usa la página del partido de admin y el acta, que son de la misma feature, pero el enum es del contrato y así lo encuentra el siguiente que lo necesite. |

## 7. Pantallas

Cada una con carga (_skeleton_ con la forma del contenido), vacío, error con _Reintentar_ y contenido (FE-14). Textos con `i18n` e id `@@<feature>.admin.<pantalla>.<texto>`. Envíos deshabilitados sin conexión (FE-31). La acción principal, abajo (FE-30). Formularios con `NonNullableFormBuilder` (FE-15), cambios sin guardar con `lazyUnsavedChangesGuard` y `warnBeforeUnload`, y toast con el verbo del botón al guardar.

- **Parámetros:**
  - §6.2 y decisiones 3, 6, 10 y 11.
  - Al cambiar el formato, la nota sin votos se borra (su escala es otra) y la conversión enseña la del nuevo formato.
  - Los mínimos de la formación solo se ven y se envían activos con la formación activada; desactivada, se reenvían los que había.
  - El presupuesto se reenvía aunque esté desactivado, porque el API lo pide mayor que 0.
  - El bloqueo lleva de máximo `lineupLockHoursMax` (`GET /settings/limits`).
- **Plantilla de admin, alta y jugador:** §6.2 y decisión 17. El nombre lleva `maxlength` (`nameMaxLength`) y el dorsal, `min` y `max` (`shirtNumberMin`, `shirtNumberMax`), de `GET /players/limits`. Tras dar de alta, vuelve a la plantilla con _Jugador dado de alta_. Tras dar de baja, también, con _Jugador dado de baja_.
- **Calendario de admin:** §6.2. Sin partidos: _Aún no hay partidos. Programa el primero._, con la acción.
- **Programar, editar o aplazar:**
  - Decisión 12. La jornada lleva de mínimo `matchdayMin`, y el rival, `maxlength` (`opponentMaxLength`), de `GET /matches/limits`.
  - Cambiar la jornada o la hora es un aplazamiento: si el partido tenía las alineaciones bloqueadas, un aviso informativo dice que, si el nuevo bloqueo aún no ha llegado, se desbloquean.
  - Con el partido cerrado, la jornada, la fecha y la hora van deshabilitadas, con la razón en el `hint`. Es estado del partido, no un límite.
- **Partido de admin:**
  - §6.2 y decisión 13.
  - La fila _Valoraciones_ sale de `GET /ratings/{matchId}` con `handling(409)`. Con el partido sin cerrar, no aparece.
  - _Borrar partido_ pide confirmación (_Se borra el partido de la jornada 12. Las alineaciones guardadas pasan al siguiente._) y vuelve al calendario.
  - Antes del inicio, encima de _Cerrar partido_: _Se puede cerrar desde el inicio del partido._
- **Acta:**
  - Decisiones 5, 9, 11 y 14.
  - Si el partido no tiene resultado, los dos `bf-stepper` empiezan en vacío (`empty`). El resultado se envía cuando los dos tienen valor. Con uno solo, _Guardar acta_ marca el otro: _Falta este marcador_. Es la forma del contrato (`MatchResultRequest` pide los dos).
  - Los steppers del resultado y de las estadísticas llevan de máximo `goalsMax` y `statCountMax`. Sobre la lista, _9 de 30 jugadores_ (`sheetPlayersMax`); al llegar al máximo, las casillas _Jugó_ sin marcar se deshabilitan con la razón en el `hint`.
  - Tras guardar, se pintan los `warnings` de la respuesta y la página vuelve al partido con _Acta guardada_.
- **Auditoría:** decisión 15. Sin cambios no puede estar vacía (el alta siempre está), pero tiene su estado por si acaso.
- **Cancelar valoraciones:** decisiones 7 y 16. La confirmación nombra al miembro: _Se borran las valoraciones de Iago en la jornada 12 y no podrá volver a valorarla. No se puede deshacer._ Después, la fila pasa a _Cancelada_.
- **Inicio** (decisión 4): la fila de admin usa `GET /matches` solo si el rol es `ADMIN`. Con `pendingMatch`, _Acta de la jornada N · Pendiente_ enlaza a `admin/matches/:matchId/sheet`. Si no hay ninguno programado, _Programar partido_ enlaza a `admin/matches/new`. Si no hay nada pendiente, la fila no aparece.
- **Índice _Liga_, _Reglas_, ficha, partido y _Valorar_:** los enlaces de §4.2 y la decisión 18.

## 8. Cambios sin guardar, conflictos y plazos

- **Cambios sin guardar** en parámetros, jugador, partido y acta: `HasUnsavedChanges` y `lazyUnsavedChangesGuard`, como la alineación y valorar (plan 12, §8). No avisa tras guardar ni si el borrador es igual a lo guardado.
- **Dos admins no hay**, pero sí dos pestañas: el `PUT` reemplaza, así que gana el último. No hay control de versión en el API y no se añade: para una liga de amigos con un admin no compensa.
- **`409` por estado** (`MATCH_CLOSED`, `PLAYER_RETIRED`, `RATING_WINDOW_CLOSED`): toast con el texto del código y recarga de la vista. Si el formulario tenía cambios, se descartan, porque lo que se veía ya no vale.
- **Plazos:** la página del partido de admin y la de cancelar valoraciones llaman a `onVisible`. La cuenta atrás de la ventana, al llegar a cero, recarga.

## 9. Tests y CI

- **Backend:** los de §3.2 (lo configurado, `403` para un manager y un no miembro, y la ruta literal frente a la del id). Se regeneran `api/openapi.json` y `schema.d.ts`.
- **Unitarios y de componentes** (FE-33):
  - `bfCheckbox`, `bfSwitch` y `bf-stepper` sin límites (estados, teclado y accesibilidad básica), y `bf-match-row`;
  - los métodos nuevos de `LeagueApi`, `SquadApi`, `MatchApi` y `RatingApi`;
  - las funciones puras: los mapeos de §5, los `*ErrorField`, `auditChanges`, `pendingMatch`, la propuesta de jornada y hora, la exclusión de tarjetas y los jugadores quitados en una corrección;
  - cada página con sus estados, sus errores en el campo y sus `409`, con los servicios simulados;
  - las rutas de admin con `adminGuard`.
- **E2E del admin** (FE-34), `e2e/admin-matchday.spec.ts`, en móvil (Chromium y WebKit) y en los dos temas, con `@axe-core/playwright` en cada pantalla de admin:
  1. El admin crea la liga desde la interfaz y, en _Parámetros_, pone una alineación de 3 sin formación y cambia un valor de la tabla de puntos (_Gol_ de delantero).
  2. Copia el enlace en _Miembros e invitación_, y el manager se une con él en su propio contexto del navegador.
  3. Da de alta la plantilla. Un dorsal repetido enseña `SHIRT_NUMBER_TAKEN` en su campo. Después edita un jugador y da de baja otro.
  4. Programa el partido a unas horas de la hora del servidor (`GET /api/e2e/clock`).
  5. Los dos guardan su alineación por API (la interfaz ya la prueba `matchday.spec.ts`).
  6. Se adelanta el reloj más allá del inicio. El inicio del admin enseña _Acta de la jornada 1 · Pendiente_. Rellena el acta (resultado, quién jugó, un gol del delantero y una amarilla) y ve el aviso de coherencia si no cuadra. La guarda y cierra el partido.
  7. El manager valora por API. El admin cancela sus valoraciones, y el manager ve _Excluido_ en _Valorar_.
  8. El admin corrige el acta (añade una asistencia), y la auditoría enseña la corrección.
  9. Se adelanta el reloj más allá del cierre de la ventana. La clasificación y los puntos de la jornada reflejan la tabla editada en el paso 1 y la corrección.
- **Reloj compartido:** `admin-matchday.spec.ts` mueve el reloj del servidor, así que entra en `CLOCK_SPECS` de `playwright.config.ts`. La cadena de proyectos se cambia para que los dos specs que mueven el reloj nunca corran a la vez: un proyecto por spec y navegador, todos encadenados.
- **CI:** sin cambios en el job `e2e`. Sin fotos no hace falta SeaweedFS (decisión 1).

## 10. Cambios en la documentación

En el cambio de la tarea que los provoca:

- `requirements.md`:
  - **Roadmap**: el ítem _Pantallas de admin_ deja de decir _jugadores y foto_ y dice _jugadores (la foto queda en el API, sin interfaz en v1)_. Al cerrar, se marcan _Pantallas de admin_ y _E2E de la jornada completa en móvil_.
  - **Mapa de pantallas**: _Admin · parámetros_ con `GET /settings/limits`; _Admin · jugadores_ sin los endpoints de foto y con `GET /players/limits`; _Admin · partidos_ con `GET /matches/limits`; _Admin · partidos, acta, cierre y auditoría_ con `GET /players` y `GET /members`; _Inicio de la liga_ con la fila de admin; y la fila de _Admin · cancelar valoraciones_ con `GET /ratings/{matchId}` y `GET /members`.
- `ui/README.md`: _Composición_ de las pantallas de admin, y `bfCheckbox`, `bfSwitch`, `bf-stepper` sin límites y `bf-match-row` en _Componentes_.
- `frontend.md`: rutas de admin por feature (`<feature>AdminRoutes`), enlaces de admin en pantallas de miembro, los `*ErrorField` y la regla de topes de la decisión 10 (límites de `GET …/limits`, mínimos fijos del contrato y nada más).
- `conventions.md`: un límite nuevo de `<módulo>.limits` que limite un formulario se publica también en el `GET …/limits` de su módulo.
- `testing.md`: el E2E del admin y la cadena de specs que mueven el reloj.
- `status.md` y _Estado_ de `AGENTS.md`.

## 11. Orden de ejecución

| #   | Tarea                                                                                                                                  | Resultado                                 | Hecho |
| --- | -------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- | ----- |
| 1   | Revisar este plan con el usuario y cerrar las propuestas (§2)                                                                          | Decisiones cerradas                       | ✅    |
| 2   | Endpoints de límites de `league`, `squad` y `match` con sus tests; spec y tipos regenerados (§3.2)                                     | Límites en el API (FE-09)                 | ✅    |
| 3   | Boceto de las pantallas de admin con la skill, revisado con el usuario; _Composición_ en `ui/README.md` (§6.1, §6.2)                   | Composición cerrada (FE-28)               | ✅    |
| 4   | Métodos de admin y de límites en `LeagueApi`, `SquadApi`, `MatchApi` y `RatingApi`, con sus tests (§5)                                 | Contrato de las pantallas (FE-05 a FE-08) | ✅    |
| 5   | `bfCheckbox`, `bfSwitch` y `bf-stepper` sin límites en `ui/` y `/_ui`; `bf-match-row` y los textos de avisos en `shared/` (§6.3, §6.4) | Componentes de admin (FE-16 a FE-20)      | ✅    |
| 6   | Rutas de admin por feature, sección _Administrar_ del índice y enlaces de admin en las pantallas de miembro (§4, decisión 18)          | Accesos de admin                          | ✅    |
| 7   | Parámetros y tabla de puntos                                                                                                           | RF-07, RF-15                              | ✅    |
| 8   | Plantilla de admin, alta, edición y baja                                                                                               | RF-09, RF-10                              | ✅    |
| 9   | Calendario de admin, programar, editar, aplazar, borrar y página del partido                                                           | RF-12                                     | ✅    |
| 10  | Acta, cierre y correcciones                                                                                                            | RF-13 a RF-16                             |       |
| 11  | Auditoría de un partido                                                                                                                | RF-16, RNF-04                             |       |
| 12  | Cancelar valoraciones                                                                                                                  | RF-41                                     |       |
| 13  | Fila de admin en el inicio de la jornada (decisión 4)                                                                                  | Atajos del admin                          |       |
| 14  | E2E del admin con el reloj de pruebas y axe, y la cadena de specs del reloj (§9)                                                       | Jornada completa automatizada (FE-34)     |       |
| 15  | Documentación de §10, `status.md`, _Estado_ de `AGENTS.md` y marcar los dos ítems de la Fase 2 en `requirements.md`                    | Ítems de la Fase 2 cerrados               |       |

Cada tarea es un commit en Conventional Commits citando sus códigos: `feat(league,squad,match): …` para los límites, `feat(frontend): … (RF-xx, FE-xx)`, `test(frontend): …`, `docs: …`. El acta va después del partido porque se llega a ella desde él. El inicio va al final porque enlaza a pantallas de las demás tareas.

## 12. Riesgos y notas

- **El acta con mucha plantilla:** con 20 jugadores, la lista es larga. Por eso los no marcados son una sola línea y solo el abierto enseña los contadores. Se comprueba en el boceto con 20 jugadores y 11 marcados.
- **Teclado y `bf-stepper`:** los contadores se tocan con − y +, pero también se escriben (`role="spinbutton"`). Se comprueba en iOS que el teclado numérico no tapa la acción principal.
- **Fecha y hora en Madrid:** `type="date"` y `type="time"` dan la hora local del campo, no la del navegador. Se convierte siempre con `madridToInstant`, y se prueba con un cambio de hora (último domingo de octubre).
- **`PUT /settings` reemplaza todo:** si un parámetro nuevo llega al spec y el formulario no lo conoce, el mapeo no compila (el tipo del body lo exige). Es lo que se quiere: nadie borra un parámetro sin darse cuenta.
- **Fila pendiente con la hora del navegador:** si el reloj del móvil va mal, la fila puede salir antes o después. Es orientativa, y cerrar lo decide el servidor.
- **Prueba real:** con este plan, una jornada se lleva entera desde la interfaz. La prueba con los amigos necesita además el Dockerfile, el Compose de producción, el VPS y los backups de la Fase 0, que conviene planear justo después (plan 14).
