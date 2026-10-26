# AGENTS.md — buddy-fantasy

Guía para agentes de código (Claude Code, Codex, Copilot…) que trabajen en este repositorio.
La fuente de verdad funcional es `docs/requirements.md`: ante cualquier duda de negocio, léelo antes de decidir.

## Qué es

Fantasy para una liga de peñas de domingo. Un administrador registra **un único equipo real** por liga, sus partidos, resultados y estadísticas. Los managers valoran a los jugadores tras cada partido y compiten con sus alineaciones en una clasificación.

- **v1 incluye:** una liga por administrador, un equipo real por liga, valoraciones, puntuación combinada, alineaciones con límite de cambios, valor de mercado dinámico, clasificación, cuentas y notificaciones.
- **v1 NO incluye:** apps nativas, pagos, chat, varias competiciones por liga, compraventa de jugadores entre managers. No implementes nada de esto sin que se pida explícitamente.
- **Objetivo:** funcionar para un grupo de amigos en un VPS barato, sin bloquear la migración a cloud.

## Estado actual

Estamos en la **Fase 0 · Fundamentos** del roadmap. Existe el esqueleto de `backend/` (kernel `shared`, `CommandBus`, tests de arquitectura) según `docs/01-backend-structure-plan.md`, con el modelo base y el login de Google (`identity`, `league`) según `docs/02-base-model-and-google-login-plan.md`. Primer ítem de la **Fase 1** hecho (`docs/03-league-invitations-settings-plan.md`): crear liga, invitación por enlace, parámetros y tabla de puntos por liga, y gestión de miembros (RF-05–08), con el módulo `datadefinition`. Aún no hay `frontend/`. Mantén actualizada la sección *Comandos* de este archivo.

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21+, Spring Boot 3, Spring Modulith (monolito modular) |
| Base de datos | PostgreSQL + Flyway |
| Autenticación | Spring Security + OAuth2 login (solo Google), sesión en Spring Session JDBC |
| Jobs | `@Scheduled` + ShedLock |
| Frontend | Angular + `@angular/pwa`, mobile first (Angular Material o PrimeNG, por decidir) |
| Notificaciones | webpush-java + SMTP (Brevo, Resend…) |
| Infra | Docker Compose (app + Postgres + Caddy), GitHub Actions |

Estructura de repositorio prevista (ajústala aquí si cambia):

```
backend/    # Spring Boot (Maven o Gradle)
frontend/   # Angular PWA
deploy/     # docker-compose, Caddyfile, scripts de backup
docs/       # requisitos y decisiones
```

## Arquitectura del backend

Monolito modular con Spring Modulith. Un paquete de primer nivel por módulo:

| Módulo | Responsabilidad | RF |
| --- | --- | --- |
| `identity` | Usuarios, login, perfil, sesiones | RF-01–04 |
| `league` | Ligas, membresías, roles por liga, invitaciones, parámetros | RF-05–08 |
| `squad` | Jugadores del equipo real y su ficha | RF-09–11 |
| `match` | Partidos, convocatoria, resultado, estadísticas | RF-12–16 |
| `rating` | Ventana y valoraciones de los managers | RF-17–23 |
| `lineup` | Alineaciones, bloqueo y límite de cambios | RF-24–29 |
| `scoring` | Puntos por jugador/manager y clasificaciones | RF-30–33 |
| `market` | Valor de mercado de los jugadores | RF-34–36 |
| `notification` | Web Push, email, preferencias y centro de avisos | RF-37–40 |
| `datadefinition` | Definiciones de datos compartidas sin lógica (`Position`, `ScoringAction`) | — |

Reglas:

- Los módulos **no acceden a los internals de otros módulos**. Solo usan su API pública (paquete raíz del módulo) o **eventos de dominio** (`MatchClosed`, `RatingWindowClosed`, `MatchdayScored`…).
- `datadefinition` es un módulo hoja y su paquete raíz es API pública: solo enums y records sin lógica, usados por dos o más módulos. No depende de ningún otro módulo; todos pueden depender de él. Si un tipo necesita comportamiento, va a `shared` o a su módulo dueño.
- Mantén un test de verificación de Modulith (`ApplicationModules.of(...).verify()`) que falle si se rompen las dependencias.
- Cada módulo es dueño de sus tablas. Nada de joins SQL entre tablas de módulos distintos.
- Diseña pensando en que un módulo pueda extraerse a servicio algún día: eventos en lugar de llamadas síncronas cuando el flujo lo permita.

## Reglas de dominio (no romper)

### Roles

Los roles son **por liga**, no globales. Un usuario puede ser administrador de una liga y manager en otra. Toda comprobación de permisos recibe la liga como contexto. Una membership tiene un único rol (`ADMIN` o `MANAGER`) y **el administrador siempre juega como manager** en su liga: trata `ADMIN` como manager al preguntar si alguien puede alinear, valorar o puntuar.

### Puntuación por acción (precargada, editable por liga)

| Acción | Portero | Defensa | Medio | Delantero |
| --- | --- | --- | --- | --- |
| Gol | +6 | +5 | +4 | +3 |
| Gol de penalti | +3 | +3 | +3 | +3 |
| Penalti parado | +3 | +3 | +3 | +3 |
| Penalti fallado | −2 | −2 | −2 | −2 |
| Asistencia | +1 | +1 | +1 | +1 |
| Gol en propia puerta | −3 | −3 | −3 | −3 |
| Tarjeta amarilla | −1 | −1 | −1 | −1 |
| Doble amarilla | −3 | −3 | −3 | −3 |
| Roja directa | −5 | −5 | −5 | −5 |

- El gol de penalti vale +3 **en cualquier posición** (no se suma además el gol normal).
- La doble amarilla vale −3 **en total**; no se suman las dos amarillas.
- La tabla es un dato por liga (sembrado con estos valores), nunca constantes en código.

### Puntos

- Puntos del jugador en la jornada = puntos por acciones + puntos por valoración. **Sin porcentajes ni pesos** entre ambas partes.
- Puntos del manager = suma de los puntos de sus jugadores alineados en la alineación **bloqueada**.
- Si un jugador no recibe valoraciones: solo puntos por acciones (o la nota por defecto de la liga, si está configurada).

### Valoraciones

- Se abren al cerrar el partido; ventana por defecto de 24 h (configurable).
- Solo se valora a jugadores que jugaron. Valorar es opcional.
- Formato por liga: picas 0–4 (por defecto, 1 pica = 1 punto) o nota decimal 0–10.
- Editables mientras la ventana siga abierta.
- **Anónimas**: ninguna API devuelve valoraciones individuales de otros usuarios; solo la media.

### Alineaciones

- N jugadores (por defecto 7), formación por posiciones opcional.
- Editable hasta X horas antes del partido (por defecto 2 h); máximo Y cambios por jornada (por defecto 2).
- Si el manager no la toca, se arrastra la de la jornada anterior.
- Presupuesto máximo opcional (desactivado por defecto).

### Valor de mercado

- Tras cerrar cada jornada: `Δvalor = k × (puntos − media de la jornada) × valor`, con `k = 0,05` por defecto, tope de ±10 % por jornada y valor mínimo.
- Guarda el histórico de valores (se pinta en una gráfica).

Todos los parámetros configurables (ventana, formato, bloqueo, cambios, tamaño, k, tope…) se guardan **por liga**. Ver la tabla completa en `docs/requirements.md`.

## Invariantes técnicas

- **Plazos en el servidor.** El bloqueo de alineaciones y el cierre de valoraciones se validan siempre en backend. El cliente solo refleja el estado.
- **Jobs idempotentes.** Todo `@Scheduled` lleva ShedLock y puede ejecutarse dos veces sin duplicar efectos. Los recálculos de jornada también son idempotentes (recalcular = mismo resultado).
- **Autorización en cada endpoint** por rol y liga. Nunca confíes en que el frontend oculte botones.
- **Fechas:** `Instant`/`timestamptz` en UTC en backend y base de datos; conversión a `Europe/Madrid` solo en la interfaz.
- **Auditoría:** los cambios del administrador en partidos y estadísticas quedan registrados para poder recalcular (RNF-04).
- **Sin estado en disco.** Configuración por variables de entorno; nada de secretos en el repo.
- **Seguridad:** HTTPS, CSRF activo, contraseñas con bcrypt/argon2 si hay login con email.
- **RGPD:** mínimos datos personales; el borrado de cuenta debe ser posible.
- **Rendimiento:** < 300 ms en pantallas principales con ~50 usuarios concurrentes. Evita N+1.

## Convenciones

- Los mappers (entidad ↔ dominio, DTO ↔ dominio) son clases con `@lombok.experimental.UtilityClass`, con métodos sin `static` explícito. Una interfaz o clase por archivo, también los repositorios Spring Data.
- Casos de uso (commands y queries) en `application/usecases/<caso>/`, un paquete por caso de uso con su `Command`/`Query` y su `Handler`.
- Los commands terminan en `Command` y las queries en `Query` (`RegisterOrLoginUserCommand`, `ListMyLeaguesQuery`); el bus único `CommandBus` expone `dispatch` (commands) y `ask` (queries).
- Código, identificadores, nombres de tablas y commits en **inglés**; textos de la interfaz en **español**.
- Migraciones Flyway versionadas (`V<n>__descripcion.sql`). **Nunca edites una migración ya aplicada**: crea una nueva.
- Referencia los requisitos por su código (`RF-xx`, `RNF-xx`) en commits, PRs y comentarios cuando aplique.
- Commits en formato Conventional Commits (`feat:`, `fix:`, `chore:`…).
- Frontend mobile first: diseña primero para pantalla de móvil.

## Tests

Obligatorios (RNF-11) para:

- Cálculo de puntos por acción según posición, incluidos los casos especiales (penalti, doble amarilla).
- Conversión de valoraciones a puntos y caso sin valoraciones.
- Plazos: bloqueo de alineación, límite de cambios, apertura/cierre de la ventana de valoración (usa un `Clock` inyectable, nunca `Instant.now()` directo).
- Algoritmo de valor de mercado: tope ±10 % y valor mínimo.
- Idempotencia de recálculos y jobs.

Usa Testcontainers con PostgreSQL para tests de integración, no H2.

## Comandos

Desde la raíz del repo:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d   # Postgres local (puerto 5432)
cd backend
mvn -B verify                                           # build + todos los tests (requiere Docker para Testcontainers)
mvn test -Dtest='ModularityTests,HexagonalArchitectureTests'   # solo verificación de arquitectura (sin Docker)
mvn spring-boot:run                                     # arranque local contra el Postgres de arriba
```

Configuración por variables de entorno (valores por defecto en `backend/src/main/resources/application.yml`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SWAGGER_UI_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (cliente OAuth de Google Cloud; sin ellas el login no funciona, pero la app arranca) y `SESSION_COOKIE_SECURE` (`true` en producción, `false` en local). Redirect URI en local: `http://localhost:8080/login/oauth2/code/google`.
Docker 29+ exige Testcontainers ≥ 1.21.4 (ya fijado en `pom.xml`). Los handlers de comandos deben ser clases, no lambdas (el bus resuelve el tipo de comando por genéricos).
Lint: pendiente.

## Decisiones abiertas

No las resuelvas por tu cuenta. Si una tarea depende de alguna, pregunta o deja la opción configurable:

- Formato de valoración por defecto y conversión de la media a puntos (redondeo incluido).
- ¿Dos managers pueden alinear al mismo jugador? (Probablemente sí, al haber un solo equipo.)
- Papel del valor del jugador: informativo, presupuesto o mercado de fichajes.
- ¿Mínimo de votos para que cuente la valoración?

## Roadmap

Fase 0 Fundamentos → Fase 1 Núcleo jugable → Fase 2 Valoraciones → Fase 3 Mercado y notificaciones → Fase 4 Pulido y operación → Fase 5 Cloud.
Cada fase tiene una puerta de salida; no adelantes trabajo de fases posteriores salvo que se pida. Detalle en `docs/requirements.md`.
