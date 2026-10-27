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
| RF-01 Login con Google | 🟡 | `GoogleOidcUserServiceTests`, `RegisterOrLoginUserTests`, `SecurityTests` | No se comprueba que el login cree la sesión en `spring_session` (Spring Session JDBC). |
| RF-02 Perfil | 🟡 | `MeEndpointsTests` (lectura) | Edición de nombre y avatar no implementada. |
| RF-03 Recuperar contraseña | — | | No aplica: solo Google. |
| RF-04 Cierre de sesión | 🟡 | `SecurityTests` (`204`) | No se comprueba que la sesión JDBC se borre ni la cookie se invalide. Gestión de sesiones activas no implementada. |
| RF-05 Crear liga | ✅ | `CreateLeagueTests` (incluido rollback) | |
| RF-06 Invitación | ✅ | `InvitationEndpointsTests` | Unión simultánea con el mismo enlace sin probar. |
| RF-07 Parámetros | 🟡 | `LeagueSettingsEndpointsTests`, `LeagueDefaultsTests`, `ConfiguredLimitsTests` | No se prueba el efecto de cambiar parámetros **con la temporada en marcha** sobre alineaciones y bloqueo. |
| RF-08 Varias ligas | ✅ | `MeEndpointsTests`, `MembersEndpointsTests`, `LeagueAccessTests` | |
| RF-09, RF-10 Jugadores y valor | ✅ | `PlayersEndpointsTests`, `PlayerPhotoEndpointsTests`, `SquadApiTests` | Carrera de dorsal duplicado (`DataIntegrityViolationException` → `409`) sin probar. |
| RF-11 Ficha con historial | 🟡 | `PlayersEndpointsTests` (ficha) | Historial de puntos y valor llega con `scoring` y `market`. |
| RF-12 Calendario | ✅ | `MatchesEndpointsTests` | Carrera de jornada u hora duplicada sin probar. |
| RF-13, RF-14 Acta | ✅ | `MatchSheetEndpointsTests` | |
| RF-15 Puntos por acción | 🟡 | `LeagueSettingsEndpointsTests` (tabla guardada) | El cálculo llega con `scoring`. |
| RF-16 Corrección | 🟡 | `MatchSheetEndpointsTests` (`MatchCorrected`) | Recálculo pendiente de `scoring` y `rating`. |
| RF-17 – RF-23 Valoraciones | — | | Ver §4.3. |
| RF-24 – RF-29 Alineaciones | ✅ | `LineupEndpointsTests`, `LineupLockTests` | Parámetros cambiados a mitad, job y cierre a la vez. Un miembro expulsado que vuelve recupera su alineación anterior; debe empezar de cero (D2, G7). |
| RF-30 – RF-40 | — | | Ver §4.3. |
| RNF-02 Autorización y CSRF | 🟡 | `ApiSecurityTests` (sesión y CSRF), un test por endpoint para rol y liga | La autorización **por rol y liga** se prueba a mano endpoint a endpoint: un endpoint nuevo puede olvidarla sin que falle nada. Cookies de sesión (`HttpOnly`, `SameSite`, `Secure`) sin test. Endpoints fuera de `/api` (`/actuator/modulith`, `/v3/api-docs`) sin test. |
| RNF-03 Plazos y jobs | 🟡 | `LineupLockTests` | Nada comprueba que **ShedLock** proteja el job. Un listener que falla deja la publicación de `event_publication` incompleta y **nadie la reintenta** (se arregla en G5, decisión D1). |
| RNF-04 Auditoría | ✅ | `MatchAuditEndpointsTests`, `MatchMigrationTests` | |
| RNF-05 Zona horaria | ❌ | | Ningún test con la JVM o Postgres fuera de UTC, ni de que el JSON devuelva instantes con `Z`. |
| RNF-09 Rendimiento | 🟡 | Conteo de sentencias en `MatchApiTests`, `SquadApiTests`, `LeagueSettingsProviderTests`, `MatchRepositoryTests` | No en los **endpoints de las pantallas principales** (partidos, jugadores, miembros, alineaciones bloqueadas) ni con volumen realista. Hibernate no cuenta las consultas de `JdbcTemplate`. |
| RNF-10 RGPD | — | | Borrado de cuenta no implementado. |
| RNF-11 Calidad | 🟡 | Plazos de alineación ✅ | Puntuación, valoración y mercado llegan con sus módulos. Sin medida de cobertura. |
| Contrato de errores | 🟡 | `ApiExceptionHandlerTests` (unitario), un caso de UUID mal formado en el cuerpo de la alineación | Sin tests de JSON mal formado, cuerpo vacío, UUID inválido en la ruta, enum desconocido ni de que un error inesperado dé `500` sin filtrar detalles. |
| Migraciones | 🟡 | `*MigrationTests` | Nada impide **editar una migración ya aplicada** (la BD de test nace vacía, así que Flyway nunca ve el checksum antiguo). |

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

Se mantiene todo en surefire con `mvn verify` (sin separar unitarios e integración, decidido).

### 4.2 Huecos de lo ya implementado

**G1 · Escaneo de autorización por liga (RNF-02).** Como `ApiSecurityTests`, pero con sesión y CSRF válidos sobre una liga real: para cada endpoint `/api/leagues/{id}/**`,
- un usuario **ajeno** a la liga recibe `403`;
- un **manager** recibe `403` en los endpoints de administrador.

El test lleva una tabla explícita *endpoint → rol mínimo → cuerpo de ejemplo válido* (el cuerpo hace falta para que no responda `400` antes de comprobar permisos). **Si el escaneo encuentra un endpoint que no está en la tabla, falla**: así ningún endpoint nuevo entra sin clasificar.

**G2 · Contrato de errores HTTP.** Con un endpoint representativo de cada tipo: JSON mal formado, cuerpo vacío, UUID inválido en la ruta, enum desconocido (`"position": "PORTERO"`), método no soportado → `400`/`405` con `ProblemDetail`; un fallo inesperado (un handler de test que lanza `IllegalStateException`) → `500` sin mensaje interno ni traza. Además, un escaneo: ningún endpoint que muta responde `500` a un cuerpo vacío.

**G3 · Concurrencia con Postgres real.** Con `ExecutorService` y `CountDownLatch`, comprobando el estado final (nunca tiempos), repetido varias veces:
- Dos altas simultáneas con el mismo dorsal → una `201` y una `409`, nunca `500`.
- Dos partidos simultáneos con la misma jornada → una `201` y una `409`.
- Dos uniones simultáneas con el mismo enlace del mismo usuario → una sola membership.
- El job de bloqueo y el listener de `MatchClosed` a la vez sobre el mismo partido → un bloqueo y un solo `LineupsLocked`.

**G4 · ShedLock (RNF-03).** Tras ejecutar `LockDueLineupsJob` existe la fila `LockDueLineupsJob` en `shedlock`; con esa fila ocupada (`lock_until` en el futuro, insertada a mano) una ejecución no bloquea nada aunque haya partidos vencidos. Un test de arquitectura (ArchUnit): todo método `@Scheduled` lleva `@SchedulerLock`.

**G5 · Reintento de eventos (RNF-03, decisión D1).** Incluye código de producción además de tests:
- `spring.modulith.events.republish-outstanding-events-on-restart: true`: al arrancar se reenvían las publicaciones incompletas.
- `ResubmitIncompleteEventsJob` en `shared/infrastructure`, con `@Scheduled` y `@SchedulerLock`, que llama a `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan(minAge)`. Configuración técnica en su propio record de properties (`shared.events.resubmit-job.cron` y `min-age`, por ejemplo cada 5 min y 5 min de antigüedad, para no reenviar lo que aún se está procesando); `EVENTS_RESUBMIT_JOB_CRON` en el entorno y `-` en los tests, que lo ejecutan a mano.
- Tests: un listener de test que falla la primera vez deja la publicación incompleta; al ejecutar el job con la publicación ya antigua se completa y el efecto aparece una sola vez; con la publicación reciente no se reenvía; reenviar un evento de `lineup` ya procesado (`MatchClosed`, `MatchRescheduled`, `MatchDeleted`) no duplica nada.
- Regla para los módulos nuevos (en `AGENTS.md`): **todo listener es idempotente**, porque un evento puede llegar más de una vez.

**G6 · Sesión y cookies (RF-01, RF-04).** Con el servidor real (`webEnvironment = RANDOM_PORT`): un login simulado crea la fila en `spring_session`; `POST /api/logout` la borra y caduca la cookie `SESSION`; la cookie es `HttpOnly` y `SameSite=Lax`, y `Secure` con `SESSION_COOKIE_SECURE=true`. Fuera de `/api`: solo `/actuator/health` (y sus *probes*) es público; `/actuator/modulith`, `/actuator/info` y `/v3/api-docs` exigen sesión; Swagger UI está apagado por defecto.

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

**G8 · Zona horaria (RNF-05).** Ejecutar la suite con la JVM en `Europe/Madrid` (`-Duser.timezone` en el `argLine` de surefire) y Postgres con `TZ=Europe/Madrid`, para que cualquier conversión implícita rompa tests. Test de ida y vuelta por la API de un partido en el día del cambio de hora (`2026-03-29T01:30:00Z`): se guarda y se devuelve el mismo instante, con `Z`.

**G9 · Rendimiento de pantallas (RNF-09).** Contador de **todas** las sentencias SQL con `datasource-proxy` (dependencia nueva de test; Hibernate no ve `JdbcTemplate`). Con volumen realista (20 miembros, 25 jugadores, 30 partidos, alineaciones bloqueadas de 20 managers), el número de sentencias es constante y no crece con N en: `GET /api/me/leagues`, `/players`, `/matches`, `/matches/{id}`, `/members`, `/lineup`, `/lineups/{matchId}`. Sin aserciones de tiempo (serían intermitentes).

**G10 · Flujo completo de una jornada.** Un único test solo por HTTP y con el job a mano: el admin crea la liga, el manager se une por enlace, plantilla, calendario, ambos guardan alineación, el reloj avanza, el job bloquea, acta, cierre, alineaciones bloqueadas visibles para todos, corrección y auditoría. Es la base que ampliarán `rating`, `scoring` y `market` (§4.3).

**G11 · Migraciones inmutables.** Test que compara el checksum de cada migración aplicada (`flyway_schema_history`) con un fichero versionado `backend/src/test/resources/db/migration-checksums.txt`. Editar `V1`–`V11` hace fallar el test; una migración nueva se añade al fichero en el mismo cambio.

### 4.3 Contratos de integración de los módulos futuros

Se escriben **con cada módulo**, no ahora. Todos heredan: endpoints dentro de `ApiSecurityTests` y del escaneo de G1, limpieza automática (T2), checksums (G11), conteo de sentencias en sus pantallas (G9) y ampliación del flujo de G10.

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
| 3 | T5: JaCoCo en `verify` y artefacto en CI | Informe de cobertura de partida | |
| 4 | G1 y la parte de endpoints fuera de `/api` de G6: escaneo de autorización por liga y rol | RNF-02 cubierto de forma automática | |
| 5 | G2: contrato de errores | Ningún `500` por entrada mal formada | |
| 6 | G11: checksums de migraciones | Migraciones aplicadas protegidas | |
| 7 | G4 y G5: ShedLock, `ResubmitIncompleteEventsJob` y reenvío al arrancar, con sus tests | RNF-03 cubierto; ningún evento se pierde por un fallo puntual | |
| 8 | G3: concurrencia | Carreras resueltas con `409` o sin duplicados | |
| 9 | G6: sesión y cookies | RF-01 y RF-04 cubiertos | |
| 10 | G7: parámetros con la temporada en marcha; `memberSince` en `LeagueAccess` y alineación desde cero al volver a la liga | RF-07 × RF-24–29; decisión D2 aplicada | |
| 11 | G8: zona horaria | RNF-05 cubierto | |
| 12 | G9: sentencias por pantalla con `datasource-proxy` | RNF-09 vigilado | |
| 13 | G10: flujo completo de una jornada | Prueba de humo de la Fase 1 | |
| 14 | `AGENTS.md`: sección *Tests* (ayudas compartidas, `DatabaseCleaner`, `TestFixtures`, checksums), *Comandos* (informe de JaCoCo y `EVENTS_RESUBMIT_JOB_CRON`) e *Invariantes técnicas* (listeners idempotentes) | Convenciones documentadas | |

Cada paso deja la suite en verde. Si un test nuevo destapa un fallo (por ejemplo un `500` en G2 o G3, o una conversión de zona en G8), el arreglo va en el mismo paso.

## 6. Riesgos y notas

- **BD compartida entre contextos.** Funciona porque surefire ejecuta en serie. Si algún día se paraleliza, habrá que pasar a una BD por *fork*. T2 debe esperar a los listeners antes de vaciar tablas, o un listener de un test anterior escribirá en la BD del siguiente.
- **Tests de concurrencia intermitentes.** Se comprueba el estado final, se sincroniza con *latches* y se repite cada caso; nada de `sleep`.
- **JVM en `Europe/Madrid`** puede destapar fallos existentes: es lo que se busca.
- **Dependencias nuevas de test:** `jacoco-maven-plugin`, `datasource-proxy` y, con `notification`, Mailpit/GreenMail y WireMock.
- **Reenvío de eventos con varias instancias:** el reenvío al arrancar no lleva ShedLock; si algún día hay más de una instancia, un evento puede procesarse dos veces a la vez. Por eso los listeners deben ser idempotentes también ante ejecuciones simultáneas (los de `lineup` lo son: `saveIfAbsent` y borrados idempotentes). Una publicación que falla siempre se reintenta en cada pasada del job: queda en el log como error hasta que se arregle.
- **Pasos 7 y 10 tocan código de producción** (D1 y D2), no solo tests: sus commits son `feat`/`fix`, no `test`.
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
