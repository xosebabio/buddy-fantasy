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
  - El login de pruebas del perfil `e2e` (`POST /api/e2e/login`, para los E2E del front) se prueba en `E2eLoginTests`, el único contexto con ese perfil; `SessionTests` comprueba que sin él no existe.
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

Reglas completas en `docs/requirements.md` (FE-33, FE-34); se montan con el esqueleto de `frontend/` (plan 11).

- **Unitarios y de componentes** con Vitest y Angular Testing Library. Obligatorios: cada componente de `ui/` y `shared/` (sus estados y la accesibilidad básica), cada servicio de API (con `HttpTestingController`), las funciones puras (mapeos, fechas, formatos) y los guards. Nombres de `describe`/`it` en inglés, como los `@DisplayName`.
- **Ayudas de test** en `*.testing.ts` junto a lo que prueban (fuera del build, dentro de `tsconfig.spec.json`): `core/auth/session.testing.ts` da un usuario, sus ligas y `answerSession(http, user)`, que responde a la carga de la sesión. Lo que dependa de la sesión la carga antes de navegar, como la app al arrancar.
- **E2E** con Playwright en viewport móvil (Chromium y WebKit), contra el backend real con Postgres en contenedor y el login de pruebas del perfil `e2e` (que no existe en producción). Cubren la jornada completa: crear liga e invitar, plantilla, partido y acta, alineación, valorar, cierre y clasificación.
- **Accesibilidad:** cada pantalla de los E2E pasa `@axe-core/playwright` sin violaciones; así se comprueba también el contraste de la paleta en los dos temas (FE-25, RNF-12).
- **Contrato:** CI regenera `api/openapi.json` y `core/api/schema.d.ts` y falla si cambian (FE-05).
