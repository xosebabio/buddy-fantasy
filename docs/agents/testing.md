# Tests

## Obligatorios (RNF-11)

- Cálculo de puntos por acción según posición, incluidos los casos especiales (penalti, doble amarilla).
- Conversión de valoraciones a puntos y caso sin valoraciones.
- Plazos: bloqueo de alineación, límite de cambios, apertura/cierre de la ventana de valoración (usa un `Clock` inyectable, nunca `Instant.now()` directo).
- Algoritmo de valor de mercado: tope ±10 % y valor mínimo.
- Idempotencia de recálculos y jobs.

## Reglas generales

- Todo método `@Test` (y `@ParameterizedTest`) lleva un `@DisplayName` con una explicación breve, en inglés, de lo que prueba (por ejemplo `@DisplayName("A penalty goal is worth +3 in any position")`).
- En tests unitarios, los valores por defecto y límites salen de `TestLeagueDefaults`, `TestMatchLimits`… (ver [conventions.md](conventions.md)).
- **Comprueba que un test nuevo falla** quitando a mano la defensa que prueba (y restáurala) antes de darlo por bueno.

## Integración

Usa Testcontainers con PostgreSQL para tests de integración, no H2. La infraestructura y los escaneos vienen del plan 07 (`docs/07-integration-tests-plan.md`); todo en `backend/src/test/java/com/buddyfantasy/`:

- **Una sola BD.** Todo `@SpringBootTest` importa `PostgresTestConfig`: un Postgres estático compartido por todos los contextos, que además trae `TestFixtures` y `RecordedEvents`. Reutiliza contextos: copia las anotaciones e `@Import` de una clase existente (por ejemplo `@AutoConfigureMockMvc` + `@Import({PostgresTestConfig.class, MutableClock.Config.class})`, el de `LineupEndpointsTests`) y no añadas `properties` ni `@MockitoBean` sin necesidad: cada combinación nueva es otro contexto y otro pool. La configuración de test está en `src/test/resources/config/application.yml` (los jobs, con cron `-`, se ejecutan a mano).
- **Limpieza automática.** `DatabaseCleaner` (un `TestExecutionListener`) espera a los listeners pendientes y trunca todas las tablas antes de cada test; nunca escribas `delete from` en un test. Si un listener de un test anterior no termina, falla el siguiente.
- **Ayudas:**
  - `MutableClock`: el reloj del servidor, que el test mueve a mano; `at(instante, tarea)` lo fija solo para un hilo, para carreras entre peticiones que leen el reloj en momentos distintos.
  - `Listeners.await(jdbc)`: espera a los listeners asíncronos antes de comprobar sus efectos.
  - `TestUsers.as(userId)`: MockMvc con sesión.
  - El login y el reloj de pruebas del perfil `e2e` (`POST /api/e2e/login` y `GET/POST /api/e2e/clock`, para los E2E del front) se prueban en `E2eLoginTests` y `E2eClockTests`, los únicos contextos con ese perfil; `SessionTests` comprueba que sin él no existen. Con el perfil, los jobs corren solos cada 2 s con la hora real, así que esas clases llevan `@DirtiesContext`: en la caché, bloquearían y cerrarían los datos de las demás.
  - `with(csrf())` sustituye para siempre el repositorio CSRF del contexto cacheado por uno sin cookies: lo que dependa de la cookie `XSRF-TOKEN` se prueba contra el servidor real (`SessionTests`), no con MockMvc.
  - `TestFixtures`: prepara datos por los casos de uso reales (usuarios, liga, miembros, plantilla, calendario, acta, parámetros); `closeRatings` mueve el reloj al cierre de la ventana de valoración y ejecuta su job, sin el cual un partido no puntúa.
  - `RecordedEvents`: eventos que publican los listeners asíncronos (`@RecordApplicationEvents` solo vale para los del hilo del test).
  - `Concurrently`: carreras con un *latch*.
  - `TableDigests`: comprueba que nada se escribió.
  - `SqlStatements.during(...)`: cuenta todas las sentencias SQL del hilo del test.
- **Escaneos que obligan a los endpoints nuevos:** `OpenApiDocumentationTests` (todo endpoint en `/v3/api-docs`, con descripción, respuestas, parámetros, y descripción y ejemplo en cada campo), `ApiSecurityTests` (sesión y CSRF), `LeagueAuthorizationTests` (todo endpoint `/api/leagues/{id}/**` debe estar en su tabla `rules()` con su rol mínimo y un cuerpo válido, o el test falla) y `ApiErrorsTests` (`400` con `ProblemDetail` ante cuerpos inválidos, nunca `5xx`). `SchedulingArchitectureTests` exige `@SchedulerLock` en todo `@Scheduled`.
- **Spec versionado:** `OpenApiSpecTests` escribe `/v3/api-docs` en `api/openapi.json` y falla si era distinto. Tras cambiar un endpoint, ejecútalo (regenera el fichero) y sube el spec en el mismo cambio; CI falla si se olvidó (FE-05).
- **Migraciones:** una migración nueva añade su línea a `src/test/resources/db/migration-checksums.txt` (el fallo de `MigrationChecksumsTests` la imprime); editar una aplicada lo hace fallar.
- **Pantallas y flujo:** una pantalla principal nueva entra en `ScreenStatementsTests` (mismas sentencias con una liga pequeña y con una temporada: sin N+1), y un módulo nuevo amplía `MatchdayFlowTests` (la jornada completa por HTTP).
- **Zona horaria:** la suite corre con la JVM y Postgres en `Europe/Madrid` (`argLine` del `pom.xml`, `PostgresTestConfig`), para que una conversión implícita rompa tests; `TimeZoneTests` falla si deja de ser así (por ejemplo, desde el IDE sin el `argLine` de Maven).

## Frontend

Reglas completas en `docs/requirements.md` (FE-33, FE-34); comandos en [commands.md](commands.md) (*Frontend* y *E2E*).

- **Unitarios y de componentes** con Vitest (jsdom, builder `@angular/build:unit-test`) y Angular Testing Library, en `*.spec.ts` junto a lo que prueban. Obligatorios: cada componente de `ui/` y `shared/` (sus estados y la accesibilidad básica), cada servicio de API (con `HttpTestingController`), las funciones puras (mapeos, fechas, formatos) y los guards. Nombres de `describe`/`it` en inglés, como los `@DisplayName`.
- **Ayudas de test** en `*.testing.ts` junto a lo que prueban (fuera del build, dentro de `tsconfig.spec.json`): `core/auth/session.testing.ts` da un usuario, sus ligas, `answerSession(http, user)`, que responde a la carga de la sesión, y `loadSession(user?, leagues?)`, que la carga en el `TestBed` (después de `render`); `features/league/members.testing.ts`, los miembros de una liga. Lo que dependa de la sesión la carga antes de navegar, como la app al arrancar. Las páginas se prueban con su servicio de API real y `HttpTestingController`; una petición pendiente de un `httpResource` no deja que `whenStable` termine, así que se responde antes, y un `reload()` se lanza con `TestBed.tick()`.
- **E2E** con Playwright en `frontend/e2e/`, en viewport móvil (Chromium con Pixel 7 y WebKit con iPhone 14), en tema claro y oscuro y con movimiento reducido: cuatro proyectos (`chromium-light`…). Corren contra el backend real con Postgres en contenedor y el login de pruebas del perfil `e2e` (que no existe en producción), y por defecto contra el build servido por Caddy, como en producción (con `E2E_BASE_URL`, contra `ng serve`). Hoy hay los de humo del plan 11 (una invitación sin sesión que vuelve tras el login; crear liga, invitar, unirse, expulsar y cerrar sesión); la jornada completa (plantilla, partido y acta, alineación, valorar, cierre y clasificación) llega con las pantallas de la Fase 2.
  - `fixtures.ts`: `openBrowser` abre un navegador por persona del test y lo hace fallar ante cualquier violación de CSP o error sin capturar; `newUser` y `unique` dan cuentas y nombres nuevos en cada test, así que no se vacía la BD entre ejecuciones; `logIn` abre la sesión con el login de pruebas (con CSRF); `expectAccessible` pasa axe.
  - `pages.ts`: los pasos que repite más de un spec (crear una liga, el enlace de invitación, un toast, confirmar), con los textos que ve una persona. Los elementos se buscan por rol y nombre accesible, nunca por clase.
- **Accesibilidad:** cada pantalla de los E2E pasa `@axe-core/playwright` (WCAG 2.2 AA) sin violaciones con `expectAccessible`; así se comprueba también el contraste de la paleta en los dos temas (FE-25, RNF-12), además de `ui/styles/contrast.spec.ts`.
- **Ficheros generados:** CI regenera `api/openapi.json` (job `backend`), `core/api/schema.d.ts`, `ui/styles/_palette.scss` y `src/locale/messages.xlf` (job `frontend`) y falla si cambian (FE-05, decisiones 6 y 7 del plan 11).
- **CI** (`.github/workflows/ci.yml`): `backend` (`mvn -B verify`), `frontend` (ficheros generados al día, `lint`, `format:check`, `test` y `build`) y, tras los dos, `e2e` (Postgres y Caddy del Compose de desarrollo, backend con el perfil `e2e`; si falla, sube el informe de Playwright, las trazas y el log del backend).
