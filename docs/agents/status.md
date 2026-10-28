# Estado actual y roadmap

Actualiza este archivo (y el resumen de *Estado* en `AGENTS.md`) cuando cierres un ítem del roadmap.

## Estado actual

Aún no hay `frontend/`. Todo lo hecho está en `backend/`.

Los requisitos del frontend ya están en `docs/requirements.md` (RNF-01, RNF-12 a RNF-16 y FE-01 a FE-35, según `docs/10-frontend-requirements-plan.md`), resumidos para agentes en [frontend.md](frontend.md). El siguiente paso es `docs/11-frontend-foundation-plan.md`: el esqueleto de `frontend/`, el trabajo de backend que se desprende (export del spec, códigos de error en el spec, vuelta tras el login, login de pruebas para E2E, CORS del bucket, Caddy) y las primeras pantallas.

### Fase 0 · Fundamentos (en curso)

- Esqueleto de `backend/` (kernel `shared`, `CommandBus`, tests de arquitectura) según `docs/01-backend-structure-plan.md`.
- Modelo base y login de Google (`identity`, `league`) según `docs/02-base-model-and-google-login-plan.md`.
- Faltan Dockerfile, Compose de producción con Caddy, CI y VPS (ver `docs/requirements.md`).

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
- **Falta:** la prueba real con los amigos, que necesita el `frontend/` y el VPS de la Fase 0.

### Tests de integración

Completos para lo implementado según `docs/07-integration-tests-plan.md` (infraestructura compartida, escaneos de autorización y errores, concurrencia, reintento de eventos, sesión, zona horaria, sentencias por pantalla y flujo de una jornada). Su §4.3 fija los contratos que deben cumplir los módulos que faltan.

## Roadmap

Fase 0 Fundamentos → Fase 1 Núcleo jugable → Fase 2 Valoraciones → Fase 3 Mercado y notificaciones → Fase 4 Pulido y operación → Fase 5 Cloud.

Cada fase tiene una puerta de salida; no adelantes trabajo de fases posteriores salvo que se pida. Detalle y checklist en `docs/requirements.md`.
