# Estado actual y roadmap

Actualiza este archivo (y el resumen de *Estado* en `AGENTS.md`) cuando cierres un ítem del roadmap.

## Estado actual

Fase 1 completa y Fase 2 (valoraciones) con el desarrollo del backend completo (plan 09). En la Fase 0 están hechos el esqueleto del backend, el modelo base con el login de Google y el esqueleto de `frontend/` con las pantallas de identidad y liga (plan 11, cerrado); faltan Dockerfile, Compose de producción y VPS. Pendiente en la Fase 2: las pantallas de manager y de admin, el E2E de la jornada completa y la prueba real; el plan `12` (manager, `docs/12-manager-screens-plan.md`) está en curso, con su backend hecho (tarea 2: plantilla completa para los miembros, `ScoringAction` en el spec y reloj de pruebas) y el boceto de sus pantallas cerrado (tarea 3), y el `13` (admin) está por redactar.

### Fase 0 · Fundamentos (en curso)

- Esqueleto de `backend/` (kernel `shared`, `CommandBus`, tests de arquitectura) según `docs/01-backend-structure-plan.md`.
- Modelo base y login de Google (`identity`, `league`) según `docs/02-base-model-and-google-login-plan.md`.
- **Esqueleto de `frontend/`** (FE-01 a FE-35, `docs/11-frontend-foundation-plan.md`, completo):
  - **Backend:** `OpenApiSpecTests` escribe el spec en `api/openapi.json` (determinista); el `code` del `Problem` es el enum `ErrorCode` del spec (unión de los `<Módulo>Error` por escaneo del classpath, con `LeagueError`, `SquadError` y `MatchError`), en todo `409` y en los `400` de límites y reglas de negocio; tras el login vuelve siempre a `/` (o a `/login?error`), `/login` es la pantalla de Angular y el proxy solo manda `/login/oauth2`; `server.forward-headers-strategy: framework` para el redirect URI detrás de Caddy; login de pruebas `POST /api/e2e/login` solo con el perfil `e2e` (oculto en el spec, avisa al arrancar); un `@Valid` que falla responde con `violations`.
  - **Esqueleto:** Angular 22 (standalone, zoneless, SCSS, sin SSR) con Node 26 LTS, TypeScript y plantillas estrictas, presupuestos de RNF-13, `autoCsp`, `@angular/localize` con `es` y `messages.xlf` versionado, ESLint (`strictTypeChecked`, `boundaries`, `i18n` con id), Stylelint que exige tokens, Prettier y Vitest con Angular Testing Library.
  - **Contrato:** `schema.d.ts` generado y versionado, `apiPath` tipado, `IdentityApi` y `LeagueApi`, CSRF, interceptor de errores (`401`, `403`, `404`, `5xx` y red con reintento), `toProblem`, `violates` y `ERROR_TEXTS`.
  - **Diseño:** dirección visual en `frontend/src/app/ui/README.md` (boceto en `docs/design/visual-direction.html`), paleta generada en OKLCH, tokens, tema claro y oscuro sin parpadeo, Barlow y Lucide, test de contraste, y los componentes de `ui/` de §7.4 (más `bf-segmented` y `bf-load-error`) con su test y en `/_ui`.
  - **Núcleo y shell:** `SessionStore`, login y logout con ruta de vuelta, `authGuard`, `memberGuard`, `adminGuard` y `guestGuard`, rutas lazy por feature, shell con cambio de liga y navegación inferior y lateral, aviso sin conexión, páginas de error, título por ruta y `core/time` (`es-ES`, `Europe/Madrid`, cambios de hora probados).
  - **Pantallas:** login, perfil (tema y cerrar sesión), mis ligas, crear liga, invitación, inicio provisional (estado vacío), miembros y salir, y admin · invitación y miembros, cada una con carga, vacío, error y contenido, y sus tests.
  - **Servido:** `deploy/Caddyfile` (estáticos con *fallback*, caché `immutable` de los ficheros con hash, proxy, cabeceras y CSP) y el perfil `caddy` del Compose de desarrollo en `http://localhost:8081`.
  - **Calidad:** E2E de humo con Playwright en móvil (Chromium y WebKit, dos temas) con axe y sin violaciones de CSP, y CI con los jobs `backend`, `frontend` y `e2e`.
- Faltan Dockerfile, Compose de producción y VPS (ver `docs/requirements.md`). CI y el `Caddyfile` ya existen.

### Fase 1 · Núcleo jugable (completa)

- **`league` y `datadefinition`** (RF-05–08, `docs/03-league-invitations-settings-plan.md`): crear liga, invitación por enlace, parámetros y tabla de puntos por liga, y gestión de miembros.
- **`squad`** (RF-09–11, `docs/04-squad-players-plan.md`): alta, edición y baja lógica de jugadores con valor inicial; foto subida desde el navegador a S3 con URL prefirmada; `SquadApi` para otros módulos.
- **`match`** (RF-12–16, `docs/05-matches-stats-plan.md`): calendario por jornada (1 jornada = 1 partido), aplazar y borrar, acta con resultado, participantes y estadísticas (con avisos de coherencia), cierre que publica `MatchClosed`, correcciones de partidos cerrados con `MatchCorrected` y auditoría de cada cambio del admin (RNF-04).
- **`lineup`** (RF-24–29, `docs/06-lineups-plan.md`): una alineación de trabajo versionada por manager que se arrastra sola, validada por tamaño, formación, presupuesto y límite de cambios; la bloquea el job `LockDueLineupsJob` (o el cierre del partido) con los valores vigentes, se desbloquea si el partido se aplaza, se publica en `LineupsLocked` y solo es visible para todos los miembros una vez bloqueada.
- **`scoring`** (RF-15, RF-30–33, `docs/08-scoring-plan.md`): con el acta (`MatchClosed`) y las alineaciones bloqueadas (`LineupsLocked`) de un partido, lleguen en el orden que lleguen, guarda su propia copia, congela la tabla de puntos de la liga y puntúa a cada jugador del acta (acciones según su posición más la valoración, sin pesos) y a cada manager (0 fuera de presupuesto o sin alineación); publica `MatchdayScored`, recalcula con la tabla congelada ante `MatchCorrected` (el recálculo forzado por el admin queda para la Fase 4) y sirve la clasificación acumulada y por jornada (miembros actuales desde su `joinedAt`, empatados con la misma posición) y los historiales de puntos del manager y del jugador.

### Fase 2 · Valoraciones (desarrollo completo, `docs/09-ratings-plan.md`)

- **Ventana (RF-17):** se abre con `MatchClosed`, congelando los parámetros de valoración de la liga (formato, conversión por formato, mínimo de votos y nota por defecto); sigue las correcciones del acta (`MatchCorrected`: los jugadores quitados pierden sus votos; con la ventana cerrada publica `RatingsCorrected` si cambia algún resultado); la cierra el job `CloseDueRatingWindowsJob` al vencer, que congela el resultado de cada jugador y publica `RatingWindowClosed`.
- **Votos (RF-18–21):** cada miembro (también el admin) vota a los jugadores del acta en el formato congelado y reemplaza sus votos mientras la ventana siga abierta según el reloj del servidor (tomando la ventana `for share`, así que un voto queda dentro del cierre o recibe `409`); ve solo los suyos y, al cerrar, la media y los votos de cada jugador; tiene el listado de ventanas de la liga con cuántos ha valorado.
- **Cancelación (RF-41):** con la ventana abierta, el admin cancela las valoraciones de un miembro en un partido: se borran sus votos, queda excluido de valorarlo para siempre (tomando la ventana `for update`, así que un voto a la vez queda borrado o recibe `409`) y se publica `RaterExcluded` (sin consumidores hasta la Fase 3). El excluido ve que lo está; solo el admin ve a quién ha excluido, nunca sus votos.
- **Puntos por valoración (RF-22, RF-23, RF-30):** `scoring` consume `RatingWindowClosed` y `RatingsCorrected` (`RatingEventsListener`): guarda su copia de los puntos por valoración, puntúa el partido solo cuando tiene acta, alineaciones bloqueadas **y** valoraciones (la jornada no aparece en clasificaciones ni historiales mientras la ventana siga abierta, y `MatchdayScored` sale ya con los puntos definitivos) y recalcula si una corrección cambia las valoraciones.
- **Tests:** las pantallas de `rating` (listado y partido) en `ScreenStatementsTests`, y la jornada de `MatchdayFlowTests` con votos de los dos miembros por HTTP, el cierre por el job y los puntos por valoración en clasificaciones e historiales, también tras una corrección.
- **Pantallas de manager** (`docs/12-manager-screens-plan.md`, en curso): hecho el backend que piden (tarea 2): `GET /players?includeRetired=true` para cualquier miembro, el enum `ScoringAction` en el spec (clave de `actions` y `scoringRules`) y, con el perfil `e2e`, el reloj ajustable `E2eClock` (`GET/POST /api/e2e/clock`) con los jobs de bloqueo y cierre cada 2 s. Cerrado el boceto (tarea 3): `docs/design/manager-screens.html`, con su composición en la sección *Composición* de `frontend/src/app/ui/README.md`. Siguiente: los servicios de API y los cruces de `shared/roster` (tarea 4).
- **Falta:** la prueba real con los amigos, que necesita las pantallas de manager y de admin y el VPS de la Fase 0.

### Tests de integración

Completos para lo implementado según `docs/07-integration-tests-plan.md` (infraestructura compartida, escaneos de autorización y errores, concurrencia, reintento de eventos, sesión, zona horaria, sentencias por pantalla y flujo de una jornada). Su §4.3 fija los contratos que deben cumplir los módulos que faltan.

## Roadmap

Fase 0 Fundamentos → Fase 1 Núcleo jugable → Fase 2 Valoraciones → Fase 3 Mercado y notificaciones → Fase 4 Pulido y operación → Fase 5 Cloud.

Cada fase tiene una puerta de salida; no adelantes trabajo de fases posteriores salvo que se pida. Detalle y checklist en `docs/requirements.md`.
