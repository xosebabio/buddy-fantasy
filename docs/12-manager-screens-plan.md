# 12 · Pantallas de manager — plan

Plan del ítem de la **Fase 2** *Pantallas de manager: ligas e invitación, inicio, alineación, valoraciones, clasificaciones, historiales y plantilla (RF-01 a RF-11, RF-17 a RF-33)*. Las de ligas e invitación (RF-01 a RF-08) ya están hechas en el plan 11; aquí se construye todo lo que hace falta para jugar una jornada como manager (y como admin cuando juega de manager): el inicio, la alineación, las valoraciones, las clasificaciones, los historiales, la plantilla con la ficha del jugador y el calendario. También va el poco backend que piden (§3), con el reloj de pruebas para que el E2E recorra la jornada de punta a punta. Las reglas son las de `docs/requirements.md` (RNF-01, RNF-12 a RNF-16, FE-01 a FE-35), resumidas en `docs/agents/frontend.md`, y la dirección visual de `frontend/src/app/ui/README.md`; este plan no las repite, dice cómo y en qué orden se aplican. Las pantallas de admin (parámetros, jugadores y foto, partidos y acta, cancelación de valoraciones) y el E2E de la jornada completa desde la interfaz del admin van en el plan 13.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Backend | `includeRetired` para cualquier miembro, `ScoringAction` publicado en el spec y reloj de pruebas con el perfil `e2e` (§3). |
| Contrato | `SquadApi`, `MatchApi`, `LineupApi`, `RatingApi` y `ScoringApi` con los endpoints de lectura y las escrituras del manager; los alias de sus DTO y los cruces de ids con jugadores y miembros (§5). |
| Diseño | Boceto de las pantallas nuevas con la skill `frontend-design`, revisado antes de construir; composición documentada en `ui/README.md` (§6). |
| Componentes | Los de `ui/` que faltan de FE-18 y los que piden estas pantallas; el catálogo de `shared/` de FE-19 (§6.3, §6.4). |
| Pantallas | Inicio de la jornada, alineación y alineaciones bloqueadas, valoraciones (listado y partido), clasificación general y por jornada, historial de un manager, plantilla y ficha, calendario y partido, y el índice *Liga* (§7). |
| Comportamiento | Cuentas atrás, refresco al volver a la pestaña, cambio de jornada mientras se edita y cambios sin guardar (§8). |
| Calidad | Tests de componentes, servicios, mapeos y páginas, y E2E del manager en móvil con axe y el reloj de pruebas (§9). |
| Fuera de alcance | Pantallas de admin y su E2E (plan 13), cancelar valoraciones (RF-41, plan 13), gráfica del valor (RF-36, Fase 3), fotos de los jugadores en la interfaz (decisión 5), notificaciones (Fase 3), Dockerfile, Compose de producción y VPS (Fase 0). |

## 2. Decisiones

Las marcadas como *Usuario* se cerraron al redactar el plan. Las *Propuesta* se revisaron con el usuario (tarea 1) y se aceptaron tal cual.

| # | Tema | Decisión | Origen |
| --- | --- | --- | --- |
| 1 | Jugadores dados de baja | `GET /players?includeRetired=true` vale para cualquier miembro, no solo para el admin. Cada pantalla pide la plantilla completa una vez y cruza con ella los ids de alineaciones, actas, puntos e historiales; las listas de la plantilla y los candidatos de la alineación filtran las bajas en el cliente. | Usuario |
| 2 | Antiguos miembros | Un `userId` que no está en `GET /members` (dejó la liga o lo expulsaron) se muestra como *Antiguo miembro*, sin avatar. Sin cambios en el backend. | Usuario |
| 3 | E2E | Este plan añade el reloj de pruebas (§3.3), y el E2E del manager recorre alineación, bloqueo, valoración, cierre, clasificación e historial con los datos de admin preparados por API. El plan 13 lo reutiliza para la jornada completa desde la interfaz. | Usuario |
| 4 | Navegación | Los cinco destinos de FE-30: *Jornada*, *Alineación*, *Valorar*, *Clasificación* y *Liga*. *Liga* pasa a ser un índice con *Plantilla*, *Calendario*, *Reglas* (decisión 12), *Miembros* y, para el admin, sus pantallas. La ficha del jugador se abre también desde la alineación, las valoraciones, los puntos de una jornada y los historiales. | Usuario |
| 5 | Fotos | Nunca en v1: solo petos. FE-19 se corrige en el mismo cambio, porque la tarjeta de jugador pasa a ser sin foto. El admin podrá seguir subiéndola (plan 13), pero la interfaz de manager no la pinta. | Usuario |
| 6 | Nota decimal | Stepper − / + con la cifra grande en medio y botones de 44 px. Empieza en *Sin voto* y el primer toque pone 5; *Quitar voto* vuelve a vacío. En picas, cinco opciones de 0 a 4 en una fila (radios nativos), con el mismo *Quitar voto*. Un 0 es un voto (en picas resta 2), así que nunca se confunde con no votar. | Usuario |
| 7 | Editar la alineación | Tocar un peto o una plaza libre abre una hoja con los candidatos: primero los de su posición y después el resto, cada uno con su valor y si cabe en lo que queda de presupuesto. Al elegir uno se hace el cambio, y la hoja tiene también *Quitar de la alineación*. La vista en lista funciona igual. | Usuario |
| 8 | Ficha del jugador | Lista de jornadas con el total. Al abrir una, sale el desglose de acciones y valoración. Sin gráficas hasta la Fase 3 (RF-36). | Usuario |
| 9 | Tipos de las acciones | `actions` y `scoringRules` son mapas con clave de texto en el spec (OpenAPI 3.0 no tipa claves). El backend publica `ScoringAction` como esquema y el front comprueba las claves con un *type guard* antes de buscar su texto en `Record<ScoringAction, string>`, así que una acción nueva rompe la compilación (FE-23). | Propuesta, aceptada |
| 10 | Cambios de la alineación | El cliente no calcula cuántos cambios gasta un borrador: muestra los `changesUsed` / `maxChanges` del servidor tras cada guardado, y `MAX_CHANGES_EXCEEDED` lo explica. Contar cambios exigiría repetir la regla de `lineup` (contra la última bloqueada, y las bajas son gratis), y FE-09 lo prohíbe. Tamaño, formación y presupuesto sí se reflejan antes de guardar, con los parámetros que da `GET /settings`. | Propuesta, aceptada |
| 11 | Plazo a punto de vencer | `bf-countdown` se pone en ámbar con menos de 3 h. Es presentación, no regla de negocio: es una constante de `ui/` y no la usa ninguna validación. | Propuesta, aceptada |
| 12 | Reglas de la liga | Pantalla de solo lectura en el índice *Liga*: tamaño, bloqueo, cambios, presupuesto, formación, formato y ventana de valoración, y la tabla de puntos (`GET /settings`, que ya es de miembro). Sirve en la prueba real para que todos sepan cómo se puntúa, y su tabla de puntos es la que el plan 13 hará editable. | Propuesta, aceptada |
| 13 | Importes | `bfMoney` en millones, con `es-ES`, entre 1 y 2 decimales y la unidad aparte, más pequeña (`87,4` + `/ 100 M`). `bfPoints` con signo y el menos tipográfico (`+14`, `−3`, `0`). | Propuesta, aceptada |
| 14 | Inicio | El inicio sale de `GET /lineup` (partido objetivo y bloqueo), `GET /ratings` (ventanas abiertas) y `GET /standings` (mi posición y mi última jornada). La acción principal depende del contexto: *Valorar la jornada N* si hay una ventana abierta con jugadores sin valorar, y si no, *Editar alineación*. | Propuesta, aceptada |
| 15 | Refresco | Sin sondeo. Las páginas con plazos (inicio, alineación y valoración de un partido) recargan sus recursos al volver a la pestaña (`visibilitychange`) y cuando su cuenta atrás llega a cero. | Propuesta, aceptada |

## 3. Trabajo de backend

### 3.1 Plantilla completa para los miembros (decisión 1)

- `ListPlayersQuery` deja de ignorar `includeRetired` para los managers: el filtro vale para cualquier miembro y por defecto sigue en `false`. Los dados de baja no son secretos: ya salen en las actas, los puntos y `GET /players/{id}`.
- Se actualizan la descripción del parámetro en el spec y el test del endpoint (un manager con `includeRetired=true` recibe las bajas).

### 3.2 `ScoringAction` en el spec (decisión 9)

- `OpenApiConfig` publica el esquema `ScoringAction` (enum con las 9 acciones), igual que hace con `ErrorCode`, y las descripciones de `actions` (`LinedUpPlayerResponse`, `PlayerMatchdayResponse`) y de `scoringRules` lo citan.
- `OpenApiDocumentationTests` comprueba que el enum del spec coincide con `datadefinition.ScoringAction`.

### 3.3 Reloj de pruebas (decisión 3)

- Con el perfil `e2e`, `ClockConfig` da un `Clock` ajustable (`E2eClock`, en `shared/infrastructure`) en vez de `Clock.systemUTC()`. Empieza en la hora real y solo avanza.
- `E2eClockController` (`@Profile("e2e")`, `@Hidden`): `GET /api/e2e/clock` devuelve la hora del servidor y `POST /api/e2e/clock` con `{ "advance": "PT26H" }` la adelanta. Pasa por CSRF, como el login de pruebas.
- Los jobs no se llaman desde fuera, porque eso tocaría las clases internas de `lineup` y `rating` (Modulith). El perfil `e2e` acorta su `cron` (`lineup.lock-job.cron` y `rating.close-job.cron`, cada 2 s), y los jobs encuentran solos lo que ha vencido con el reloj adelantado.
- Tests: con el perfil, adelantar el reloj hace que una alineación se bloquee y que una ventana se cierre sin otra intervención. Sin el perfil, el bean no existe y la ruta no responde (`401`/`404`). Al arrancar con el perfil se registra un `WARN`, como con el login de pruebas.

## 4. Rutas y navegación

### 4.1 Pantallas

Rutas bajo `/leagues/:leagueId`. Las del API cuelgan de `/api/leagues/{id}`.

| Pantalla | Ruta | Feature | RF | Endpoints |
| --- | --- | --- | --- | --- |
| Inicio de la jornada | `''` | `league` | RF-17, RF-25, RF-32 | `GET /lineup`, `GET /ratings`, `GET /standings` |
| Alineación | `lineup` | `lineup` | RF-24–29 | `GET/PUT /lineup`, `GET /players`, `GET /settings` |
| Alineaciones bloqueadas | `lineups/:matchId` | `lineup` | RF-27 | `GET /lineups/{matchId}`, `GET /players`, `GET /members` |
| Valoraciones | `ratings` | `rating` | RF-17 | `GET /ratings` |
| Valorar un partido | `ratings/:matchId` | `rating` | RF-18–23 | `GET/PUT /ratings/{matchId}`, `GET /matches/{matchId}`, `GET /players` |
| Clasificación general | `standings` | `scoring` | RF-32 | `GET /standings`, `GET /members`, `GET /matches` |
| Puntos de una jornada | `standings/:matchId` | `scoring` | RF-30–32 | `GET /standings/{matchId}`, `GET /players`, `GET /members` |
| Historial de un manager | `managers/:userId` | `scoring` | RF-33 | `GET /managers/{userId}/history`, `GET /players`, `GET /members` |
| Liga (índice) | `league` | `league` | RF-08 | `GET /api/leagues/{id}` |
| Plantilla | `squad` | `squad` | RF-11 | `GET /players` |
| Ficha del jugador | `squad/:playerId` | `squad` | RF-11 | `GET /players/{playerId}`, `GET /players/{playerId}/points` |
| Calendario | `matches` | `match` | RF-12 | `GET /matches` |
| Partido y acta | `matches/:matchId` | `match` | RF-13, RF-14 | `GET /matches/{matchId}`, `GET /players` |
| Reglas de la liga | `rules` | `league` | RF-07, RF-15 | `GET /settings` |

Cada pantalla tiene su `title` con `$localize`. Las de una feature van en su `<feature>.routes.ts`, y `app.routes.ts` las coloca como hijas de `leagues/:leagueId`, como las de `league`.

### 4.2 Navegación

- `DESTINATIONS` del shell pasa a los cinco destinos: *Jornada* (`LucideHouse`), *Alineación* (`LucideShirt`), *Valorar* (`LucideStar`), *Clasificación* (`LucideTrophy`) y *Liga* (`LucideUsers`), como en el boceto.
- Cada destino declara los prefijos de ruta que cubre, y el shell decide cuál está activo con ellos: *Alineación* cubre `lineup` y `lineups`; *Valorar*, `ratings`; *Clasificación*, `standings` y `managers`; *Liga*, `league`, `squad`, `matches`, `rules`, `members` y `admin`. Hoy `routerLinkActive` solo mira la ruta exacta.
- El índice *Liga* reemplaza a *Miembros* como destino; `members` sigue siendo su propia página.

## 5. Contrato con el backend

- **Servicios** (FE-07), cada uno en `core/api/<módulo>/` con los alias de sus DTO (FE-06) y su test con `HttpTestingController`:
  - `SquadApi`: plantilla (con `includeRetired`) y jugador.
  - `MatchApi`: calendario y partido.
  - `LineupApi`: mi alineación, guardarla y las bloqueadas de un partido.
  - `RatingApi`: ventanas, valoraciones de un partido y guardarlas.
  - `ScoringApi`: clasificación, puntos de una jornada, historial de un manager y puntos de un jugador.
  - Los métodos de admin (alta de jugadores, partidos, acta, cierre, auditoría, exclusiones) llegan con el plan 13. Las lecturas que dependen de la liga o del partido de la ruta devuelven `httpResource`, y las escrituras, `Observable`, como en `LeagueApi`.
- **Cruces de ids:** las respuestas de `lineup`, `rating`, `scoring` y `match` traen ids de jugadores y de usuarios. En `shared/roster/` hay funciones puras y probadas: `playerIndex(players)` y `memberIndex(members)` devuelven un `Map` por id, y `memberName(index, userId)` devuelve el nombre o *Antiguo miembro*. Las páginas las usan en sus `computed` para montar los modelos de vista (FE-06), derivados con `Pick` y `&`, cada uno con su mapeo probado.
- **Formato congelado:** la página de valorar usa el `format` de `MatchRatingsResponse` (congelado al abrir la ventana), nunca el `ratingFormat` actual de la liga.
- **Errores** (FE-08): los `409` que puede provocar un manager se tratan en su pantalla con su texto de `ERROR_TEXTS` y recargando la vista:
  - alineación: `SIZE`, `FORMATION`, `OVER_BUDGET`, `MAX_CHANGES_EXCEEDED`, `INACTIVE_PLAYER`, `UNKNOWN_PLAYER` y `DUPLICATE_PLAYER`;
  - valoraciones: `RATING_WINDOW_CLOSED`, `RATER_EXCLUDED`, `INVALID_RATING` y `PLAYER_NOT_IN_SHEET`;
  - lecturas que todavía no tienen datos: `LINEUPS_NOT_LOCKED`, `MATCH_NOT_CLOSED` y `MATCH_NOT_SCORED`. Cada una se pide con `handling(409)` y se pinta como un estado propio de la vista, no como un error.

## 6. Diseño

### 6.1 Boceto (FE-28)

Primera tarea de UI, con la skill `.agents/skills/frontend-design` y sobre la dirección de `ui/README.md`, que no se reabre: el peto sigue siendo lo memorable, las listas son filas, las cifras mandan y la acción va abajo. El boceto se añade a `docs/design/` (`manager-screens.html`, en los dos temas y en 360 px y `lg`): inicio, alineación (campo, hoja de candidatos y lista), valorar en picas y en nota, clasificación general, puntos de una jornada y ficha del jugador. Se revisa con el usuario antes de construir, y la composición final se documenta en la sección *Composición* de `ui/README.md`.

Lo que se le pide al boceto:

- **Un protagonista por pantalla:** el campo en la alineación, la cifra de puntos en el inicio y en la jornada, la posición en la clasificación. El resto, callado.
- **Puntos legibles de un vistazo:** cifra condensed con signo, en `brand-text` o `danger-text`, y el desglose solo cuando se pide.
- **Plazos sin alarmismo:** el plazo junto al título (como *Se bloquea en 1 d 4 h* en el boceto actual), en ámbar solo cuando está cerca (decisión 11).
- **Textos** según *Textos* de `ui/README.md`: *equipo*, nunca *peña*; cada botón dice lo que hace y su aviso usa el mismo verbo (*Guardar alineación* → *Alineación guardada*; *Guardar valoraciones* → *Valoraciones guardadas*).

### 6.2 Composición propuesta

Puntos de partida para el boceto. Los cierra la tarea 3.

**Inicio** (`''`):

```
Jornada 12            [◷ Se bloquea en 5 h]  título (paso 5) y plazo
Contra CD Arrabal, domingo 2 de noviembre, 11:00
──────────────────────────────
Tu alineación        87,4 / 100 M      ›   fila: estado y presupuesto; avisos en ámbar
1 de 2 cambios                              (OVER_BUDGET, RETIRED_PLAYERS, SIZE_MISMATCH)
──────────────────────────────
Valora la jornada 11       3 de 9      ›   una fila por ventana abierta, con su cierre
Cierra en 14 h
──────────────────────────────
         +38                                cifra protagonista (paso 7): tu última jornada
   Jornada 11 · 2.º de 6                   y tu posición en la general (enlace)

[    Valorar la jornada 11     ]            acción contextual (decisión 14)
```

Estados: sin partidos por jugar (vacío con el contorno del peto: *Aún no hay partidos. Cuando se programe el siguiente, lo verás aquí.*), sin jornadas puntuadas (sin la cifra) y alineaciones ya bloqueadas (la fila enlaza a `lineups/:matchId`).

**Alineación** (`lineup`): la del boceto de `visual-direction.html`, con un control segmentado *Campo* / *Lista* bajo las cifras. Las filas del campo salen de la posición de cada jugador (portería propia abajo). Las plazas libres son petos discontinuos: en la fila de su posición si faltan mínimos de formación, y si no, en una fila *Plazas libres* al pie del campo. Si el campo está sin guardar, encima de la acción va *Descartar cambios* (`quiet`). En `lg`, el campo a la izquierda y las cifras, avisos y acción a la derecha.

```
Hoja de candidatos (al tocar un peto o una plaza)
Cambiar a Pablo                         título de la hoja
Te quedan 12,6 M                        lo que queda de presupuesto sin él
── Delanteros ──────────────
(9) Pablo          15,2 M  ✓  En la alineación
(11) Rubén         13,0 M     Cabe
(7) Hugo           18,4 M     Supera el presupuesto   (se puede elegir: valida el servidor)
── Medios ──────────────────
…
[ Quitar de la alineación ]
```

**Valorar** (`ratings/:matchId`):

```
Jornada 11              [◷ Cierra en 14 h]
Ganamos 3–1 a CD Arrabal
──────────────────────────────
(1) Diego        Portero                    peto, nombre y posición en texto
    1 penalti parado                        lo que hizo en el acta, apagado
    ( 0 )( 1 )( 2 )( 3 )( 4 )   Quitar      picas; en nota: [−]  7  [+]
──────────────────────────────
…
[    Guardar valoraciones      ]
```

Con la ventana cerrada, cada fila enseña mi voto, la media, los votos que contaron y los puntos por valoración (`bfPoints`). Con `status: CLOSED` y sin resultados (el job aún no ha pasado), *Calculando los resultados*. Si el usuario está excluido, en lugar de los controles va un aviso: *El administrador ha cancelado tus valoraciones de este partido.* (RF-41, sin motivo).

**Clasificación** (`standings`): pestañas *General* / *Por jornada*. La general es una fila por manager con la posición (condensed, grande), el avatar, el nombre, los puntos y, apagados, las jornadas y los puntos de la última; mi fila va con `current`. *Por jornada* lleva un selector de jornada (`bf-select`, solo las cerradas) y lleva a `standings/:matchId`.

**Puntos de una jornada** (`standings/:matchId`): pestañas *Managers* y *Jugadores*. En *Managers*, cada fila se abre y enseña la alineación bloqueada de ese manager con los puntos de cada jugador; *Fuera de presupuesto* va como `bf-badge` en ámbar. En *Jugadores*, los del acta con sus puntos por acciones, por valoración y en total.

**Ficha del jugador** (`squad/:playerId`): cabecera con el peto grande, el nombre (paso 6), la posición en texto y en chip, y las cifras *Valor actual* y *Valor inicial*. Debajo, la lista de jornadas de la decisión 8. *Dado de baja* como `bf-badge` si lo está.

### 6.3 Componentes de `ui/` (FE-18)

Los que faltan de FE-18, más los que piden estas pantallas. Cada uno con su test y en `/_ui` con todos sus estados en los dos temas:

| Componente | Uso |
| --- | --- |
| `bf-tabs` + `bfTab` | Pestañas dentro de una página (*Managers* / *Jugadores*) con `role="tablist"` y teclado del CDK (`ListKeyManager`); también como enlaces de ruta (`bfTabLink`) para *General* / *Por jornada*. |
| `bf-select` (`select[bfSelect]`) | Select nativo con el estilo de `bfInput`, dentro de `bf-field`. |
| `bf-stepper` | Número con − y +, `min`, `max` y `step` como `input()` (los da la página según el formato) y estado vacío opcional; `role="spinbutton"`. |
| `bf-countdown` | Tiempo hasta un `Instant` (*1 d 4 h*, *3 h 20 min*, *12 min*, *menos de 1 min*) con su `<time datetime>`; en ámbar bajo el umbral (decisión 11); emite `elapsed` al llegar a cero. Se actualiza cada minuto, y cada segundo en el último. No sabe de zonas horarias: la fecha absoluta la pone la página con `bfDate`. |
| `bf-notice` | Aviso en línea (`info`, `warning` y `danger`, con icono y texto, sin texto sobre el rojo base): avisos de la alineación, excluido, *Calculando los resultados*. |
| `bf-figure` | Cifra condensed con su unidad o contexto más pequeño y la etiqueta debajo (`87,4` `/ 100 M` *Presupuesto*). |
| `bf-disclosure` | Fila que se abre para enseñar un detalle (`CdkAccordionItem`, `aria-expanded`), sobre `bfListItem`. |

### 6.4 Componentes de `shared/` (FE-19)

| Componente | Uso |
| --- | --- |
| `bf-position-chip` | Posición en texto sobre su color (`on-color`). Los textos van en `Record<Position, string>` en `shared/position/`. |
| `bf-player-row` | La «tarjeta de jugador» de FE-19 como fila, sin foto (decisión 5): peto pequeño, nombre, posición en texto y, opcionales, valor y un *trailing* proyectado. La usan la plantilla, los candidatos, valorar y los desgloses. |
| `bf-points` | Marcador de puntos con signo y color (`brand-text`, `danger-text`, neutro en 0), en tamaños de lista y protagonista. |
| `bf-rating-input` | Picas (radios 0–4) o nota (`bf-stepper` 0–10) según el `format` que recibe, con *Sin voto* y *Quitar voto*; `model()` de `number \| null`. Los máximos de cada formato los fija el contrato (0–4 y 0–10, en la descripción de `PlayerRatingRequest`), no la liga, así que pueden ser constantes de presentación. |
| `bf-pitch` | Campo de césped a franjas con las filas por posición, petos de 44 px con nombre y valor, plazas libres, selección con el anillo lavanda y `output()` al tocar. La lista alternativa es `bf-lineup-list`, con las mismas entradas y salidas. Accesible: cada peto es un botón con nombre, posición y valor en su etiqueta. |
| `bf-points-breakdown` | Desglose de acciones (texto de `Record<ScoringAction, string>` y sus puntos) y de la valoración. Lo usan la ficha, el historial y la jornada. |
| Pipes `bfMoney` y `bfPoints` | Decisión 13; `bfDate` ya existe. |

## 7. Pantallas

Cada una con carga (*skeleton* con la forma del contenido), vacío, error con *Reintentar* y contenido (FE-14); textos con `i18n` e id `@@<feature>.<pantalla>.<texto>`; envíos deshabilitados sin conexión (FE-31); y la acción principal abajo (FE-30).

- **Inicio:** decisión 14 y §6.2. Si el usuario no ha guardado nunca la alineación (`savedAt` nulo), la fila dice *Aún no tienes alineación* y la acción es *Hacer alineación*.
- **Alineación:**
  - Borrador local inicializado con `GET /lineup`. Tamaño, formación y presupuesto se comprueban contra `GET /settings` y los valores actuales de la plantilla, y lo que falla se pinta en el campo y en las cifras, sin impedir guardar (decisión 10).
  - *Guardar alineación* hace el `PUT` y repinta con la respuesta.
  - Los `warnings` del servidor se pintan como `bf-notice`.
  - Sin `targetMatch`: *No quedan partidos por jugar. Tu alineación contará para el siguiente que se programe.*
- **Alineaciones bloqueadas:** las de todos los managers en ese partido, una fila por manager que se abre y enseña sus jugadores con el valor al bloquear, el total y *Fuera de presupuesto*. Con `LINEUPS_NOT_LOCKED`: *Las alineaciones se verán cuando se bloqueen*, con el plazo.
- **Valoraciones:** las abiertas primero, con su cuenta atrás y *3 de 9*, y después las cerradas. *Excluido* va como `bf-badge`.
- **Valorar un partido:** §6.2. *Guardar valoraciones* envía todos los votos (el `PUT` los reemplaza); un jugador sin voto no se envía. Con la ventana abierta, ninguna cifra de otros (FE-11): solo los votos propios.
- **Clasificación general:** §6.2. Sin jornadas puntuadas: *La clasificación empieza cuando se puntúe la primera jornada.* Una fila enlaza al historial de ese manager.
- **Puntos de una jornada:** §6.2. Con `MATCH_NOT_CLOSED` o `MATCH_NOT_SCORED`: *Los puntos se publican al cerrar las valoraciones*, con un enlace a valorar si la ventana sigue abierta.
- **Historial de un manager:** una fila por jornada, de la más reciente a la más antigua (el API las da al revés). Cada una se abre y enseña sus jugadores con `bf-points-breakdown`. Las que tienen `inStandings: false` van marcadas como *Antes de su última alta*, y las de `overBudget`, como *Fuera de presupuesto*. Un manager se llega a ver a sí mismo desde su fila de la clasificación.
- **Liga (índice):** filas enlace a *Plantilla*, *Calendario*, *Reglas* y *Miembros*. El admin ve además una sección *Administrar* con *Miembros e invitación*, y el plan 13 añade las demás.
- **Plantilla:** los activos agrupados por posición, con su peto, nombre y valor; cada fila enlaza a la ficha.
- **Ficha del jugador:** §6.2 y decisión 8. Sin jornadas puntuadas: *Aún no ha puntuado.*
- **Calendario:** una fila por jornada con el rival, *En casa* o *Fuera*, la fecha y el resultado si está cerrado. Enlaza al partido.
- **Partido y acta:** el resultado y, si está cerrado, el acta (jugadores, goles, asistencias y tarjetas) y los enlaces a *Valorar*, *Puntos de la jornada* y *Alineaciones*. Si no está cerrado: *El acta se publica al cerrar el partido.*
- **Reglas de la liga:** los parámetros en filas con su cifra, y la tabla de puntos por acción y posición como `<table>` con cabeceras de posición, que en 360 px hace scroll horizontal dentro de su contenedor.

## 8. Plazos, refresco y cambios sin guardar

- **Cuentas atrás** (FE-09): orientativas, con la hora del navegador. Al llegar a cero, la página recarga sus recursos, y la verdad la dice el servidor.
- **Cambio de jornada mientras se edita:** si al recargar (o en la respuesta del `PUT`) `targetMatch` es otro partido distinto del que se veía, un toast lo explica: *La jornada 12 ya se ha bloqueado. Lo que guardes cuenta para la 13.* Así nadie cree que ha cambiado una alineación que ya estaba bloqueada.
- **Volver a la pestaña** (decisión 15): un helper de `core/http` (`onVisible(callback)`), que se llama en el constructor de la página, recarga los recursos con plazos.
- **Cambios sin guardar** en la alineación y en valorar: un `canDeactivate` común (`core/layout/unsaved-changes`) pregunta con `Confirm` (*Tienes cambios sin guardar. ¿Salir sin guardar?*) y `beforeunload` avisa al cerrar la pestaña. No avisa después de guardar ni si el borrador es igual a lo guardado.
- **`409` de plazo** (`RATING_WINDOW_CLOSED`, o guardar justo al bloquear): toast con el texto del código, recarga de la vista y el borrador descartado, porque lo que se veía ya no vale.

## 9. Tests y CI

- **Backend:** los de §3 (`includeRetired` para un manager, `ScoringAction` en el spec, reloj de pruebas con y sin perfil). Se regeneran `api/openapi.json` y `schema.d.ts`.
- **Unitarios y de componentes** (FE-33):
  - cada componente nuevo de `ui/` y `shared/` (estados, teclado y accesibilidad básica);
  - cada servicio de API;
  - las funciones puras: cruces de ids, *Antiguo miembro*, modelos de vista de cada página, comprobación local del borrador de la alineación, *type guard* de `ScoringAction`, `bfMoney` y `bfPoints`;
  - `bf-countdown` con temporizadores falsos;
  - el `canDeactivate`;
  - cada página con sus estados y sus `409`, con los servicios simulados.
- **E2E del manager** (FE-34), en `e2e/`, en móvil (Chromium y WebKit) y en los dos temas, con `@axe-core/playwright` en cada pantalla:
  - **Preparación por API**, con el login de pruebas: el admin crea la liga y la plantilla, programa un partido a unas horas de la hora del servidor (`GET /api/e2e/clock`) y el manager se une por la invitación.
  - **Recorrido:**
    1. El manager hace su alineación en el campo con la hoja de candidatos y la guarda. El admin guarda la suya por API.
    2. Se adelanta el reloj más allá del bloqueo, y el inicio y las alineaciones bloqueadas lo reflejan.
    3. El admin guarda el acta y cierra el partido por API.
    4. El manager valora en picas, cambia un voto y lo guarda.
    5. Se adelanta el reloj más allá del cierre de la ventana.
    6. El manager ve la media en valorar, los puntos de la jornada, la clasificación general, su historial y la ficha de un jugador con el desglose.
  - **Reloj del navegador:** se sincroniza con el del servidor (`page.clock`), para que las cuentas atrás cuadren.
  - Los specs que mueven el reloj van en serie (`workers: 1` en ese proyecto), porque el reloj es del servidor entero.
- **CI:** el job `e2e` arranca el backend con el perfil `e2e`, que ya acorta los `cron` (§3.3). Sin jobs nuevos.

## 10. Cambios en la documentación

En el cambio de la tarea que los provoca:

- `requirements.md`:
  - **FE-19** (decisión 5): la tarjeta de jugador queda sin foto en v1.
  - **Mapa de pantallas**: añade *Liga (índice)*, *Calendario y partido*, *Alineaciones bloqueadas* y *Reglas*, y cambia el endpoint de *Plantilla y ficha* a `GET /players?includeRetired=true`.
  - **Roadmap**: marca el ítem al cerrar.
- `ui/README.md`: *Composición* de las pantallas nuevas, y los componentes nuevos en *Componentes*.
- `frontend.md`: cruces de ids, *Antiguo miembro*, destinos con prefijos, `onVisible` y `canDeactivate`.
- `testing.md`: el reloj de pruebas y los specs en serie.
- `commands.md`: `GET/POST /api/e2e/clock` y los `cron` del perfil `e2e`.
- `status.md` y *Estado* de `AGENTS.md`.

## 11. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | Revisar este plan con el usuario y cerrar las propuestas (§2) | Decisiones cerradas | ✅ |
| 2 | `includeRetired` para miembros, `ScoringAction` en el spec, reloj de pruebas con el perfil `e2e` y sus tests; spec y tipos regenerados (§3) | Backend listo (FE-05, FE-34) | |
| 3 | Boceto de las pantallas nuevas con la skill, revisado con el usuario; *Composición* en `ui/README.md` (§6.1, §6.2) | Composición cerrada (FE-28) | |
| 4 | `SquadApi`, `MatchApi`, `LineupApi`, `RatingApi` y `ScoringApi`, y los cruces de `shared/roster` (§5) | Contrato de las pantallas (FE-05 a FE-09) | |
| 5 | Componentes de `ui/` de §6.3 y `/_ui` | Catálogo de FE-18 completo (FE-17, FE-18, FE-20) | |
| 6 | Componentes y pipes de `shared/` de §6.4 | Catálogo de FE-19 (FE-19, FE-23) | |
| 7 | Navegación de cinco destinos con prefijos, índice *Liga*, `onVisible` y `canDeactivate` (§4.2, §8) | Shell completo (FE-30) | |
| 8 | Plantilla, ficha, calendario, partido y reglas | RF-07, RF-11 a RF-15 vistos por el manager | |
| 9 | Alineación y alineaciones bloqueadas | RF-24 a RF-29 | |
| 10 | Valoraciones y valorar un partido | RF-17 a RF-23 | |
| 11 | Clasificación general, puntos de una jornada e historial de un manager | RF-30 a RF-33 | |
| 12 | Inicio de la jornada | RF-17, RF-25, RF-32 | |
| 13 | E2E del manager con el reloj de pruebas y axe (§9) | Jornada del manager automatizada (FE-34) | |
| 14 | Documentación de §10, `status.md`, *Estado* de `AGENTS.md` y marcar el ítem de la Fase 2 en `requirements.md` | Ítem de la Fase 2 cerrado | |

Cada tarea es un commit en Conventional Commits citando sus códigos: `feat(squad): …`, `feat(frontend): … (RF-xx, FE-xx)`, `test(frontend): …`, `docs: …`. El inicio va al final porque reúne lo de las demás pantallas.

## 12. Riesgos y notas

- **Campo con muchos jugadores:** `lineupSize` es configurable. Con 11 jugadores y cinco en una fila, en 360 px los nombres no caben: cada fila admite como mucho cinco petos y parte el resto en dos, y el nombre se recorta con su etiqueta completa en el botón. Se comprueba en el boceto con 7 y con 11.
- **Reloj compartido en E2E:** el reloj de pruebas es de todo el backend y solo avanza. Cada spec crea su propia liga y programa sus partidos respecto a la hora del servidor, nunca a la del sistema; los que lo mueven van en serie.
- **Jobs con `cron` corto:** con el perfil `e2e`, los jobs corren cada 2 s y llenan el log de ShedLock. Es solo para E2E y no afecta a la suite de integración, que usa su propio reloj.
- **Bloqueo entre la hora y el job:** después de `lockAt` y antes de que pase el job, `GET /lineup` ya da el partido siguiente, pero `GET /lineups/{matchId}` aún responde `LINEUPS_NOT_LOCKED`. El inicio lo trata como *Bloqueándose…* y recarga.
- **Peticiones por pantalla:** varias pantallas piden la plantilla y los miembros además de su recurso. Para unos 50 usuarios cabe en RNF-09, y las peticiones van en paralelo. Si pesa, se cachea la plantilla por liga en la sesión de la página, sin estado global (FE-13).
- **Fotos subidas sin mostrar:** con la decisión 5, el admin puede subir fotos (plan 13) que nadie ve. El plan 13 decide si las enseña en su propia pantalla o si oculta la subida hasta que se diseñe la variante con foto.
- **Bundle:** cada feature nueva es lazy; `bf-pitch` y `bf-tabs` solo los cargan sus pantallas. Se vigila el presupuesto de RNF-13 en cada tarea.
