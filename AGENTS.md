# AGENTS.md — buddy-fantasy

Guía para agentes de código (Claude Code, Codex, Copilot…) que trabajen en este repositorio.
La fuente de verdad funcional es `docs/requirements.md`: ante cualquier duda de negocio, léelo antes de decidir.

## Qué es

Fantasy para una liga de peñas de domingo. Un administrador registra **un único equipo real** por liga, sus partidos, resultados y estadísticas. Los managers valoran a los jugadores tras cada partido y compiten con sus alineaciones en una clasificación.

- **v1 incluye:** una liga por administrador, un equipo real por liga, valoraciones, puntuación combinada, alineaciones con límite de cambios, valor de mercado dinámico, clasificación, cuentas y notificaciones.
- **v1 NO incluye:** apps nativas, pagos, chat, varias competiciones por liga, compraventa de jugadores entre managers. No implementes nada de esto sin que se pida explícitamente.
- **Objetivo:** funcionar para un grupo de amigos en un VPS barato, sin bloquear la migración a cloud.

## Estado actual

Estamos en la **Fase 0 · Fundamentos** del roadmap. Existe el esqueleto de `backend/` (kernel `shared`, `CommandBus`, tests de arquitectura) según `docs/01-backend-structure-plan.md`, con el modelo base y el login de Google (`identity`, `league`) según `docs/02-base-model-and-google-login-plan.md`. De la **Fase 1** están hechos cuatro ítems: crear liga, invitación por enlace, parámetros y tabla de puntos por liga, y gestión de miembros (RF-05–08, `docs/03-league-invitations-settings-plan.md`), con el módulo `datadefinition`; la plantilla del equipo en el módulo `squad` (RF-09–11, `docs/04-squad-players-plan.md`): alta, edición y baja lógica de jugadores con valor inicial, y foto subida desde el navegador a S3 con URL prefirmada, más `SquadApi` para otros módulos; y los partidos en el módulo `match` (RF-12–16, `docs/05-matches-stats-plan.md`): calendario por jornada (1 jornada = 1 partido), aplazar y borrar, acta con resultado, participantes y estadísticas (con avisos de coherencia), cierre que publica `MatchClosed`, correcciones de partidos cerrados con `MatchCorrected` y auditoría de cada cambio del admin (RNF-04); y las alineaciones en el módulo `lineup` (RF-24–29, `docs/06-lineups-plan.md`): una alineación de trabajo versionada por manager que se arrastra sola, validada por tamaño, formación, presupuesto y límite de cambios, bloqueada por el job `LockDueLineupsJob` (o al cerrar el partido) con los valores vigentes, desbloqueada si el partido se aplaza, publicada en `LineupsLocked` y visible para todos los miembros solo una vez bloqueada. El cálculo de puntos (RF-15, RF-31) y el recálculo tras una corrección llegan con `scoring`. Los tests de integración de lo implementado están completos según `docs/07-integration-tests-plan.md` (infraestructura compartida, escaneos de autorización y errores, concurrencia, reintento de eventos, sesión, zona horaria, sentencias por pantalla y flujo de una jornada); su §4.3 fija los contratos que deben cumplir los módulos que faltan. Aún no hay `frontend/`. Mantén actualizada la sección *Comandos* de este archivo.

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21+, Spring Boot 3, Spring Modulith (monolito modular) |
| Base de datos | PostgreSQL + Flyway |
| Autenticación | Spring Security + OAuth2 login (solo Google), sesión en Spring Session JDBC |
| Jobs | `@Scheduled` + ShedLock |
| Frontend | Angular + `@angular/pwa`, mobile first (Angular Material o PrimeNG, por decidir) |
| Notificaciones | webpush-java + SMTP (Brevo, Resend…) |
| Imágenes | S3 + CloudFront (AWS SDK v2, subida por URL prefirmada); SeaweedFS en local y en tests |
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
- La tabla es un dato por liga, sembrado desde `league.defaults.scoring-rules` de `application.yml` con estos valores; nunca constantes en código.

### Puntos

- Puntos del jugador en la jornada = puntos por acciones + puntos por valoración. **Sin porcentajes ni pesos** entre ambas partes.
- Puntos del manager = suma de los puntos de sus jugadores alineados en la alineación **bloqueada**.
- Si un jugador no recibe valoraciones, o menos que el mínimo de votos de la liga (por defecto 1): solo puntos por acciones (o la nota por defecto de la liga, si está configurada). Sin valoraciones son 0 puntos, no los −2 de 0 picas.

### Valoraciones

- Se abren al cerrar el partido; ventana por defecto de 24 h (configurable).
- Solo se valora a jugadores que jugaron. Valorar es opcional.
- Formato por liga: picas 0–4 (por defecto) o nota decimal 0–10.
- Conversión de la media a puntos, por defecto: picas 0 = −2, 1 = 2, 2 = 6, 3 = 10, 4 = 14 (`4 × media − 2`); nota decimal, 1 punto por punto de nota. Se convierte la media (no cada voto) y el resultado se redondea al entero, mitad hacia arriba. La conversión es un parámetro por liga sembrado desde `league.defaults`; nunca constantes en código.
- Editables mientras la ventana siga abierta.
- **Anónimas**: ninguna API devuelve valoraciones individuales de otros usuarios; solo la media.

### Alineaciones

- N jugadores (por defecto 7), formación por posiciones opcional: mínimo de jugadores por posición configurable por liga (por defecto 1 portero y 1 por línea).
- Varios managers pueden alinear al mismo jugador (sin exclusividad). En el futuro será un parámetro de la liga.
- Editable hasta X horas antes del partido (por defecto 2 h); máximo Y cambios por jornada (por defecto 2).
- Si el manager no la toca, se arrastra la de la jornada anterior.
- Presupuesto máximo de la alineación según el valor vigente de los jugadores (activado por defecto, 100 M; el admin puede desactivarlo). Se comprueba al guardar y al bloquear; si al bloquear lo supera, esa jornada no puntúa.
- Los cambios se cuentan por diferencia final contra la alineación bloqueada del partido anterior; sustituir a un jugador dado de baja es gratis.

### Valor de mercado

- Tras cerrar cada jornada: `Δvalor = k × (puntos − media de la jornada) × valor`, con `k = 0,05` por defecto, tope de ±10 % por jornada y valor mínimo.
- Guarda el histórico de valores (se pinta en una gráfica).

Todos los parámetros configurables (ventana, formato, bloqueo, cambios, tamaño, k, tope…) se guardan **por liga**, y una liga nueva nace con los de `league.defaults` (ver *Valores por defecto* en *Convenciones*). Ver la tabla completa en `docs/requirements.md`.

## Invariantes técnicas

- **Plazos en el servidor.** El bloqueo de alineaciones y el cierre de valoraciones se validan siempre en backend. El cliente solo refleja el estado.
- **Jobs idempotentes.** Todo `@Scheduled` lleva ShedLock y puede ejecutarse dos veces sin duplicar efectos. Los recálculos de jornada también son idempotentes (recalcular = mismo resultado).
- **Listeners idempotentes.** Un evento de módulo puede llegar más de una vez: si su listener falla, `ResubmitIncompleteEventsJob` lo reenvía hasta que funcione, y también al arrancar. Todo listener tolera recibir dos veces el mismo evento (incluso a la vez) sin duplicar efectos ni eventos.
- **Autorización en cada endpoint** por rol y liga. Nunca confíes en que el frontend oculte botones.
- **Fechas:** `Instant`/`timestamptz` en UTC en backend y base de datos; conversión a `Europe/Madrid` solo en la interfaz.
- **Auditoría:** los cambios del administrador en partidos y estadísticas quedan registrados para poder recalcular (RNF-04).
- **Sin estado en disco.** Configuración por variables de entorno; nada de secretos en el repo.
- **Seguridad:** HTTPS, CSRF activo, contraseñas con bcrypt/argon2 si hay login con email.
- **RGPD:** mínimos datos personales; el borrado de cuenta debe ser posible.
- **Rendimiento:** < 300 ms en pantallas principales con ~50 usuarios concurrentes. Evita N+1.

## Convenciones

- **Valores por defecto y límites en configuración, nunca en código.** Todo valor por defecto y todo límite de negocio (longitudes máximas, máximos de goles y estadísticas, rangos de dorsal, tope de horas de bloqueo, tamaño máximo de foto…), los que ya existen y los que lleguen con nuevos módulos o parámetros, se hace así:
  - Se declara en `application.yml` bajo `<módulo>.defaults` o `<módulo>.limits` (por ejemplo `league.defaults`, `match.limits`), con un comentario si no es obvio. Así se puede sobrescribir como cualquier propiedad (variable de entorno, perfil…).
  - Se enlaza con un record `@ConfigurationProperties("<módulo>.defaults|limits")` en `<módulo>/adapter/out/config/` y se convierte a objetos de dominio (`LeagueSettings`, `ScoringRules`, `<Módulo>Limits`).
  - El resto del módulo lo lee solo a través de un puerto `<Módulo>DefaultsRepository` o `<Módulo>LimitsRepository` en `application/port/out/` (por ejemplo `LeagueDefaultsRepository`, `MatchLimitsRepository`), implementado por `Properties<Módulo>DefaultsRepository` o `Properties<Módulo>LimitsRepository`. Otros módulos no lo leen: piden lo que necesiten por la API pública del módulo dueño.
  - El adaptador construye los objetos de dominio al arrancar, así que un valor inválido impide arrancar. Los valores por defecto también se comprueban contra los límites.
  - Los límites se comprueban en el dominio en **cada escritura** (el handler pasa el `<Módulo>Limits` al aggregate: `Match.schedule(..., limits)`), **nunca al cargar** lo ya guardado: bajar un límite no rompe los datos existentes. No van en anotaciones `@Size`/`@Max` de los DTO web, que exigen constantes.
  - La BD **no repite los límites de negocio**: solo guarda reglas estructurales (no negativo, enums, `NOT NULL`, unicidad) y columnas sin longitud fija (`VARCHAR` sin tamaño). Así se puede subir un límite sin migración.
  - Siguen siendo constantes solo las de **formato o almacenamiento**, que no tiene sentido configurar: rango del tipo de columna (`Short.MAX_VALUE` en un `SMALLINT`), precisión de `NUMERIC(10, 2)`, longitud de la clave de foto, tamaño del token de invitación, definición de cada formato (picas 0–4, nota 0–10), porcentajes ≤ 100. Los límites configurables se validan al arrancar contra ellas.
  - Nada de métodos `defaults()` estáticos ni constantes de valores por defecto o límites de negocio en el dominio o los handlers.
  - En tests unitarios usa `TestLeagueDefaults`, `TestLeagueLimits`, `TestSquadLimits`, `TestMatchLimits` (o su equivalente en un módulo nuevo, con `TestApplicationYaml`), que leen el `application.yml` real; en tests de Spring, inyecta el repositorio. No copies los valores en los tests salvo para comprobar que coinciden con los requisitos.
  - Las migraciones no leen esta configuración: un `DEFAULT` de columna solo rellena filas ya existentes.
  - La configuración técnica de un adaptador (por ejemplo `squad.photos.s3.upload-ttl`) va en su propio record de properties del adaptador, sin repositorio.
- Los mappers (entidad ↔ dominio, DTO ↔ dominio) son clases con `@lombok.experimental.UtilityClass`, con métodos sin `static` explícito. Una interfaz o clase por archivo, también los repositorios Spring Data.
- Casos de uso (commands y queries) en `application/usecases/<caso>/`, un paquete por caso de uso con su `Command`/`Query` y su `Handler`.
- Los commands terminan en `Command` y las queries en `Query` (`RegisterOrLoginUserCommand`, `ListMyLeaguesQuery`); el bus único `CommandBus` expone `dispatch` (commands) y `ask` (queries).
- Código, identificadores, nombres de tablas y commits en **inglés**; textos de la interfaz en **español**.
- Migraciones Flyway versionadas (`V<n>__descripcion.sql`). **Nunca edites una migración ya aplicada**: crea una nueva.
- Referencia los requisitos por su código (`RF-xx`, `RNF-xx`) en commits, PRs y comentarios cuando aplique.
- Commits en formato Conventional Commits (`feat:`, `fix:`, `chore:`…).
- **Marca el progreso al terminar cada tarea**, en el mismo cambio que la completa:
  - En el plan (`docs/NN-*-plan.md`), marca la fila de la tarea en la tabla *Orden de ejecución* con ✅ en una columna `Hecho` (añádela si no existe).
  - En `docs/requirements.md`, cambia `- [ ]` por `- [x]` en el ítem del roadmap cuando esté completo del todo (si queda una parte para otra fase o módulo, indícalo en el propio ítem), y marca las *Decisiones abiertas* que se vayan cerrando.
  - Actualiza *Estado actual* de este archivo cuando cierres un ítem del roadmap.
- **No hagas commits (ni push) salvo que el usuario lo pida explícitamente.** Deja los cambios en el árbol de trabajo para que los revise.
- Frontend mobile first: diseña primero para pantalla de móvil.

## Tests

Obligatorios (RNF-11) para:

- Cálculo de puntos por acción según posición, incluidos los casos especiales (penalti, doble amarilla).
- Conversión de valoraciones a puntos y caso sin valoraciones.
- Plazos: bloqueo de alineación, límite de cambios, apertura/cierre de la ventana de valoración (usa un `Clock` inyectable, nunca `Instant.now()` directo).
- Algoritmo de valor de mercado: tope ±10 % y valor mínimo.
- Idempotencia de recálculos y jobs.

Usa Testcontainers con PostgreSQL para tests de integración, no H2. La infraestructura y los escaneos vienen del plan 07 (`docs/07-integration-tests-plan.md`); todo en `backend/src/test/java/com/buddyfantasy/`:

- **Una sola BD.** Todo `@SpringBootTest` importa `PostgresTestConfig`: un Postgres estático compartido por todos los contextos, que además trae `TestFixtures` y `RecordedEvents`. Reutiliza contextos: copia las anotaciones e `@Import` de una clase existente (por ejemplo `@AutoConfigureMockMvc` + `@Import({PostgresTestConfig.class, MutableClock.Config.class})`, el de `LineupEndpointsTests`) y no añadas `properties` ni `@MockitoBean` sin necesidad: cada combinación nueva es otro contexto y otro pool. La configuración de test está en `src/test/resources/config/application.yml` (los jobs, con cron `-`, se ejecutan a mano).
- **Limpieza automática.** `DatabaseCleaner` (un `TestExecutionListener`) espera a los listeners pendientes y trunca todas las tablas antes de cada test; nunca escribas `delete from` en un test. Si un listener de un test anterior no termina, falla el siguiente.
- **Ayudas:** `MutableClock` (el reloj del servidor, que el test mueve a mano), `Listeners.await(jdbc)` (espera a los listeners asíncronos antes de comprobar sus efectos), `TestUsers.as(userId)` (MockMvc con sesión), `TestFixtures` (prepara datos por los casos de uso reales: usuarios, liga, miembros, plantilla, calendario, acta, parámetros), `RecordedEvents` (eventos que publican los listeners asíncronos; `@RecordApplicationEvents` solo vale para los del hilo del test), `Concurrently` (carreras con un *latch*), `TableDigests` (comprueba que nada se escribió) y `SqlStatements.during(...)` (cuenta todas las sentencias SQL del hilo del test).
- **Escaneos que obligan a los endpoints nuevos:** `ApiSecurityTests` (sesión y CSRF), `LeagueAuthorizationTests` (todo endpoint `/api/leagues/{id}/**` debe estar en su tabla `rules()` con su rol mínimo y un cuerpo válido, o el test falla) y `ApiErrorsTests` (`400` con `ProblemDetail` ante cuerpos inválidos, nunca `5xx`). `SchedulingArchitectureTests` exige `@SchedulerLock` en todo `@Scheduled`.
- **Migraciones:** una migración nueva añade su línea a `src/test/resources/db/migration-checksums.txt` (el fallo de `MigrationChecksumsTests` la imprime); editar una aplicada lo hace fallar.
- **Pantallas y flujo:** una pantalla principal nueva entra en `ScreenStatementsTests` (mismas sentencias con una liga pequeña y con una temporada: sin N+1), y un módulo nuevo amplía `MatchdayFlowTests` (la jornada completa por HTTP).
- **Zona horaria:** la suite corre con la JVM y Postgres en `Europe/Madrid` (`argLine` del `pom.xml`, `PostgresTestConfig`), para que una conversión implícita rompa tests; `TimeZoneTests` falla si deja de ser así (por ejemplo, desde el IDE sin el `argLine` de Maven).
- **Comprueba que un test nuevo falla** quitando a mano la defensa que prueba (y restáurala) antes de darlo por bueno.

## Comandos

Desde la raíz del repo:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d   # Postgres (5432) y SeaweedFS para fotos (S3 en 8333; crea el bucket)
cd backend
mvn -B verify                                           # build + todos los tests (requiere Docker para Testcontainers)
                                                        # informe de cobertura (JaCoCo, sin umbral) en target/site/jacoco/index.html
                                                        # la JVM de los tests corre en Europe/Madrid (argLine del pom.xml)
mvn test -Dtest='LineupLockTests' -Djacoco.skip=true    # una clase, sin instrumentar la cobertura
mvn test -Dtest='ModularityTests,HexagonalArchitectureTests'   # solo verificación de arquitectura (sin Docker)
S3_ENDPOINT=http://localhost:8333 AWS_ACCESS_KEY_ID=buddyfantasy AWS_SECRET_ACCESS_KEY=buddyfantasy-local \
  mvn spring-boot:run                                   # arranque local contra los servicios de arriba
```

Configuración por variables de entorno (valores por defecto en `backend/src/main/resources/application.yml`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SWAGGER_UI_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (cliente OAuth de Google Cloud; sin ellas el login no funciona, pero la app arranca), `SESSION_COOKIE_SECURE` (`true` en producción, `false` en local), los valores con los que nace una liga en `league.defaults` (por ejemplo `LEAGUE_DEFAULTS_LINEUP_SIZE`), los límites de negocio en `league.limits`, `squad.limits` y `match.limits` (por ejemplo `MATCH_LIMITS_SHEETPLAYERSMAX`), la validez de la URL de subida de fotos en `squad.photos.s3.upload-ttl`, el cron del job de bloqueo de alineaciones `LINEUP_LOCK_JOB_CRON` (cada minuto; `-` lo desactiva, como en los tests, que lo ejecutan a mano), el del job que reenvía los eventos cuyo listener falló `EVENTS_RESUBMIT_JOB_CRON` (cada 5 minutos, solo los de hace al menos `shared.events.resubmit-job.min-age`; `-` lo desactiva, como en los tests), y para las fotos `S3_BUCKET`, `S3_REGION`, `S3_ENDPOINT` (solo en local; vacía en AWS) y `PHOTOS_BASE_URL` (origen desde el que se sirven: CloudFront en producción, `http://localhost:8333/buddy-fantasy-photos` en local). Las credenciales de S3 salen de la cadena por defecto del AWS SDK (rol IAM en producción, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` en local; las de SeaweedFS están en `deploy/seaweedfs/s3.json` y solo valen en local). Redirect URI en local: `http://localhost:8080/login/oauth2/code/google`.
Docker 29+ exige Testcontainers ≥ 1.21.4 (ya fijado en `pom.xml`). Los handlers de comandos deben ser clases, no lambdas (el bus resuelve el tipo de comando por genéricos).
Lint: pendiente.

## Decisiones abiertas

No las resuelvas por tu cuenta. Si una tarea depende de alguna, pregunta o deja la opción configurable.

Ninguna por ahora.

## Roadmap

Fase 0 Fundamentos → Fase 1 Núcleo jugable → Fase 2 Valoraciones → Fase 3 Mercado y notificaciones → Fase 4 Pulido y operación → Fase 5 Cloud.
Cada fase tiene una puerta de salida; no adelantes trabajo de fases posteriores salvo que se pida. Detalle en `docs/requirements.md`.
