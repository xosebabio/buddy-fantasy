# 11 · Esqueleto del frontend — plan

Plan del ítem de la **Fase 0** *Esqueleto de `frontend/`: Angular, ESLint, Prettier, Vitest, tipos del OpenAPI, tokens y tema, shell con navegación, login y CI (FE-01 a FE-35)*, del trabajo de backend que se desprende de los requisitos del frontend (plan 10, §5) y de las primeras pantallas: las de identidad y liga, que bastan para entrar, crear una liga e invitar. Las reglas son las de `docs/requirements.md` (RNF-01, RNF-12 a RNF-16, FE-01 a FE-35), resumidas en `docs/agents/frontend.md`; este plan no las repite, dice cómo y en qué orden se montan. Las pantallas de manager y de admin que faltan, y el E2E de la jornada completa, van en planes propios de la Fase 2.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Backend | Export del spec a `api/openapi.json`, `ErrorCode` publicado como enum (y códigos en los `409` y `400` que hoy no los llevan), vuelta tras el login, login de pruebas con perfil `e2e`, cabeceras de proxy y documentación del redirect URI local (§3). |
| Esqueleto | `frontend/` con Angular (última estable al empezar), estructura FE-01, TypeScript y plantillas estrictas, ESLint con `boundaries`, Prettier, Stylelint, Vitest con Angular Testing Library, `@angular/localize` con `es`, presupuestos de RNF-13 y `autoCsp` (§4, §5). |
| Contrato | Tipos generados del spec, `IdentityApi` y `LeagueApi` completos, interceptor de errores y textos de `ErrorCode` (§6). Los demás servicios de API llegan con sus pantallas. |
| Diseño | Dirección visual (FE-28), tokens, paleta, tema claro y oscuro, tipografía, los componentes de `ui/` que piden el shell y las primeras pantallas, y `/_ui` (§7). |
| Núcleo y shell | `SessionStore`, login y logout, guards, rutas, barra superior, navegación inferior y lateral, cambio de liga, aviso sin conexión, páginas de error, utilidades de fecha (§8). |
| Pantallas | Login, mis ligas y crear liga, invitación, perfil, miembros y salir de la liga, y admin · invitación y miembros (§9). |
| Servido | `deploy/Caddyfile` con estáticos, proxy, cabeceras y CSP, probado en local con un perfil de Compose (§10). |
| Calidad | Tests unitarios y de componentes (FE-33), E2E de humo en móvil con axe (FE-34) y CI del front (§11). |
| Fuera de alcance | Resto de pantallas de manager y admin, E2E de la jornada completa, Dockerfile, Compose de producción, VPS, PWA. |

## 2. Decisiones

Vienen cerradas las de `docs/10-frontend-requirements-plan.md` §2 y las reglas FE-xx. Las de esta tabla se propusieron en este plan y se revisaron con el usuario (tarea 1): todas se aceptaron tal cual salvo la 7, que pasa a versionar las traducciones.

| # | Tema | Decisión |
| --- | --- | --- |
| 1 | Primeras pantallas | Las de identidad y liga (§9). Las demás de manager y admin, en dos planes siguientes (`12` manager, `13` admin, con el E2E de la jornada). |
| 2 | Códigos de error | Todo `409` lleva `code`, y también todo `400` de dominio que un formulario pueda provocar (§3.2). Un mismo nombre significa lo mismo en todos los módulos, porque el front tiene un solo `Record<ErrorCode, string>`. |
| 3 | `/login` | Corrige la decisión 9 del plan 10 y FE-35: el proxy (Caddy y `ng serve`) manda al backend `/login/oauth2/*`, no `/login`, que es la pantalla de login de Angular. Se actualizan `requirements.md` y `frontend.md` en el mismo cambio. |
| 4 | Catálogo de `ui/` | En este plan, solo los componentes de FE-18 que usan el shell y las primeras pantallas (§7.4); los demás (select, stepper, tabs, control segmentado, cuenta atrás) con la pantalla que los pida. FE-18 no cambia: se completa a lo largo de la Fase 2. |
| 5 | Servicios de API | `IdentityApi` y `LeagueApi` completos ahora; cada uno de los demás, con su primera pantalla. |
| 6 | Paleta | Las escalas 50–900 las genera un script versionado (`frontend/scripts/palette.mjs`, en OKLCH) que escribe `ui/styles/_palette.scss`; un test de Vitest comprueba el contraste de los pares texto/fondo de los tokens en los dos temas, antes de que lo haga axe. |
| 7 | Traducciones | `src/locale/messages.xlf` se versiona, como el spec: CI ejecuta `i18n:extract` y falla si el fichero cambia o hay ids duplicados o mal formados, así que el diff de cada PR enseña los textos cambiados. Los ids explícitos los exige el lint. |
| 8 | E2E | En CI, contra el build servido por Caddy, como en producción (comprueba rutas, cabeceras y CSP); en local, también contra `ng serve` con `E2E_BASE_URL`. |
| 9 | Caddy en local | Servicio `caddy` en `deploy/docker-compose.dev.yml` con el perfil `caddy` (`http://localhost:8081`), que sirve `frontend/dist` y hace proxy al backend del host. Dockerfile, Compose de producción y VPS siguen en su ítem de la Fase 0, fuera de este plan. |
| 10 | Login de pruebas | `POST /api/e2e/login` solo con el perfil `e2e`, oculto en el spec (§3.4). |

## 3. Trabajo de backend

### 3.1 Export del spec (FE-05)

- `OpenApiSpecTests` pide `/v3/api-docs` con sesión, lo escribe en `api/openapi.json` (raíz del repo) y **falla si difería** del que había, con un mensaje que dice que se suba el regenerado. Como `MigrationChecksumsTests`: un único comando lo regenera y CI falla si se olvidó.
- Spec determinista: `springdoc.writer-with-order-by-keys` y `writer-with-default-pretty-printer`, y `servers` fijado a `/` en `OpenApiConfig` (springdoc lo deduce de la petición, y cambiaría según el puerto).

### 3.2 Códigos de error en el spec (FE-08)

- `OpenApiConfig` ya pone el esquema `Problem` en toda respuesta de error. Falta que `code` sea un **enum**: la unión de los nombres de todas las implementaciones de `ErrorCode`, encontradas por escaneo del classpath (sin que `shared` dependa de los módulos, así que Modulith sigue limpio).
- Hoy solo `rating`, `lineup` y `scoring` tienen códigos. Se añaden `MatchError`, `SquadError` y `LeagueError` para los `409` (todos) y los `400` de dominio que puede provocar un formulario. Cerrados al implementar:

  | Módulo | `409` | `400` de formulario |
  | --- | --- | --- |
  | `match` | `MATCHDAY_TAKEN`, `KICKOFF_TAKEN`, `MATCH_CLOSED` (cambiar jornada u hora, o borrar uno cerrado), `MATCH_NOT_STARTED`, `MATCH_SHEET_INCOMPLETE` | `INVALID_OPPONENT`, `INVALID_MATCHDAY`, `INVALID_GOALS`, `INVALID_STAT_COUNT`, `INVALID_CARDS`, `SHEET_TOO_LARGE`, y los de `lineup` `DUPLICATE_PLAYER`, `UNKNOWN_PLAYER` e `INACTIVE_PLAYER` con el mismo significado |
  | `squad` | `SHIRT_NUMBER_TAKEN`, `PLAYER_RETIRED` | `INVALID_PLAYER_NAME`, `INVALID_SHIRT_NUMBER`, `INVALID_INITIAL_VALUE`, `INVALID_PHOTO_FORMAT`, `INVALID_PHOTO_SIZE`, `PHOTO_NOT_UPLOADED` |
  | `league` | `ADMIN_CANNOT_LEAVE` | `INVALID_LEAGUE_NAME` y uno por parámetro: `INVALID_RATING_WINDOW_HOURS`, `INVALID_RATING_FORMAT`, `INVALID_RATING_CONVERSION`, `INVALID_RATING_MIN_VOTES`, `INVALID_DEFAULT_RATING`, `INVALID_LINEUP_LOCK_HOURS`, `INVALID_LINEUP_MAX_CHANGES`, `INVALID_LINEUP_SIZE`, `INVALID_LINEUP_MINIMUMS`, `INVALID_LINEUP_BUDGET`, `INVALID_MARKET_K`, `INVALID_MARKET_MAX_CHANGE`, `INVALID_MARKET_MIN_VALUE`, `SCORING_TABLE_INCOMPLETE` |
  | `lineup` | `LINEUPS_NOT_LOCKED` (ver las de un partido sin bloquear) y `LOCK_NOT_DUE` (bloqueo antes de hora, que solo ocurre en una carrera del job con un aplazamiento y no llega al API) | — |

- `ConflictException` exige el código (todo `409` lo lleva). Un campo obligatorio que falta sigue respondiendo `400` con `violations` y sin código (lo rechaza la validación del DTO): por eso un resultado a medias en el acta no tiene código propio. Tampoco lo tiene lo que la interfaz nunca envía (una clave de foto de otro jugador, una lista nula).

- Un test comprueba que el enum del spec contiene todos los códigos (`OpenApiDocumentationTests`, contra los enums importados con ArchUnit), y `ApiErrorsTests` se amplía para que los `409` lleven siempre `code`; los tests de endpoints de cada módulo comprueban el `code` de sus `400` y `409`.
- Los límites no se exponen en el API (FE-09): el formulario no valida antes de enviar; el `code` del `400` dice qué campo falla.

### 3.3 Vuelta tras el login

- `oauth2Login` con `loginPage("/login")` (la de Angular; Spring deja de generar la suya), `defaultSuccessUrl("/", true)` y `failureUrl("/login?error")`. El front recupera la ruta guardada en `sessionStorage` (FE-10). `/login` es pública: sin front, el backend responde `404` en vez de redirigirse a sí mismo en bucle.
- `server.forward-headers-strategy: framework`, para que detrás de Caddy el redirect URI salga en `https` y con el dominio.
- En local, el redirect URI de Google pasa a `http://localhost:4200/login/oauth2/code/google`: el proxy de `ng serve` conserva la cabecera `Host` (sin `changeOrigin`), así que Spring lo construye con el origen del front. Se documenta en `commands.md` junto al de `8080`, que sigue valiendo para probar el backend solo.
- Tests en `SessionTests`: tras el login, redirección a `/` (también si antes se pidió otra página); con error, a `/login?error`; `/login` en el backend es `404`; `/api/**` sigue respondiendo `401` sin redirigir a un navegador; con `X-Forwarded-Proto` y `X-Forwarded-Host`, el redirect URI lleva el origen público.

### 3.4 Login de pruebas para E2E (FE-34)

- `E2eLoginController` en `identity/adapter/in/web`, con `@Profile("e2e")` y `@Hidden`: `POST /api/e2e/login` con `email` y `displayName` registra o reutiliza al usuario con `RegisterOrLoginUserCommand` (sujeto `e2e:<email>`) y guarda en la sesión un `CurrentUser`, el mismo principal que el login de Google. Pasa por CSRF como cualquier `POST`.
- Tests: con el perfil, abre sesión y `GET /api/me` devuelve al usuario; sin él (el contexto de la suite y el de producción) el bean no existe y la ruta responde `401`/`404`. Al arrancar con el perfil se registra un `WARN`.
- Deja sitio para lo que necesitará el E2E de la jornada completa (mover el reloj del servidor y ejecutar los jobs), que no se hace aquí.

### 3.5 CORS del bucket

- En local ya está: `deploy/seaweedfs/cors.json` admite `PUT` y `GET` desde `http://localhost:4200`. Se añade `http://localhost:8081` (Caddy en local).
- En producción es configuración del bucket de S3 (origen del dominio, `PUT` y `GET`, cabeceras `Content-Type` y `Cache-Control`): se documenta en `commands.md` hasta que exista el despliegue.

## 4. Estructura de `frontend/`

```
frontend/
  .nvmrc  package.json  package-lock.json  angular.json  tsconfig*.json
  eslint.config.js  .prettierrc  .stylelintrc.json  proxy.conf.json  playwright.config.ts
  scripts/palette.mjs
  e2e/                          # Playwright: fixtures (login de pruebas), páginas y specs
  src/
    index.html                  # script de tema antes del primer pintado (FE-26)
    locale/messages.xlf         # textos extraídos, versionado (decisión 7)
    main.ts  styles.scss
    app/
      app.config.ts  app.routes.ts
      core/
        api/                    # schema.d.ts generado, identity/, league/ (alias y servicio), path tipado
        auth/                   # SessionStore, login/logout, ruta de vuelta, guards
        http/                   # interceptor de errores, Problem y su parser, textos de ErrorCode
        layout/                 # shell, barra superior, navegación, cambio de liga, aviso sin conexión
        i18n/  time/            # locale es-ES; formato en Europe/Madrid y conversión a Instant
        pages/                  # Sin acceso, No encontrado
      ui/
        styles/                 # tokens, paleta, temas, tipografía, reset, mixins de breakpoints
        <componente>/           # bf-button, bf-sheet… con su test
        catalog/                # /_ui, solo en desarrollo
        README.md               # dirección visual (FE-28)
      shared/                   # avatar de usuario, pipes bfDate (FE-19)… lo que pidan estas pantallas
      features/
        identity/               # login, perfil
        league/                 # mis ligas, crear, invitación, inicio provisional, miembros, admin/
```

## 5. Herramientas y configuración

- **Proyecto:** `ng new` standalone, zoneless, SCSS, sin SSR. Node LTS compatible con esa versión de Angular en `.nvmrc` y `engines`; npm con `package-lock.json`.
- **TypeScript y plantillas:** las opciones de FE-21 en `tsconfig.json` y `angularCompilerOptions`.
- **`angular.json`:** presupuestos de RNF-13 (inicial: aviso a 400 kB, error a 600 kB), `i18n.sourceLocale: "es"`, `security.autoCsp`, y `fileReplacements` para que `/_ui` no entre en el build de producción.
- **ESLint** (FE-22, FE-32): `angular-eslint` (TS y plantillas, con accesibilidad y `template/i18n` exigiendo id explícito), `typescript-eslint` `strictTypeChecked` y `stylisticTypeChecked`, `no-non-null-assertion`, `consistent-type-assertions` con `assertionStyle: never` (salvo `as const`), `switch-exhaustiveness-check`, `eslint-plugin-boundaries` con los elementos `core`, `core-api`, `ui`, `shared` y `feature` (capturando el nombre, para prohibir una feature desde otra), y `eslint-config-prettier`. `--max-warnings 0`.
- **Stylelint:** `stylelint-config-standard-scss` y `stylelint-declaration-strict-value` para exigir `var(--bf-*)` en color, fondo, borde, sombra, radio, espaciado y tamaño de letra fuera de `ui/styles/`.
- **Prettier** para TS, HTML, SCSS y JSON.
- **Vitest** con el builder de tests unitarios de Angular, Angular Testing Library y `@testing-library/jest-dom`; jsdom.
- **Scripts** de FE-35: `start` (`ng serve` con `proxy.conf.json` para `/api`, `/oauth2` y `/login/oauth2`), `build`, `lint` (ESLint y Stylelint), `format`, `format:check`, `test`, `e2e`, `api:types` (`openapi-typescript ../api/openapi.json -o src/app/core/api/schema.d.ts`) e `i18n:extract` (`ng extract-i18n --output-path src/locale`). `api:types` llega con sus tipos (tarea 7) y `e2e` con Playwright (tarea 14).
- `.gitignore`: `node_modules`, `dist`, `.angular`, informes de Playwright.

## 6. Contrato con el backend

- **Ruta tipada:** una función de `core/api` construye la URL a partir de una clave de `paths` y sus parámetros, para que un endpoint renombrado rompa la compilación.
- **`IdentityApi`** (`me`, `logout`) y **`LeagueApi`** (ligas, crear, invitación, unirse, miembros, salir, expulsar, parámetros): un método por endpoint; lecturas que devuelven `httpResource` cuando dependen de signals, escrituras que devuelven `Observable`. Alias con el nombre del DTO (FE-06). Tests con `HttpTestingController`.
- **CSRF:** `withXsrfConfiguration` con `XSRF-TOKEN`/`X-XSRF-TOKEN` (los de Angular por defecto, explícitos). Angular no lo añade a URLs absolutas, así que el `PUT` a S3 no lo lleva.
- **Errores** (FE-08): `toProblem(unknown)` valida en tiempo de ejecución la forma de `Problem` (sin `as`); el interceptor actúa solo sobre `/api/`: `401` guarda la ruta y va a `/login`, `403` y `404` a sus páginas (salvo en las peticiones que la pantalla marca como propias con un `HttpContext`), y `5xx` o red, un toast con reintento. `ERROR_TEXTS: Record<ErrorCode, string>` con `$localize`, exhaustivo sobre la unión generada.

## 7. Sistema de diseño

### 7.1 Dirección visual (FE-28)

Primera tarea de UI, con la skill `.agents/skills/frontend-design`: tono, composición del shell y de dos pantallas clave (mis ligas y, como boceto, el campo de la alineación para que la escala aguante lo que viene), escala tipográfica y de espaciado, radios, sombras y movimiento. Se documenta en `ui/README.md` y se revisa con el usuario antes de construir componentes.

### 7.2 Tokens, paleta y temas (FE-24 a FE-26)

- `ui/styles/`: `_palette.scss` (generado, §2 decisión 6), `_tokens.scss` (semánticos: `--bf-color-bg`, `-surface`, `-text`, `-text-muted`, `-border`, `-brand`, `-focus`, `-position-goalkeeper`…, `-danger`, `-warning`; espaciado, radios, sombras, fuentes y escala), `_themes.scss` (claro en `:root`, oscuro con `[data-theme=dark]` y con `prefers-color-scheme` cuando no hay `data-theme`), `_breakpoints.scss` (mixins `md` y `lg`, siempre `min-width`) y un reset mínimo.
- El script de `index.html` lee la preferencia de `localStorage` y pone `data-theme` en `<html>` antes de pintar; `autoCsp` lo incluye por hash. `ThemeService` en `core` la cambia y la guarda.
- Contraste comprobado por el test de la decisión 6 y, en E2E, por axe en los dos temas.

### 7.3 Tipografía e iconos (FE-27)

`@fontsource/barlow` y `@fontsource/barlow-condensed`, solo los pesos y el subconjunto latino que se usen, con `font-display: swap`; números tabulares en cifras. Inputs a 16 px como mínimo. Lucide importando cada icono por separado.

### 7.4 Componentes de `ui/` de este plan (FE-17, FE-18)

Botón e icon button, campo de formulario (input), tarjeta, elemento de lista, chip/badge, avatar, hoja inferior (CDK Overlay), diálogo de confirmación, toast, skeleton, estado vacío, barra superior y navegación (inferior y lateral). Cada uno con su test (estados y accesibilidad básica) y en `/_ui` en los dos temas.

## 8. Núcleo y shell

- **Arranque:** `provideAppInitializer` pide `GET /api/me`; con `401`, no hay sesión. `SessionStore` guarda usuario y ligas con rol (`GET /api/me/leagues`) y se recarga al crear, unirse o salir de una liga.
- **Login y logout** (FE-10): el botón navega a `/oauth2/authorization/google`; la ruta de vuelta vive en `sessionStorage` y se consume al volver a `/`. Logout con `POST /api/logout`, limpia el store y va a `/login`.
- **Guards** (solo ocultan, autoriza el backend): `authGuard` (sin sesión, guarda la ruta y va a `/login`), `memberGuard` (liga que no está en mis ligas: «No encontrado»), `adminGuard` (no admin: «Sin acceso»). Tests de los tres.
- **Rutas** (FE-03; en inglés, decisión 15 del plan 10): `withComponentInputBinding`, lazy por feature. `/` lleva a la última liga usada si sigue siendo mía, o a `/leagues`.
- **Shell** (FE-29, FE-30): barra superior con la liga actual y el cambio de liga (hoja inferior), navegación inferior en móvil y lateral desde `lg`, *safe areas* y `dvh`. La navegación solo muestra los destinos que ya existen; en este plan, el inicio provisional y *Liga*.
- **Sin conexión** (FE-31): `online`/`offline` del navegador en un signal; aviso fijo y botones de envío deshabilitados.
- **Fechas** (RNF-05, RNF-15): `core/time` formatea con `Intl.DateTimeFormat` (`es-ES`, `Europe/Madrid`) y convierte hora de Madrid a `Instant` con `@date-fns/tz`; tests en los cambios de hora (último domingo de marzo y de octubre). Pipe `bfDate` en `shared`.

## 9. Primeras pantallas

| Pantalla | Ruta | Quién | RF | Endpoints |
| --- | --- | --- | --- | --- |
| Login (con el error de `?error`) | `/login` | Todos | RF-01 | `/oauth2/authorization/google` |
| Mis ligas | `/leagues` | Todos | RF-08 | `GET /api/me/leagues` |
| Crear liga | `/leagues/new` | Todos | RF-05 | `POST /api/leagues` |
| Invitación (vista previa y unirse) | `/invite/:token` | Todos | RF-06 | `GET /api/invitations/{token}`, `POST …/join` |
| Perfil, tema y cerrar sesión | `/profile` | Todos | RF-02, RF-04 | `GET /api/me`, `POST /api/logout` |
| Inicio de la liga (provisional: estado vacío) | `/leagues/:leagueId` | Miembro | — | `GET /api/leagues/{id}` |
| Liga: miembros y salir | `/leagues/:leagueId/members` | Miembro | RF-08 | `GET /members`, `DELETE /members/me` |
| Admin · invitación y miembros | `/leagues/:leagueId/admin/members` | Admin | RF-06 | `POST/DELETE /invitation`, `DELETE /members/{id}` |

Cada una con carga, vacío, error con reintento y contenido (FE-14), textos con `i18n` y sus `400`/`409` tratados (`ADMIN_CANNOT_LEAVE`, nombre de liga…). Compartir el enlace de invitación usa `navigator.share` cuando existe y, si no, copiar al portapapeles.

## 10. Servido: Caddy y CSP

- `deploy/Caddyfile`: `/api/*`, `/oauth2/*`, `/login/oauth2/*` y `/actuator/health` al backend; el resto, estáticos con *fallback* a `index.html`. `index.html` sin caché; los ficheros con hash, `immutable` un año.
- Cabeceras: HSTS (solo con dominio), `X-Content-Type-Options`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` restrictiva y CSP con `default-src 'self'`, `img-src` con el origen de las fotos y el de los avatares de Google, `connect-src` con el del bucket, `font-src 'self'`, `style-src 'self' 'unsafe-inline'` (estilos que inyecta Angular), `frame-ancestors 'none'`, `base-uri 'self'` y `object-src 'none'`. La política de scripts la pone la meta de `autoCsp`; la cabecera no la endurece (las dos se aplican a la vez).
- Orígenes de fotos y bucket por variables de entorno del Caddyfile.
- Probado en local con el perfil `caddy` de Compose (decisión 9) y en el E2E de CI, que falla ante cualquier violación de CSP en consola.

## 11. Tests y CI

- **Unitarios y de componentes** (FE-33): los de §6, §7 y §8, y cada página con sus estados (con los servicios de API simulados).
- **E2E de humo** (FE-34) en `e2e/`, móvil en Chromium y WebKit, en los dos temas, con `@axe-core/playwright` en cada pantalla: un usuario sin sesión abre una invitación y vuelve a ella tras el login; el admin entra, crea una liga, genera la invitación, otro usuario se une, el admin lo ve y lo expulsa, y el otro ya no ve la liga. El login usa `POST /api/e2e/login`.
- **CI** (`.github/workflows/ci.yml`):
  - `backend`: igual que ahora; incluye `OpenApiSpecTests`.
  - `frontend`: Node de `.nvmrc`, `npm ci`, `api:types`, `palette` e `i18n:extract` con `git diff --exit-code` sobre `api/`, `schema.d.ts`, `_palette.scss` y `messages.xlf`, `lint`, `format:check`, `test` y `build`.
  - `e2e` (tras los dos): Postgres como servicio, backend con el perfil `e2e`, build del front servido por Caddy, Playwright (Chromium y WebKit); sube el informe si falla.

## 12. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | Revisar este plan con el usuario y cerrar las propuestas (§2) | Decisiones cerradas | ✅ |
| 2 | `OpenApiSpecTests`, spec determinista y `api/openapi.json` (§3.1) | Spec versionado (FE-05) | ✅ |
| 3 | `ErrorCode` como enum en el spec; `MatchError`, `SquadError` y `LeagueError`; tests (§3.2) | Códigos en el contrato (FE-08) | ✅ |
| 4 | Vuelta tras el login, cabeceras de proxy y `SessionTests`; redirect URI local y CORS en `commands.md`; `/login/oauth2` en `requirements.md` y `frontend.md` (§3.3, §3.5, decisión 3) | Login listo para el front (RF-01, FE-10) | ✅ |
| 5 | Login de pruebas con perfil `e2e` y sus tests (§3.4) | Sesión para E2E (FE-34) | ✅ |
| 6 | `ng new`, TypeScript estricto, `angular.json`, ESLint, Prettier, Stylelint, Vitest, scripts y estructura de carpetas (§4, §5) | Esqueleto que compila, pasa lint y tests (FE-01, FE-02, FE-21, FE-22, FE-32, FE-35) | ✅ |
| 7 | Tipos generados, ruta tipada, `IdentityApi`, `LeagueApi`, CSRF, interceptor y textos de `ErrorCode` (§6) | Contrato con el backend (FE-05 a FE-09) | ✅ |
| 8 | Dirección visual con la skill, revisada con el usuario; `ui/README.md` (§7.1) | Identidad cerrada (FE-28) | ✅ |
| 9 | Paleta, tokens, temas, tipografía, iconos y test de contraste (§7.2, §7.3) | Sistema de diseño base (FE-24 a FE-27, RNF-12) | ✅ |
| 10 | Componentes de `ui/` de §7.4 y `/_ui` | Catálogo inicial (FE-17, FE-18, FE-20) | ✅ |
| 11 | Arranque, `SessionStore`, login y logout, guards, rutas, shell, sin conexión, páginas de error y `core/time` (§8) | Shell navegable (FE-03, FE-10, FE-13, FE-29 a FE-31) | ✅ |
| 12 | Pantallas de §9 con sus tests | Entrar, crear liga e invitar (RF-01, RF-02, RF-04 a RF-06, RF-08) | ✅ |
| 13 | `deploy/Caddyfile`, perfil `caddy` en Compose y `cors.json` (§10) | Servido como en producción (RNF-16) | ✅ |
| 14 | Playwright, E2E de humo con axe y jobs `frontend` y `e2e` de CI (§11) | Calidad automatizada (FE-33, FE-34) | ✅ |
| 15 | `commands.md` (comandos y variables del front y de Caddy), `testing.md`, `frontend.md`, `status.md`, *Estado* de `AGENTS.md` y marcar el ítem de la Fase 0 en `requirements.md` | Ítem de la Fase 0 cerrado | |

Cada tarea es un commit en Conventional Commits citando sus códigos: `feat(identity): …`, `feat(frontend): … (FE-xx)`, `ci: …`, `docs: …`.

## 13. Riesgos y notas

- **Proxy de desarrollo y OAuth:** si el proxy de `ng serve` reescribe `Host`, Spring construye el redirect URI con `localhost:8080` y Google lo rechaza. Se prueba a mano una vez (no se puede automatizar con Google) y queda documentado.
- **Dos CSP a la vez:** la meta de `autoCsp` y la cabecera de Caddy se aplican las dos, así que una fuente debe estar permitida en ambas. El E2E por Caddy detecta una combinación que rompa la app.
- **`style-src 'unsafe-inline'`:** Angular inyecta estilos en `<style>`; un nonce exigiría plantillas por petición en Caddy. El riesgo es bajo (no se pinta HTML de usuario, RNF-16); se revisa si Angular ofrece hashes de estilos.
- **Códigos compartidos entre módulos:** `DUPLICATE_PLAYER` ya existe en `rating` y `lineup`. Con un solo `Record`, un nombre repetido debe significar lo mismo; si no, se renombra.
- **Spec y versiones:** subir springdoc o Spring Boot puede cambiar el spec sin cambiar el contrato. Se regenera y el diff lo enseña.
- **Login de pruebas en producción:** depende de no activar nunca el perfil `e2e`. Lo cubre el test sin perfil y, cuando exista el Compose de producción, se revisa que no lo active.
- **WebKit no es Safari iOS:** el E2E en Linux aproxima; la comprobación en iPhone real se hace a mano antes de la prueba real.
- **Versión de Angular:** se fija la última estable al empezar la tarea 6; si el builder de Vitest o Angular Testing Library no van con ella, se documenta el ajuste en `frontend.md`.
- **Inicio provisional:** la navegación crece con cada pantalla; hasta entonces, el inicio de la liga es un estado vacío y no una pantalla a medias.
