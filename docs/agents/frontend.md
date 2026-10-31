# Frontend

Guía para agentes de `frontend/`. La fuente de verdad son las secciones *Requisitos no funcionales* (RNF-01, RNF-12 a RNF-16) y *Frontend* (FE-01 a FE-35) de `docs/requirements.md`; esto es un resumen para trabajar. Cita los códigos en commits y revisiones, como los RF.

El esqueleto de `frontend/` se montó según `docs/11-frontend-foundation-plan.md` (completo: núcleo, shell, sistema de diseño, pantallas de identidad y liga, Caddy, E2E de humo y CI); las demás pantallas llegan con los planes de la Fase 2. Angular 22 (zoneless, Vitest con `@angular/build:unit-test`, nombres de fichero de la guía de 2025: `app.ts`, `catalog-page.ts`), TypeScript 6 y Node 26 LTS (`.nvmrc`; Angular 22 no admite Node 25). `openapi-typescript` 7 pide TypeScript 5 como *peer*: un `overrides` de `package.json` le da el 6 del proyecto, con el que genera igual.

## Qué es

Una web Angular **mobile first** (360 × 640 primero), responsive, en español y preparada para traducir. Sin PWA instalable, Web Push ni modo offline hasta la Fase 3. Se sirve desde el mismo origen que el API (Caddy: estáticos, y `/api`, `/oauth2` y `/login/oauth2` al backend; `/login` es la pantalla de Angular).

Stack: Angular (última estable: standalone, zoneless, signals, `@if`/`@for`/`@defer`), Angular CDK y librería propia (`ui/`), SCSS con tokens en CSS custom properties, tipos generados con `openapi-typescript`, `@angular/localize`, Lucide, Barlow y Barlow Condensed con `@fontsource`, `Intl.DateTimeFormat` y `@date-fns/tz`. Calidad: ESLint, Prettier, Stylelint, Vitest con Angular Testing Library y Playwright. npm con `package-lock.json`, Node LTS fijado en `.nvmrc`.

## Reglas resumidas

### Arquitectura (FE-01 a FE-04)

```
frontend/src/app/
  core/       # singletons: auth, http (interceptores), api (tipos generados y un servicio por módulo del backend), layout, i18n, time
  ui/         # sistema de diseño sin dominio: tokens, estilos globales, componentes bf-*
  shared/     # piezas de dominio reutilizables: chip de posición, tarjeta de jugador, input de valoración, pipes
  features/   # una carpeta por módulo del backend: identity, league, squad, match, lineup, rating, scoring
```

- `ui` no importa nada de la app; `shared` usa `ui`, los tipos de `core/api` y las funciones de fecha de `core/time`; una feature usa `core`, `ui` y `shared`, **nunca otra feature** (se navega por ruta). Lo comprueba `eslint-plugin-boundaries`, como Modulith en el backend.
- Rutas lazy por feature, en inglés, con la liga en la URL (`/leagues/:leagueId/...`); parámetros y `data` de la ruta como `input()` (`withComponentInputBinding`, con `paramsInheritanceStrategy: 'always'`, así que `leagueId` llega a cualquier página de la liga). Admin bajo `features/<feature>/admin/` con `adminGuard`. Cada feature exporta sus rutas en `<feature>.routes.ts` (páginas con `loadComponent`) y `app.routes.ts` las coloca: las de fuera de una liga, en la primera ruta `''` del shell, antes de `leagues/:leagueId` (así `leagues/new` no se toma por una liga) y con `authGuard` en cada una; las de una liga, como hijas de `leagues/:leagueId` (shell con `authGuard` y `memberGuard`), y sus admin bajo `admin`; el login, fuera del shell con `guestGuard` (con sesión, a `/`). Cada ruta lleva su `title` (con `$localize`): `PageTitle` lo pone en la pestaña.
- Solo las páginas (`*-page`) inyectan servicios de API y orquestan; los componentes de `ui/` y `shared/` son de presentación (`input()`, `output()`, `model()`).

### Contrato con el backend (FE-05 a FE-11)

- Los tipos salen del OpenAPI: `api/openapi.json` (lo escribe un test del backend) → `npm run api:types` → `core/api/schema.d.ts`. Los dos están versionados y CI falla si no están al día. Se generan con `--properties-required-by-default`: Jackson escribe siempre todas las propiedades (las vacías como `null`, y el spec las marca `nullable`), así que una propiedad sin `required` en el spec llega igual. La excepción es `Problem`, que solo se lee con `toProblem`.
- URLs con `apiPath('/api/leagues/{id}', { id })`: solo admite rutas de `paths` con todos sus parámetros, así que un endpoint renombrado rompe la compilación.
- **Nada de tipos del backend a mano:** alias con el nombre del DTO (`type PlayerResponse = components['schemas']['PlayerResponse']`); un modelo de vista se deriva (`Pick`, `&`) con una función de mapeo probada.
- Un servicio por módulo del backend (`LeagueApi`, `SquadApi`, `MatchApi`, `LineupApi`, `RatingApi`, `ScoringApi`, `IdentityApi`), un método por endpoint, URLs relativas, sin lógica de negocio. Las lecturas que dependen de un signal (la liga o el token de la ruta) devuelven un `httpResource` y reciben `() => string | undefined` (con `undefined` no piden nada): se llaman en un contexto de inyección, como un inicializador de campo de la página. Las demás devuelven un `Observable`, y una respuesta `204`, `Observable<null>`.
- Errores `ProblemDetail`: el interceptor (`core/http`, solo en `/api/`) resuelve `401` (login guardando la ruta), `403` y `404` (su página, sin cambiar la URL), y `5xx` y la red (la petición espera en `FailedRequests` a que el usuario reintente o desista); los `400` y `409` los trata la pantalla con `toProblem(error)`: los `violations` (campo obligatorio que falta, `campo: mensaje`; `violates(problem, 'name')` dice si nombran el campo), en su campo; el `code` (todo `409` y los `400` de un límite o regla de negocio), con su texto de `ERROR_TEXTS`, un `Record<ErrorCode, string>` exhaustivo sobre el enum `ErrorCode` del spec. Una pantalla que trata ella misma un estado global (el `404` de una invitación caducada) pasa `handling(404)` como `HttpContext` al método del servicio.
- **Plazos y límites en el servidor** (RNF-03): las cuentas atrás son orientativas y un `409` refresca y explica. Ningún límite ni valor por defecto de negocio como constante en el front: se lee del API o valida solo el servidor.
- Sesión (`core/auth/SessionStore`): `GET /api/me` al arrancar; login con navegación completa a `/oauth2/authorization/google` con `session.login(returnUrl?)` (ruta de vuelta en `sessionStorage`: el backend vuelve siempre a `/`, que la recupera; o a `/login?error` si falla); logout con `session.logout()`. Tras crear, unirse o salir de una liga, la página llama a `session.reloadLeagues()`. Los guards solo ocultan: autoriza el backend. `/` lleva a la ruta guardada, a la última liga usada (`LastLeague`) o a `/leagues`.
- **Anonimato** (RF-21): nunca se muestran ni deducen votos individuales de otros.

### Estado y datos (FE-12 a FE-15)

- Lecturas con `httpResource`/`resource` dependientes de signals; escrituras como métodos del servicio que, al terminar, recargan lo afectado. Sin actualizaciones optimistas ni librería de estado.
- Estado global mínimo: `SessionStore` (usuario y ligas con rol) y tema.
- Toda vista asíncrona resuelve carga (*skeleton*), vacío, error con reintento y contenido.
- Reactive Forms tipados (`NonNullableFormBuilder`), con los errores del servidor en su campo.

### Componentes (FE-16 a FE-20)

- **Reutilizar antes de crear**; un patrón que aparece en dos pantallas se extrae.
- Standalone, `OnPush`, `inject()`, prefijo `bf-`, guía de estilo oficial de Angular; accesibles de serie con `@angular/cdk/a11y`.
- Catálogos iniciales: FE-18 (`ui/`) y FE-19 (`shared/`). Lo demás se añade cuando lo pida una pantalla.
- Los componentes de `ui/` y su uso están en `ui/README.md` (*Componentes*). La hoja inferior y el diálogo de confirmación usan CDK Dialog (`Sheet`, `Confirm`); los estilos de lo que el CDK pinta fuera de los componentes están en `ui/styles/_overlay.scss`. Un componente sobre un elemento nativo usa selector de atributo (`button[bfButton]`), que el lint admite en camelCase.
- Cada componente de `ui/` aparece en `/_ui` (solo en desarrollo) con todos sus estados y en los dos temas. Sus rutas están en `app/dev.routes.ts`, que el build de producción sustituye por `dev.routes.prod.ts` (vacío); el catálogo no se traduce, así que el lint de `i18n` no lo revisa.

### Tipado (FE-21 a FE-23)

- TypeScript estricto (con `noUncheckedIndexedAccess` y `noPropertyAccessFromIndexSignature`) y plantillas estrictas.
- Prohibidos por lint: `any`, `!`, `as` salvo `as const`, y `switch` no exhaustivo sobre uniones.
- Enums del backend como uniones generadas; sus textos en `Record<Position, string>` y similares, para que un valor nuevo rompa la compilación.

### Estilo visual y mobile first (FE-24 a FE-31)

- Solo tokens (`--bf-color-*`, `--bf-space-*`, `--bf-radius-*`, `--bf-font-*`, `--bf-shadow-*`, `--bf-motion-*`) de `ui/styles/_tokens.scss`; Stylelint prohíbe colores y medidas literales. La paleta (`_palette.scss`) la genera `npm run palette` y solo la usan los tokens; `ui/styles/contrast.spec.ts` comprueba el contraste de los pares de tokens en los dos temas. Los estilos de componente importan mixins con `@use 'ui/styles/typography' as type;` o `'ui/styles/breakpoints'` (`includePaths` apunta a `src/app`).
- Paleta de FE-25: verde de marca, lavanda secundaria y un color por posición (portero, defensa, medio, delantero), con escalas 50–900, neutros cálidos, rojo de error y ámbar de aviso. En tema claro, texto casi negro sobre los tonos base y tonos 700–800 para texto de color.
- Tema claro y oscuro: sigue a `prefers-color-scheme`, con interruptor (`Theme` de `core/theme`, clave `bf.theme` en `localStorage`) que el script de `index.html` aplica como `data-theme` antes del primer pintado. Cualquier elemento con `data-theme` cambia el tema de lo que contiene.
- Barlow Condensed (títulos y cifras tabulares) y Barlow (texto), de `@fontsource`; inputs de 16 px como mínimo. Iconos con `@lucide/angular`, un componente por icono (`<svg lucideHouse></svg>`). La dirección visual (peto, césped, escala, radios, movimiento y textos) está en `ui/README.md`, con el boceto en `docs/design/visual-direction.html`: léela antes de construir una pantalla o un componente.
- Puntos de corte: base (< 600), `md` (≥ 600) y `lg` (≥ 1024: navegación lateral y dos columnas), siempre con `min-width`.
- Acción principal abajo; navegación inferior de 4–5 destinos; *safe areas* y `dvh`; `type` e `inputmode` correctos; nada solo con hover y todo gesto con alternativa de botón.
- Navegación (FE-30): los cinco destinos están en `core/layout/destinations.ts`, cada uno con los prefijos de ruta que cubre (*Liga* cubre `league`, `squad`, `matches`, `rules`, `members` y `admin`); el shell mira el primer segmento tras la liga y se lo pasa a cada `bfNavItem` con `[active]`. Una ruta nueva de una liga se añade al prefijo de su destino, y `destinations.spec.ts` impide que dos destinos compartan uno.
- Plazos y cambios sin guardar (FE-09, FE-30): una página con plazos llama en su constructor a `onVisible(callback)` (`core/http/visible`), que recarga sus recursos al volver a la pestaña. Una página con borrador (alineación, valorar) expone `unsaved: Signal<boolean>` (`HasUnsavedChanges`), pone `canDeactivate: [unsavedChangesGuard]` en su ruta y llama a `warnBeforeUnload(unsaved)` (`core/layout/unsaved-changes`) para avisar al cerrar la pestaña.
- Una página ocupa el alto del contenido (`min-height: 100%`) para dejar su acción principal abajo con `margin-top: auto`; en `features/league/_page.scss` están los mixins de la feature. Los textos de los roles están en `core/layout/role-texts.ts`.
- Sin conexión: el componente raíz muestra el aviso; cada página deshabilita sus envíos con `[disabled]="!online.online()"` (`Online` de `core/http`). El toast de reintento de `FailedRequests` y `bf-toaster` también están en el componente raíz, así que valen en cualquier página.
- Fechas: `formatInstant` y `madridToInstant` de `core/time` (`es-ES`, `Europe/Madrid`); en plantillas, `{{ instant | bfDate: 'dateTime' }}` (`shared/time`); `day` («2 de noviembre de 2026») para un día lejano, como el alta de un miembro.

### No funcionales (RNF-12 a RNF-16)

- WCAG 2.2 AA en los dos temas, objetivos táctiles de 44 × 44 px, foco visible, teclado y lector de pantalla, `prefers-reduced-motion`.
- Bundle inicial: aviso a 400 kB y error a 600 kB; LCP < 2,5 s en 4G.
- Textos con `i18n`/`$localize` e id explícito (`@@<capa o feature>.<pantalla>.<texto>`, lo exige el lint en las plantillas); fechas, números e importes en `es-ES` y `Europe/Madrid`. `npm run i18n:extract` escribe `src/locale/messages.xlf` (versionado) desde el build de producción.
- Sesión solo en la cookie `HttpOnly`; CSRF con `XSRF-TOKEN`/`X-XSRF-TOKEN`; sin `innerHTML` con contenido de usuario ni `bypassSecurityTrust*`; CSP estricta (`autoCsp`).

### Calidad (FE-32 a FE-35)

- ESLint (flat config, `strictTypeChecked`, `boundaries`, reglas de accesibilidad de plantillas) y Prettier; un aviso de lint es un error.
- Tests: ver el apartado *Frontend* de [testing.md](testing.md).
- Scripts npm: `start`, `build`, `lint`, `format`, `format:check`, `test`, `e2e`, `palette`, `api:types` e `i18n:extract` (detalle en [commands.md](commands.md)). CI (jobs `frontend` y `e2e`) los ejecuta todos y falla si un fichero generado no está al día.
- Servido: en producción y en local con el perfil `caddy`, `deploy/Caddyfile` (estáticos, proxy, cabeceras y CSP; variables en [commands.md](commands.md)). Un origen nuevo del que la app cargue imágenes o datos se añade a la CSP de Caddy (`img-src`, `connect-src`): la `<meta>` de `autoCsp` solo fija scripts, `object-src` y `base-uri`, y las dos se aplican a la vez. El E2E falla ante cualquier violación.

## Cómo añadir o cambiar un endpoint

1. En el backend, documenta el endpoint con springdoc como pide [conventions.md](conventions.md) (si añade un `ErrorCode`, que quede en el spec).
2. Regenera `api/openapi.json` con `OpenApiSpecTests` (ver [commands.md](commands.md)) y después `npm run api:types`.
3. En `core/api/<módulo>/`, exporta los alias de los DTO nuevos y añade el método al `<Módulo>Api`, tipado con `paths`.
4. Si hay códigos de error nuevos, el `Record<ErrorCode, string>` deja de compilar: añade su texto.
5. Prueba el método con `HttpTestingController`.
6. Sube el spec y los tipos en el mismo cambio que el endpoint: el diff enseña el cambio de contrato.

## Cómo añadir un componente

1. Busca en `ui/`, `shared/` y `/_ui` algo que sirva o que se pueda ampliar.
2. Elige la capa: sin dominio, `ui/`; con dominio y en más de una pantalla, `shared/`; solo en una pantalla, dentro de su feature.
3. Aplica FE-17: standalone, `OnPush`, prefijo `bf-`, solo `input()`/`output()`/`model()`, accesible con el CDK, estilos solo con tokens y textos con `i18n`.
4. Pruébalo con Vitest y Angular Testing Library: sus estados y la accesibilidad básica.
5. Si es de `ui/`, añádelo a `/_ui` con todos sus estados en los dos temas.
