# 07 · Tests de integración — análisis y plan

Análisis de la cobertura de tests de integración a 27-oct-2026 (commit `d9b1136`, final de la Fase 1 sin `scoring`) contra `requirements.md`, y plan para cerrar los huecos. Cubre lo ya implementado (`identity`, `league`, `squad`, `match`, `lineup`, `shared`), la infraestructura de tests y los contratos de integración que deberán llegar con `scoring`, `rating`, `market` y `notification` (RNF-11).

## 1. Estado actual

`mvn -B verify` en local: **414 tests, todos en verde, ~46 s de reloj**, con **12 contenedores de Postgres** y 1 de SeaweedFS.

| Tipo | Clases | Cómo |
| --- | --- | --- |
| Integración de endpoints | `*EndpointsTests`, `SecurityTests`, `ApiSecurityTests` | `@SpringBootTest` + MockMvc con `oidcLogin()` y `csrf()`, Postgres 16 con Testcontainers. Los datos se preparan con el `CommandBus`. |
| Integración de casos de uso y eventos | `LineupLockTests`, `CreateLeagueTests`, `RegisterOrLoginUserTests`, `CommandBusTests`… | `CommandBus` directo, `@RecordApplicationEvents`, espera a que `event_publication` no tenga publicaciones pendientes. |
| APIs públicas de módulo | `MatchApiTests`, `SquadApiTests`, `LeagueSettingsProviderTests`, `LeagueAccessTests` | Incluyen conteo de sentencias de Hibernate (una sola consulta, sin N+1). |
| Esquema | `*MigrationTests` | Restricciones SQL con `JdbcTemplate` (unicidad, `CHECK`, cascadas, columnas sin longitud fija). |
| Configuración | `LeagueDefaultsTests`, `ConfiguredLimitsTests` | Valores por defecto y límites sobrescritos por propiedades. |
| Adaptador externo | `S3ImageStorageTests` | SeaweedFS en Testcontainers: URL prefirmada, tipo, tamaño, `Cache-Control`, borrado idempotente. |
| Unitarios y arquitectura | `domain/*Tests`, `ModularityTests`, `HexagonalArchitectureTests`, `DataDefinitionArchitectureTests` | Sin Spring. |

Lo que ya está bien resuelto y conviene mantener:

- **Escaneo automático de endpoints** (`ApiSecurityTests`): cualquier endpoint `/api/**` nuevo exige sesión (`401`) y, si muta, CSRF (`403`) sin escribir nada.
- **Reloj controlable** (`MutableClock`) para plazos de alineación, sin `Instant.now()`.
- **Idempotencia** del job de bloqueo y del cierre de partido (`runningTheJobTwiceLocksAndPublishesOnce`, `closingTwiceKeepsTheFirstCloseAndPublishesOnce`).
- **Aislamiento entre ligas** y **roles por liga** probados en cada recurso.
- **Auditoría** (RNF-04) con antes/después y conservación tras borrar el partido.

## 2. Cobertura por requisito

✅ cubierto · 🟡 parcial · ❌ sin test · — no implementado todavía

| Requisito | Estado | Tests | Hueco |
| --- | --- | --- | --- |
| RF-01 Login con Google | ✅ | `GoogleOidcUserServiceTests`, `RegisterOrLoginUserTests`, `SecurityTests`, `SessionTests` | |
| RF-02 Perfil | 🟡 | `MeEndpointsTests` (lectura) | Edición de nombre y avatar no implementada. |
| RF-03 Recuperar contraseña | — | | No aplica: solo Google. |
| RF-04 Cierre de sesión | 🟡 | `SecurityTests`, `SessionTests` | Gestión de sesiones activas no implementada. |
| RF-05 Crear liga | ✅ | `CreateLeagueTests` (incluido rollback) | |
| RF-06 Invitación | ✅ | `InvitationEndpointsTests`, `ConcurrencyTests` | |
| RF-07 Parámetros | ✅ | `LeagueSettingsEndpointsTests`, `LeagueDefaultsTests`, `ConfiguredLimitsTests`, `LineupSeasonChangesTests` (con la temporada en marcha) | |
| RF-08 Varias ligas | ✅ | `MeEndpointsTests`, `MembersEndpointsTests`, `LeagueAccessTests` | |
| RF-09, RF-10 Jugadores y valor | ✅ | `PlayersEndpointsTests`, `PlayerPhotoEndpointsTests`, `SquadApiTests`, `ConcurrencyTests` | |
| RF-11 Ficha con historial | 🟡 | `PlayersEndpointsTests` (ficha) | Historial de puntos y valor llega con `scoring` y `market`. |
| RF-12 Calendario | ✅ | `MatchesEndpointsTests`, `ConcurrencyTests` | |
| RF-13, RF-14 Acta | ✅ | `MatchSheetEndpointsTests` | |
| RF-15 Puntos por acción | 🟡 | `LeagueSettingsEndpointsTests` (tabla guardada) | El cálculo llega con `scoring`. |
| RF-16 Corrección | 🟡 | `MatchSheetEndpointsTests` (`MatchCorrected`) | Recálculo pendiente de `scoring` y `rating`. |
| RF-17 – RF-23 Valoraciones | — | | Ver §4.3. |
| RF-24 – RF-29 Alineaciones | ✅ | `LineupEndpointsTests`, `LineupLockTests`, `LineupSeasonChangesTests`, `ConcurrencyTests` | |
| RF-30 – RF-40 | — | | Ver §4.3. |
| RNF-02 Autorización y CSRF | ✅ | `ApiSecurityTests` (sesión y CSRF), `LeagueAuthorizationTests` (rol y liga), `SecurityTests` (fuera de `/api`), `SessionTests` (cookies) | |
| RNF-03 Plazos y jobs | ✅ | `LineupLockTests`, `SchedulerLockTests`, `SchedulingArchitectureTests`, `EventResubmissionTests` | |
| RNF-04 Auditoría | ✅ | `MatchAuditEndpointsTests`, `MatchMigrationTests` | |
| RNF-05 Zona horaria | ✅ | `TimeZoneTests`; toda la suite con la JVM y Postgres en `Europe/Madrid` | |
| RNF-09 Rendimiento | ✅ | `ScreenStatementsTests` (todas las sentencias de las pantallas principales, con volumen de temporada), y conteo de Hibernate en `MatchApiTests`, `SquadApiTests`, `LeagueSettingsProviderTests`, `MatchRepositoryTests` | Sin aserciones de tiempo, a propósito. |
| RNF-10 RGPD | — | | Borrado de cuenta no implementado. |
| RNF-11 Calidad | 🟡 | Plazos de alineación ✅ | Puntuación, valoración y mercado llegan con sus módulos. Sin medida de cobertura. |
| Contrato de errores | ✅ | `ApiErrorsTests`, `ApiExceptionHandlerTests` | |
| Migraciones | ✅ | `*MigrationTests`, `MigrationChecksumsTests` | |

## 3. Problemas de la infraestructura de tests

1. **Un Postgres por contexto.** `PostgresTestConfig` declara el contenedor como `@Bean`, así que cada combinación distinta de `@Import`/`properties` arranca el suyo: 12 por ejecución. Cada módulo nuevo con su `@Import` propio añadirá más.
2. **Limpieza manual y frágil.** 22 clases borran tablas con listas de `delete from` escritas a mano en orden de claves foráneas (14 de ellas, todas las tablas). Cada tabla nueva obliga a tocarlas todas, y olvidar una deja datos que contaminan a otros tests del mismo contexto.
3. **Listeners asíncronos sin esperar.** Solo `LineupLockTests` espera a que `event_publication` quede vacía. En el resto (por ejemplo `MatchesEndpointsTests` al aplazar o `MatchSheetEndpointsTests` al cerrar) el listener de `lineup` sigue corriendo mientras el siguiente test borra tablas: los errores acaban en el log y no en el test, y es una fuente de tests intermitentes.
4. **Ayudas duplicadas o encerradas en un paquete.** `MutableClock` es *package-private* en `lineup` (y `rating` y `scoring` lo necesitarán); `FixedClockConfig` vive dentro de `RegisterOrLoginUserTests`; el `as(userId)` de MockMvc está copiado en 9 clases; los `setUp` repiten registrar usuarios, crear liga, unirse y dar de alta una plantilla (40–100 líneas por clase).
5. **Sin medida de cobertura.**

## 4. Plan

### 4.1 Infraestructura (primero: abarata todo lo demás)

**T1 · Un único Postgres.** Contenedor estático en `PostgresTestConfig` (arrancado una vez por JVM y expuesto con `@ServiceConnection`), compartido por todos los contextos. Flyway migra una vez; los demás contextos validan. Los tests se siguen ejecutando en serie, así que compartir BD es seguro si T2 limpia todo entre tests.

> Hecho así: el contenedor no es un *bean* (Spring Boot cierra los contenedores *bean* al cerrar su contexto, y dejaría sin BD a los contextos aún en caché), sino un estático expuesto con un `JdbcConnectionDetails`. Postgres arranca con `max_connections=300` y los tests usan `spring.datasource.hikari.minimum-idle: 1`, porque cada contexto en caché tiene su propio pool contra la misma BD.

**T2 · `DatabaseCleaner`.** Un bean de test (o extensión de JUnit) que primero espera a que no queden publicaciones pendientes en `event_publication` (lo que hoy hace `awaitListeners`) y después hace `TRUNCATE ... CASCADE` de **todas** las tablas del esquema leídas de `information_schema`, salvo `flyway_schema_history`. Incluye `event_publication`, `shedlock` y `spring_session`. Sustituye las 22 listas manuales; las tablas de módulos nuevos se limpian solas.

> Hecho así: es un `TestExecutionListener` registrado en `META-INF/spring.factories`, así que se aplica a todo test de Spring sin anotarlo. Solo trunca las tablas con filas (truncar también las vacías costaba más del doble). `shedlock` no se trunca: se liberan sus bloqueos (`lock_until = locked_at`), porque ShedLock recuerda las filas que ya ha creado y después solo las actualiza, y con la fila borrada el job no volvería a bloquear nunca. Si un listener de un test anterior no termina en 10 s, falla el test siguiente con la lista de publicaciones incompletas, y la BD se limpia igualmente.

**T3 · Ayudas compartidas** en el paquete raíz de test (`com.buddyfantasy`, públicas):
- `MutableClock` (con su `Config`) sustituye también a `FixedClockConfig`.
- `Listeners.await(jdbc)` (la espera de `event_publication`, sin el retardo inicial de 100 ms de Awaitility), que usan los tests que provocan eventos antes de comprobar sus efectos.
- `TestUsers.as(userId)` para MockMvc.

**T4 · `TestFixtures`.** Bean de test sobre el `CommandBus` para preparar datos sin repetir código: `user(sub)`, `league(admin)` → `CreatedLeague`, `join(user, league)`, `squad(league, posiciones…)`, `schedule(league, jornada, kickoff)`, `closeWithSheet(match, …)`, `settings(league, …)`. Se migran todos los `setUp`.

> Hecho así: `PostgresTestConfig` lo importa, así que está en todo test de Spring sin otro `@Import` ni contextos nuevos. `league(admin)` devuelve un `TestLeague(id, admin, invitationToken)` en lugar de `CreatedLeague`, para que el resto de métodos no pidan el admin: `join(league, users…)`, `player(league, …)`, `squad(league, posiciones…)`, `schedule(league, jornada, [rival,] kickoff)`, `sheet(league, match, …)` y `closeWithSheet(league, match, …)`, que además espera a los listeners de `MatchClosed`. `settings(league, cambio)` pasa por el caso de uso del admin con un `EditableSettings` (los parámetros vigentes como campos públicos), y `played(player, goles)` crea una línea del acta. Se quedan como estaban, a propósito, los tests de API de módulo (`LeagueAccessTests`, `LeagueSettingsProviderTests`, `MatchApiTests`, `MatchRepositoryTests`, `SquadApiTests`), que preparan datos con el repositorio del propio módulo para probarlo aislado y contar sus sentencias, y los `*MigrationTests`, que insertan por SQL para probar el esquema.

**T5 · JaCoCo, solo informe.** `jacoco-maven-plugin` en `mvn verify` (informe HTML en `backend/target/site/jacoco/`), subido como artefacto en `ci.yml`. Sin umbral que rompa el build.

> Hecho así: `jacoco-maven-plugin` 0.8.14 (`jacoco.version` en el `pom.xml`), artefacto `jacoco-report` en CI. Un `backend/lombok.config` marca el código que genera Lombok con `@lombok.Generated` para que JaCoCo no lo cuente. Cobertura de partida (27-oct-2026, 414 tests): 97,9 % de líneas, 91,1 % de ramas; por módulo, en líneas, `identity` 99,0 %, `league` 99,1 %, `squad` 97,8 %, `match` 98,5 %, `lineup` 98,1 %, `shared` 87,2 % (56,2 % de ramas, el más bajo), y el `main` de `BuddyFantasyApplication` sin cubrir.

Se mantiene todo en surefire con `mvn verify` (sin separar unitarios e integración, decidido).

### 4.2 Huecos de lo ya implementado

**G1 · Escaneo de autorización por liga (RNF-02).** Como `ApiSecurityTests`, pero con sesión y CSRF válidos sobre una liga real: para cada endpoint `/api/leagues/{id}/**`,
- un usuario **ajeno** a la liga recibe `403`;
- un **manager** recibe `403` en los endpoints de administrador.

El test lleva una tabla explícita *endpoint → rol mínimo → cuerpo de ejemplo válido* (el cuerpo hace falta para que no responda `400` antes de comprobar permisos). **Si el escaneo encuentra un endpoint que no está en la tabla, falla**: así ningún endpoint nuevo entra sin clasificar.

> Hecho así: `LeagueAuthorizationTests`, con los 27 endpoints de liga en `rules()` (`LeagueRole.MANAGER` = cualquier miembro, `ADMIN` = solo el admin) sobre una liga con un partido, un jugador y un miembro reales para las variables de la ruta. Además comprueba que las peticiones rechazadas no escriben nada: un `md5` de las filas de cada tabla antes y después (salvo `spring_session*` y el historial de Flyway). El escaneo de endpoints es ahora `ApiEndpoints`, compartido con `ApiSecurityTests`. Comprobado quitando a mano `requireAdmin` de `DeleteMatchHandler` y `requireMember` de `GetMyLineupHandler`: fallan los dos tests.

**G2 · Contrato de errores HTTP.** Con un endpoint representativo de cada tipo: JSON mal formado, cuerpo vacío, UUID inválido en la ruta, enum desconocido (`"position": "PORTERO"`), método no soportado → `400`/`405` con `ProblemDetail`; un fallo inesperado (un handler de test que lanza `IllegalStateException`) → `500` sin mensaje interno ni traza. Además, un escaneo: ningún endpoint que muta responde `500` a un cuerpo vacío.

> Hecho así: `ApiErrorsTests`, con un controlador de test que lanza `IllegalStateException` (solo en su contexto). `ApiExceptionHandler` añade un manejador genérico de `Exception` → `500` con `detail: "Internal error"` (el error completo solo va al log) y, para que este no se lo trague, mapea `AccessDeniedException` de los handlers a `403` con `ProblemDetail`. Los dos escaneos recorren los endpoints de la app: todo endpoint con `@RequestBody` (`ApiEndpoints.withBody`) responde `400` con `ProblemDetail` a un cuerpo ausente, vacío, `{}`, `null`, `[]` o ilegible, y ningún endpoint que muta responde `5xx` sin cuerpo. No destapó ningún `500`: el `CommandBus` valida también los comandos. Comprobado quitando el manejador genérico y forzando un `NullPointerException` en `PlayersController`: fallan los tests.

**G3 · Concurrencia con Postgres real.** Con `ExecutorService` y `CountDownLatch`, comprobando el estado final (nunca tiempos), repetido varias veces:
- Dos altas simultáneas con el mismo dorsal → una `201` y una `409`, nunca `500`.
- Dos partidos simultáneos con la misma jornada → una `201` y una `409`.
- Dos uniones simultáneas con el mismo enlace del mismo usuario → una sola membership.
- El job de bloqueo y el listener de `MatchClosed` a la vez sobre el mismo partido → un bloqueo y un solo `LineupsLocked`.

> Hecho así: `ConcurrencyTests`, con la ayuda `Concurrently` (un hilo por tarea y un *latch* que las suelta a la vez) y 10 rondas por caso. Destapó un `500`: dos uniones simultáneas del mismo usuario pasaban las dos la comprobación previa y la segunda chocaba con `league_membership_unique`. Ahora `JoinLeagueHandler` usa `MembershipRepository.saveIfAbsent` (`insert ... on conflict do nothing`, como `lineup`), y las uniones repetidas responden todas `200` con una sola membership. El dorsal y la jornada ya se traducían a `409` y el bloqueo ya era idempotente. En la carrera del bloqueo compiten el job, el cierre (cuyo listener de `MatchClosed` también bloquea) y un `LockMatchLineupsCommand` directo. Comprobado quitando cada defensa (la traducción del índice del dorsal y de la jornada, y los `on conflict do nothing` de la membership y del bloqueo): cada caso falla ya en la primera ronda.
>
> Los eventos de los listeners asíncronos se graban ahora con `RecordedEvents` (un `@EventListener` de test en todos los contextos, que `DatabaseCleaner` vacía), no con `@RecordApplicationEvents`: ver §6. Lo usan `ConcurrencyTests`, `EventResubmissionTests`, `SchedulerLockTests` y `LineupLockTests`.

**G4 · ShedLock (RNF-03).** Tras ejecutar `LockDueLineupsJob` existe la fila `LockDueLineupsJob` en `shedlock`; con esa fila ocupada (`lock_until` en el futuro, insertada a mano) una ejecución no bloquea nada aunque haya partidos vencidos. Un test de arquitectura (ArchUnit): todo método `@Scheduled` lleva `@SchedulerLock`.

> Hecho así: `SchedulerLockTests` (en el mismo contexto que `LineupLockTests`) comprueba que cada job deja su fila en `shedlock` y que, con la fila de `LockDueLineupsJob` tomada por otra instancia, el job no bloquea un partido vencido (y sí al liberarla). La regla de ArchUnit está en `SchedulingArchitectureTests` y exige además que el método sea público, porque ShedLock bloquea a través de un proxy. Las columnas de `shedlock` son `timestamp` sin zona en UTC (ShedLock escribe `timezone('utc', CURRENT_TIMESTAMP)`): al escribirlas a mano hay que usar `timezone('utc', now())`, no `now()`, o quedan desplazadas por la zona de la sesión (Europe/Madrid en local).

**G5 · Reintento de eventos (RNF-03, decisión D1).** Incluye código de producción además de tests:
- `spring.modulith.events.republish-outstanding-events-on-restart: true`: al arrancar se reenvían las publicaciones incompletas.
- `ResubmitIncompleteEventsJob` en `shared/infrastructure`, con `@Scheduled` y `@SchedulerLock`, que llama a `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan(minAge)`. Configuración técnica en su propio record de properties (`shared.events.resubmit-job.cron` y `min-age`, por ejemplo cada 5 min y 5 min de antigüedad, para no reenviar lo que aún se está procesando); `EVENTS_RESUBMIT_JOB_CRON` en el entorno y `-` en los tests, que lo ejecutan a mano.
- Tests: un listener de test que falla la primera vez deja la publicación incompleta; al ejecutar el job con la publicación ya antigua se completa y el efecto aparece una sola vez; con la publicación reciente no se reenvía; reenviar un evento de `lineup` ya procesado (`MatchClosed`, `MatchRescheduled`, `MatchDeleted`) no duplica nada.
- Regla para los módulos nuevos (en `AGENTS.md`): **todo listener es idempotente**, porque un evento puede llegar más de una vez.

> Hecho así: `ResubmitIncompleteEventsJob` en `shared/infrastructure`, con `ResubmitIncompleteEventsJobProperties` (`min-age`, 5 min) y el cron en `shared.events.resubmit-job.cron` (`EVENTS_RESUBMIT_JOB_CRON`, cada 5 min). Spring Modulith fecha las publicaciones con el `Clock` del contexto, así que los tests las envejecen con `MutableClock`. En los tests el reenvío al arrancar está apagado: los contextos comparten la BD y un contexto nuevo no es un reinicio. `EventResubmissionTests` usa un listener de test que falla las primeras veces: la publicación queda incompleta, el job la reenvía cuando tiene `min-age` y no antes, se reintenta en cada pasada mientras falle, y el efecto aparece una sola vez. Para `MatchClosed`, `MatchRescheduled` y `MatchDeleted` marca su publicación como incompleta y la reenvía: ninguna tabla cambia (`TableDigests`, extraído de `LeagueAuthorizationTests`) y no se publica ningún `LineupsLocked` ni `LineupsUnlocked` más. Comprobado quitando `@SchedulerLock` a `LockDueLineupsJob` y reenviando con `min-age` cero: fallan los tests. La regla de listeners idempotentes y `EVENTS_RESUBMIT_JOB_CRON` ya están en `AGENTS.md`.

**G6 · Sesión y cookies (RF-01, RF-04).** Con el servidor real (`webEnvironment = RANDOM_PORT`): un login simulado crea la fila en `spring_session`; `POST /api/logout` la borra y caduca la cookie `SESSION`; la cookie es `HttpOnly` y `SameSite=Lax`, y `Secure` con `SESSION_COOKIE_SECURE=true`. Fuera de `/api`: solo `/actuator/health` (y sus *probes*) es público; `/actuator/modulith`, `/actuator/info` y `/v3/api-docs` exigen sesión; Swagger UI está apagado por defecto.

> La parte fuera de `/api` ya está hecha (paso 4), en `SecurityTests`: recorre los endpoints de actuator expuestos (`PathMappedEndpoints`), así que uno expuesto más adelante queda cubierto sin listarlo. Sin sesión responden `401` a una petición que no es de navegador (un navegador va al login de Google).
>
> El resto, hecho así: `SessionTests`, con el servidor real y un cliente HTTP del JDK que guarda sus cookies y no sigue redirecciones. El login es el de verdad salvo la pantalla de Google: `FakeGoogle` (un `HttpServer` del JDK) hace de endpoints de token, JWKS y *userinfo*, y firma el ID token con una clave de test y el `nonce` de la petición de autorización, así que pasan el filtro de OAuth2, la validación del ID token y `GoogleOidcUserService`. Comprueba que empezar el login guarda la sesión en `spring_session`; que al volver con el código se registra el usuario, la sesión queda autenticada con su `principal_name` y `/api/me` responde; que una cuenta sin email verificado no entra; que `POST /api/logout` (con el token CSRF como lo manda la SPA) borra la fila, caduca la cookie (`Max-Age=0`) y la cookie antigua ya no abre sesión; y que `SESSION` es `HttpOnly` y `SameSite=Lax`, y `Secure` solo con `SESSION_COOKIE_SECURE=true` (en un `@Nested` con su propio contexto, que apunta al puerto de su servidor: la instancia exterior se inyecta con el contexto exterior). No destapó fallos. Ojo: surefire resume un `@Nested` en el `.txt` de su clase con el recuento solo de la clase exterior, pero el build sí falla si falla un test anidado.

**G7 · Parámetros cambiados con la temporada en marcha (RF-07 × RF-24–29).**
- Subir `lockHours` adelanta el bloqueo de un partido aún no bloqueado; bajarlo lo retrasa.
- Subir `lineupSize`: la alineación arrastrada sigue valiendo y rellenar los huecos es gratis.
- Desactivar el presupuesto antes del bloqueo: la alineación no queda `overBudget`.
- Activar la formación o cambiar el tamaño: la alineación arrastrada que ya no cumple se bloquea tal cual y `GET /lineup` avisa con `SIZE_MISMATCH` (regla del plan 06, §9).
- **Miembro expulsado (o que se va) y vuelve a unirse empieza de cero (decisión D2).** Incluye código de producción:
  - `league` expone en `LeagueAccess` desde cuándo es miembro cada uno (por ejemplo `Map<UUID, Instant> memberSince(leagueId)`; la membership se borra al salir y al volver nace con un `joinedAt` nuevo).
  - `lineup` ignora las versiones guardadas antes de ese `joinedAt`: al consultar su alineación (vacía), al bloquear (no tiene alineación hasta que guarde una nueva) y al contar cambios (sin alineación bloqueada de referencia posterior a su vuelta, la primera alineación no tiene límite).
  - Las versiones antiguas **no se borran**: las referencian las alineaciones bloqueadas de partidos pasados, que siguen siendo historia de esas jornadas.
  - Tests: Ana guarda, es expulsada y vuelve → `GET /lineup` vacío; si no guarda, el siguiente bloqueo la deja fuera; su primera alineación nueva no tiene límite de cambios aunque tuviera una alineación bloqueada antes de irse. Lo mismo si se va ella (`DELETE /members/me`).

> Hecho así: `LineupSeasonChangesTests` (mismo contexto que `LineupEndpointsTests`), por HTTP y con el job a mano. `LeagueAccess` cambia `memberIds` (solo lo usaba el bloqueo) por `memberSince(leagueId)` → `Map<UUID, Instant>` y añade `requireMemberSince(userId, leagueId)`, que comprueba la membership y devuelve su `joinedAt` en la misma consulta. En `lineup`, `LineupVersion.savedSince(memberSince)` decide qué versiones cuentan: `LineupTimeline` recibe el `joinedAt` e ignora las anteriores (alineación vacía y sin referencia para contar cambios), y el bloqueo descarta la última versión antes de `lockAt` si es anterior al `joinedAt` (entonces no hay ninguna posterior). Los casos de D2 van parametrizados con la expulsión y la salida voluntaria; comprobado haciendo que `savedSince` devuelva siempre `true`: fallan los cuatro.
>
> Destapó un fallo: `LineupTimeline` recalculaba el `lockAt` de **todos** los partidos con el `lockHours` vigente, también de los ya bloqueados. Bajar `lockHours` tras un bloqueo y antes del saque devolvía el partido bloqueado como objetivo y dejaba al manager sin alineación de referencia (cambios ilimitados); subirlo cambiaba la referencia por otra versión que no era la bloqueada. Ahora el timeline recibe el `lock_at` de los partidos ya bloqueados (`MatchLineupLockRepository.findLockAtsByLeagueId`, una consulta más en `GET` y `PUT /lineup`) y solo los bloqueos pendientes siguen el `lockHours` vigente.

**G8 · Zona horaria (RNF-05).** Ejecutar la suite con la JVM en `Europe/Madrid` (`-Duser.timezone` en el `argLine` de surefire) y Postgres con `TZ=Europe/Madrid`, para que cualquier conversión implícita rompa tests. Test de ida y vuelta por la API de un partido en el día del cambio de hora (`2026-03-29T01:30:00Z`): se guarda y se devuelve el mismo instante, con `Z`.

> Hecho así: la propiedad `argLine` del `pom.xml` es `-Duser.timezone=Europe/Madrid`; surefire la usa y el `prepare-agent` de JaCoCo le antepone su agente (también vale con `-Djacoco.skip=true`). `PostgresTestConfig` arranca Postgres con `TZ` y `-c timezone=Europe/Madrid`; la sesión JDBC toma de todos modos la zona de la JVM. `TimeZoneTests` (mismo contexto que `LineupEndpointsTests`) falla si la JVM o la sesión de Postgres dejan de estar en Madrid, para que G8 no se apague sin avisar. Por la API, con los dos cambios de hora de Madrid: un partido el 29 de marzo a las `01:30Z` y dos el 25 de octubre a las `00:30Z` y `01:30Z`, que son las 02:30 locales las dos veces. Cada uno se devuelve igual al crearlo, en su ficha y en el calendario (en orden y a una hora de distancia), Postgres guarda el mismo instante (comparado en segundos desde la época, calculados en SQL) y la auditoría lo guarda con `Z`. Un instante con desfase (`+01:00`) se devuelve en UTC. El bloqueo de alineaciones queda 2 h reales antes del saque aunque el reloj de Madrid avance 3 (`lockAt` en `GET /lineup`, en `lineup_match_lock` y en `GET /lineups/{matchId}`). No destapó fallos: la suite entera (473 tests) pasa en Madrid. Comprobado guardando `lock_at` como `LocalDateTime` en UTC: falla en Madrid y pasa con la JVM en UTC, que es lo que pasaba antes en CI.

**G9 · Rendimiento de pantallas (RNF-09).** Contador de **todas** las sentencias SQL con `datasource-proxy` (dependencia nueva de test; Hibernate no ve `JdbcTemplate`). Con volumen realista (20 miembros, 25 jugadores, 30 partidos, alineaciones bloqueadas de 20 managers), el número de sentencias es constante y no crece con N en: `GET /api/me/leagues`, `/players`, `/matches`, `/matches/{id}`, `/members`, `/lineup`, `/lineups/{matchId}`. Sin aserciones de tiempo (serían intermitentes).

> Hecho así: `datasource-proxy` 1.11.0 (`datasource-proxy.version` en el `pom.xml`). `SqlStatements.during(acción)` devuelve las sentencias que la acción ejecuta en el hilo del test, una por ejecución (un *batch* cuenta una), por JPA, `JdbcClient` o `JdbcTemplate`; las de otros hilos, como los listeners asíncronos, no cuentan. `PostgresTestConfig` registra el `BeanPostProcessor` que envuelve el `DataSource`, así que vale en todo test de Spring sin contextos nuevos. `ScreenStatementsTests` mide cada pantalla en una liga pequeña (2 miembros, 7 jugadores, 2 partidos, uno jugado con 1 línea) y otra vez con una temporada (20 miembros, 25 jugadores, 30 partidos, alineaciones bloqueadas de 20 managers, un partido con 14 líneas y dos correcciones, Ana en 4 ligas y con varias versiones de su alineación), y exige el mismo número de sentencias; el mensaje de fallo lista las sentencias de cada una. Además de las pantallas de la lista, cubre la auditoría del partido. Recuentos de partida, iguales en los dos tamaños: `me/leagues` 1, `players` 2, `matches` 2, `matches/{id}` 2, auditoría 2, `members` 3, `lineup` 6, `lineups/{matchId}` 2. No destapó ningún N+1. Comprobado pidiendo el perfil de cada miembro por separado en `ListLeagueMembersHandler`: falla con 5 sentencias frente a 23.

**G10 · Flujo completo de una jornada.** Un único test solo por HTTP y con el job a mano: el admin crea la liga, el manager se une por enlace, plantilla, calendario, ambos guardan alineación, el reloj avanza, el job bloquea, acta, cierre, alineaciones bloqueadas visibles para todos, corrección y auditoría. Es la base que ampliarán `rating`, `scoring` y `market` (§4.3).

> Hecho así: `MatchdayFlowTests` (mismo contexto que `LineupEndpointsTests`), con `MutableClock` y el job a mano. Los usuarios se registran con `TestFixtures`: el login de Google lo cubre ya `SessionTests` (G6); todo lo demás va por HTTP. El admin crea la liga, Ana ve la invitación y se une, y la liga aparece en sus ligas y en los miembros; el admin da de alta 8 jugadores y 2 partidos; los dos guardan alineación y nadie ve las alineaciones hasta el bloqueo (`409`); el job bloquea a su hora y los dos ven las dos alineaciones; Ana pasa a preparar la jornada 2 con 2 cambios; el admin graba el acta (Ana no ve el borrador) y cierra; el partido cerrado se ve con su resultado y estadísticas; una corrección cambia el resultado, la auditoría la registra con el antes y el después (solo para el admin) y las alineaciones bloqueadas no cambian. No destapó fallos. El acta y el cierre van en instantes distintos: la auditoría se ordena por `occurred_at` y, con el reloj quieto, el orden de un empate no está definido (en la realidad no coinciden).

**G11 · Migraciones inmutables.** Test que compara el checksum de cada migración aplicada (`flyway_schema_history`) con un fichero versionado `backend/src/test/resources/db/migration-checksums.txt`. Editar `V1`–`V11` hace fallar el test; una migración nueva se añade al fichero en el mismo cambio.

> Hecho así: `MigrationChecksumsTests` lee `version`, `checksum` y `script` de las migraciones aplicadas en `flyway_schema_history` y las compara con `db/migration-checksums.txt` (una línea `<versión> <checksum> <script>` por migración). Distingue tres fallos: migración editada o renombrada (muestra el checksum antiguo y el nuevo), migración borrada y migración nueva sin registrar (el mensaje imprime las líneas que hay que añadir). Los checksums de partida salen de V1–V11 tal como están, que en git no se han tocado desde su commit original. Comprobado añadiendo un comentario a `V8`: falla el test.

### 4.3 Contratos de integración de los módulos futuros

Se escriben **con cada módulo**, no ahora. Todos heredan: endpoints dentro de `ApiSecurityTests` y del escaneo de G1, limpieza automática (T2), checksums (G11), conteo de sentencias en sus pantallas (G9, `ScreenStatementsTests`) y ampliación del flujo de G10 (`MatchdayFlowTests`).

**`scoring` (RF-15, RF-16, RF-30–33)**
- `MatchClosed` + `LineupsLocked`, en cualquier orden → puntos de cada jugador y de cada manager, y `MatchdayScored` una vez.
- Tabla editada por la liga: se aplica la de esa liga, no la por defecto. Penalti +3 en cualquier posición y doble amarilla −3 en total, también de extremo a extremo.
- Alineación `overBudget` → 0 puntos; jugador dado de baja alineado → no puntúa.
- `MatchCorrected` → recálculo; recalcular dos veces da el mismo resultado y no duplica eventos.
- Clasificación por jornada y acumulada por HTTP, aislada por liga; partido aplazado o borrado.

**`rating` (RF-17–23)**
- `MatchClosed` abre la ventana; el job de cierre lleva ShedLock, usa `MutableClock` y es idempotente (`RatingWindowClosed` una vez).
- Valorar dentro de la ventana y editar; fuera de plazo se rechaza en el servidor.
- Solo jugadores que jugaron; solo miembros (el admin incluido); formato de la liga (picas 0–4 o nota 0–10).
- **Anonimato:** ningún endpoint devuelve valoraciones individuales de otro usuario; solo la media. Test explícito con dos managers.
- Sin valoraciones → nota por defecto de la liga o 0; mínimo de votos según se decida (decisión abierta de `requirements.md`).
- Corrección del partido con la ventana abierta o cerrada → recálculo.

**`market` (RF-34–36)**
- `MatchdayScored` → nuevo valor con tope ±10 % y valor mínimo, guardado en el histórico; idempotente.
- El valor nuevo es el que usa `lineup` al guardar y al bloquear la jornada siguiente (presupuesto).
- Recálculo tras una corrección.

**`notification` (RF-37–40)**
- Cada evento genera los avisos según las preferencias del usuario; desactivar un tipo lo suprime.
- Email contra un SMTP falso en Testcontainers (Mailpit o GreenMail); Web Push contra un stub HTTP (WireMock).
- Avisos programados ("la alineación se bloquea pronto", "la valoración cierra pronto") con ShedLock y sin duplicar al ejecutar dos veces.

**RGPD (RNF-10)**
- Borrar la cuenta elimina o anonimiza los datos personales y la sesión; las valoraciones siguen anónimas y las clasificaciones no se rompen.

## 5. Orden de ejecución

| # | Tarea | Salida | Hecho |
| --- | --- | --- | --- |
| 1 | T1, T2, T3: un Postgres, `DatabaseCleaner`, `MutableClock`, `Listeners.await()` y `as(...)` compartidos; migrar todas las clases | Suite en verde con 1 contenedor de Postgres y sin `delete from` a mano | ✅ |
| 2 | T4: `TestFixtures` y migración de los `setUp` | Tests más cortos, misma cobertura | ✅ |
| 3 | T5: JaCoCo en `verify` y artefacto en CI | Informe de cobertura de partida | ✅ |
| 4 | G1 y la parte de endpoints fuera de `/api` de G6: escaneo de autorización por liga y rol | RNF-02 cubierto de forma automática | ✅ |
| 5 | G2: contrato de errores | Ningún `500` por entrada mal formada | ✅ |
| 6 | G11: checksums de migraciones | Migraciones aplicadas protegidas | ✅ |
| 7 | G4 y G5: ShedLock, `ResubmitIncompleteEventsJob` y reenvío al arrancar, con sus tests | RNF-03 cubierto; ningún evento se pierde por un fallo puntual |✅ |
| 8 | G3: concurrencia | Carreras resueltas con `409` o sin duplicados | ✅ |
| 9 | G6: sesión y cookies | RF-01 y RF-04 cubiertos | ✅ |
| 10 | G7: parámetros con la temporada en marcha; `memberSince` en `LeagueAccess` y alineación desde cero al volver a la liga | RF-07 × RF-24–29; decisión D2 aplicada |✅ |
| 11 | G8: zona horaria | RNF-05 cubierto | ✅ |
| 12 | G9: sentencias por pantalla con `datasource-proxy` | RNF-09 vigilado | ✅ |
| 13 | G10: flujo completo de una jornada | Prueba de humo de la Fase 1 | ✅ |
| 14 | `AGENTS.md`: sección *Tests* (ayudas compartidas, `DatabaseCleaner`, `TestFixtures`, checksums, `SqlStatements`), *Comandos* (informe de JaCoCo, `EVENTS_RESUBMIT_JOB_CRON` y la suite en `Europe/Madrid`) e *Invariantes técnicas* (listeners idempotentes) | Convenciones documentadas | |

Cada paso deja la suite en verde. Si un test nuevo destapa un fallo (por ejemplo un `500` en G2 o G3, o una conversión de zona en G8), el arreglo va en el mismo paso.

## 6. Riesgos y notas

- **BD compartida entre contextos.** Funciona porque surefire ejecuta en serie. Si algún día se paraleliza, habrá que pasar a una BD por *fork*. T2 debe esperar a los listeners antes de vaciar tablas, o un listener de un test anterior escribirá en la BD del siguiente.
- **Tests de concurrencia intermitentes.** Se comprueba el estado final, se sincroniza con *latches* y se repite cada caso; nada de `sleep`.
- **JVM en `Europe/Madrid`** puede destapar fallos existentes: es lo que se busca (en el paso 11 no destapó ninguno). Un test lanzado desde el IDE sin el `argLine` de Maven corre en la zona del equipo, y `TimeZoneTests.theTestsRunOutsideUtc` falla si no es Madrid.
- **Dependencias nuevas de test:** `jacoco-maven-plugin`, `datasource-proxy` y, con `notification`, Mailpit/GreenMail y WireMock.
- **Reenvío de eventos con varias instancias:** el reenvío al arrancar no lleva ShedLock; si algún día hay más de una instancia, un evento puede procesarse dos veces a la vez. Por eso los listeners deben ser idempotentes también ante ejecuciones simultáneas (los de `lineup` lo son: `saveIfAbsent` y borrados idempotentes). Una publicación que falla siempre se reintenta en cada pasada del job: queda en el log como error hasta que se arregle.
- **Pasos 7 y 10 tocan código de producción** (D1 y D2), no solo tests: sus commits son `feat`/`fix`, no `test`.
- **`@RecordApplicationEvents` y listeners asíncronos.** Spring guarda los eventos grabados en un `InheritableThreadLocal`: un hilo del pool asíncrono hereda el registro del test que lo creó y sigue grabando en él. Los eventos que publica un listener de `@ApplicationModuleListener` solo llegan al test actual mientras el pool aún crea hilos nuevos (los 8 primeros), así que dependen del orden de los tests. Para esos eventos se usa `RecordedEvents`; `@RecordApplicationEvents` sigue valiendo para los que se publican en el hilo del test.
- El informe obsoleto `S3PlayerPhotoStorageTests` de `target/surefire-reports` es de una clase renombrada; desaparece con `mvn clean`.

## 7. Decisiones

Tomadas:

| Tema | Decisión |
| --- | --- |
| Alcance | Huecos de lo implementado y contratos de los módulos futuros. |
| Infraestructura | Refactor completo: un Postgres, limpieza genérica, ayudas y fixtures compartidas. |
| Separación unitarios / integración | Ninguna: todo en surefire con `mvn verify`. |
| Cobertura | JaCoCo solo como informe, sin umbral. |
| D1 · Eventos incompletos | **Se reintentan**: reenvío al arrancar y job con ShedLock que reenvía las publicaciones incompletas con cierta antigüedad. Todo listener es idempotente. Hoy un listener que falla deja su publicación incompleta para siempre; `lineup` tiene el job de bloqueo como red, pero `scoring` y `rating` dependerán solo de eventos. |
| D2 · Volver a la liga | **Empieza de cero**: quien sale de la liga (expulsado o por su cuenta) y vuelve no recupera su alineación ni arrastra límite de cambios. Se ignoran las versiones anteriores a su nuevo `joinedAt`, sin borrarlas. |

No quedan decisiones abiertas para este plan.
