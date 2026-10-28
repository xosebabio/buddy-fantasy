# 10 · Requisitos de frontend — plan

Plan para añadir a `docs/requirements.md` los requisitos **no funcionales** del frontend: arquitectura, stack, reglas de código, sistema de diseño, estilo visual y calidad. Los requisitos funcionales (RF-01 a RF-41) ya están y no cambian. El plan solo toca documentación; el código de `frontend/` y el trabajo de backend que se desprende (§5) van en un plan propio (`11-frontend-foundation-plan.md`).

Motivo: el núcleo jugable (Fases 1 y 2) está hecho en backend, pero la *prueba real con los amigos* necesita una web. Se aparca la PWA instalable: primero, una buena web pensada para el teléfono.

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Plataforma | Web Angular **mobile first**, responsive. **Sin PWA instalable** por ahora: ni manifest ni service worker. El ítem *PWA instalable* sigue en la Fase 3, junto con Web Push, que lo necesita. |
| Pantallas | Todo lo que ya expone el backend, de manager y de admin: lo necesario para jugar una jornada real sin Swagger. |
| Escritorio | Responsive con ajustes: la barra inferior pasa a ser lateral y algunas vistas se ven a dos columnas. |
| Idioma | Interfaz en español, **preparada para traducir** con `@angular/localize`. Solo se compila `es`. |
| Fuera de alcance | PWA, Web Push, modo offline, gráfica de valor (RF-36, Fase 3), apps nativas. |

## 2. Decisiones tomadas

| # | Tema | Decisión | Origen |
| --- | --- | --- | --- |
| 1 | Framework | Angular, última versión estable: standalone, zoneless, signals, control flow nativo (`@if`, `@for`, `@defer`) | Usuario |
| 2 | Componentes | **Angular CDK + librería propia** (`ui/`) con tokens de diseño. Sin Material ni PrimeNG | Usuario |
| 3 | Contrato con el backend | **Solo tipos** generados con `openapi-typescript` y **servicios propios** finos por módulo del backend, sobre `HttpClient`/`httpResource` | Usuario |
| 4 | Origen del spec | **Fichero versionado** `api/openapi.json`: lo escribe un test del backend; CI falla si el spec o los tipos generados no están al día | Usuario |
| 5 | Estilos | **SCSS + CSS custom properties** (tokens); estilos encapsulados por componente | Usuario |
| 6 | Estado | **Signals + servicios** (`httpResource`/`resource`), sin librería de estado | Usuario |
| 7 | Tema | **Claro y oscuro**: sigue a `prefers-color-scheme`, con interruptor manual que se recuerda | Usuario |
| 8 | Tests | **Vitest** + Angular Testing Library; **Playwright** E2E en viewport móvil contra el backend real | Usuario |
| 9 | Servido | **Caddy, mismo origen**: estáticos, y `/api`, `/oauth2` y `/login` al backend. En cloud, CDN con las mismas rutas | Usuario |
| 10 | Lint y formato | ESLint (angular-eslint + typescript-eslint *strict type checked*) y Prettier | Usuario |
| 11 | Paleta | La del usuario, asignada a marca y posiciones (§3.3.6), más neutros y colores de estado añadidos | Usuario + propuesta (aceptada) |
| 12 | Tipografía | Barlow Condensed (títulos, marcadores y cifras) + Barlow (texto), servidas desde el propio dominio con `@fontsource` | Propuesta, aceptada |
| 13 | Iconos | Lucide (`lucide-angular`), un único set | Propuesta, aceptada |
| 14 | Fechas | `Intl.DateTimeFormat` con `Europe/Madrid` para mostrar; `@date-fns/tz` para convertir lo que escribe el admin (hora de Madrid) a `Instant` UTC | Propuesta, aceptada |
| 15 | Rutas | En inglés (`/leagues/:leagueId/lineup`), como el API: con `@angular/localize` las rutas no se traducen | Propuesta, aceptada |
| 16 | Catálogo de componentes | Ruta solo de desarrollo (`/_ui`) con cada componente de `ui/` en todos sus estados. Sin Storybook | Propuesta, aceptada |
| 17 | Gestor de paquetes | npm con `package-lock.json`; versión de Node LTS fijada en `.nvmrc` y `engines` | Propuesta, aceptada |
| 18 | Colores en SCSS | Stylelint para prohibir colores y medidas literales fuera de los tokens | Propuesta (herramienta extra), aceptada |
| 19 | Roadmap | Esqueleto del frontend en la Fase 0; pantallas en la Fase 2, antes de la *prueba real* (§3.5) | Propuesta, aceptada |

Las decisiones marcadas como *Propuesta* se revisaron con el usuario (tarea 1) y se aceptaron tal cual.

## 3. Cambios en `requirements.md`

### 3.1 Cabecera y *Visión y alcance*

- Primer párrafo: «Una web app en Angular (PWA)…» pasa a «Una web en Angular, pensada para el móvil, …».
- Añadir a *Fuera del alcance (v1)* de momento: «PWA instalable y modo offline (se valoran en la Fase 3 con Web Push)».

### 3.2 *Requisitos no funcionales*

Reescribir RNF-01 y añadir RNF-12 a RNF-16:

- **RNF-01 Plataforma:** web responsive en Angular, *mobile first*: se diseña primero para 360 × 640 y se adapta a tableta y escritorio. Sin apps nativas. La instalación como PWA queda para la Fase 3.
- **RNF-12 Accesibilidad:** WCAG 2.2 AA: contraste de 4,5:1 en texto y 3:1 en elementos gráficos, en los dos temas; objetivos táctiles de al menos 44 × 44 px; foco visible; todo usable con teclado y lector de pantalla; respeta `prefers-reduced-motion`.
- **RNF-13 Rendimiento del cliente:** rutas cargadas bajo demanda por feature; presupuestos de `angular.json` (bundle inicial: aviso a 400 kB y error a 600 kB, sin comprimir); LCP < 2,5 s en un móvil medio con 4G.
- **RNF-14 Compatibilidad:** las dos últimas versiones de Safari iOS, Chrome Android y los navegadores de escritorio principales (Chrome, Firefox, Safari, Edge).
- **RNF-15 Internacionalización:** textos marcados con `@angular/localize` (`i18n` en plantillas, `$localize` en código), con id explícito; idioma de origen `es`. Fechas, números e importes con el locale `es-ES` y la zona `Europe/Madrid` (RNF-05).
- **RNF-16 Seguridad en el cliente:** la sesión vive solo en la cookie `HttpOnly` del backend (nada en `localStorage`); CSRF con la cookie `XSRF-TOKEN` y la cabecera `X-XSRF-TOKEN`; sin `innerHTML` con contenido de usuario ni `bypassSecurityTrust*`; CSP estricta servida por Caddy (build con `autoCsp`).

### 3.3 Sección nueva: *Frontend*

Va después de *Requisitos no funcionales* y antes de *Stack técnico propuesto*. Cada regla lleva un código **FE-xx** para citarla en commits y revisiones, como RF y RNF. Texto propuesto:

#### 3.3.1 Arquitectura

- **FE-01 Estructura.** `frontend/src/app/` en cuatro capas:

  ```
  core/       # singletons: auth, http (interceptores), api (tipos generados y un servicio por módulo del backend), layout (shell y navegación), i18n, time
  ui/         # sistema de diseño: tokens, estilos globales y componentes genéricos sin dominio (bf-button, bf-sheet…)
  shared/     # piezas de dominio reutilizables: chip de posición, tarjeta de jugador, input de valoración, pipes de puntos e importes
  features/   # una carpeta por módulo del backend: identity, league, squad, match, lineup, rating, scoring
  ```

- **FE-02 Límites entre capas**, comprobados con `eslint-plugin-boundaries`, igual que Modulith en el backend:
  - `ui` no importa nada de la app (solo Angular y CDK).
  - `shared` puede importar `ui` y los tipos de `core/api`.
  - Una feature usa `core`, `ui` y `shared`, **nunca otra feature**. Para pasar a otra feature se navega por ruta.
- **FE-03 Rutas.** Lazy por feature; la liga activa va en la URL (`/leagues/:leagueId/...`) y es lo que permite cambiar de liga (RF-08). Los parámetros llegan a los componentes como `input()` (`withComponentInputBinding`). Las pantallas de admin están en su feature bajo `admin/` y llevan `adminGuard`.
- **FE-04 Páginas y componentes.** Las páginas (`*-page`) son las únicas que inyectan servicios de API y orquestan. Los componentes de `ui/` y `shared/` son de presentación: solo `input()`, `output()` y `model()`.

#### 3.3.2 Contrato con el backend

- **FE-05 OpenAPI es la fuente de los tipos.** Un test del backend escribe `api/openapi.json` (versionado). `npm run api:types` genera `core/api/schema.d.ts` con `openapi-typescript` (también versionado). CI regenera los dos y falla si cambian.
- **FE-06 Nada de tipos del backend a mano.** Cada servicio de `core/api/<módulo>/` exporta alias con el mismo nombre que el DTO (`type PlayerResponse = components['schemas']['PlayerResponse']`). Las features no redeclaran formas del backend: si necesitan un modelo de vista, lo derivan (`Pick`, `&`) con una función de mapeo probada.
- **FE-07 Un servicio de API por módulo del backend** (`LeagueApi`, `SquadApi`, `MatchApi`, `LineupApi`, `RatingApi`, `ScoringApi`, `IdentityApi`): un método por endpoint, tipado con `paths` del schema, URLs relativas (`/api/...`). Sin lógica de negocio.
- **FE-08 Errores.** El backend responde `ProblemDetail` (RFC 9457) con `code` (`ErrorCode`) y `violations`. Un interceptor resuelve los globales: `401` lleva al login guardando la ruta de vuelta, `403` a «Sin acceso», `404` a «No encontrado», y `5xx` o un fallo de red muestran un aviso con reintento. Los `400` y `409` los trata la pantalla: los `violations`, en su campo del formulario; el `code`, con su mensaje de un `Record<ErrorCode, string>` exhaustivo (por eso el spec publica los códigos, §5).
- **FE-09 Plazos y límites en el servidor.** El cliente solo refleja (RNF-03): las cuentas atrás de bloqueo y de cierre son orientativas, y un `409` refresca la vista y lo explica. Ningún límite ni valor por defecto de negocio se repite en el front como constante (regla 1 del backend): si se quiere validar antes de enviar, el valor se lee del API; si el API no lo da, valida solo el servidor.
- **FE-10 Autenticación.** Al arrancar, `GET /api/me` decide si hay sesión. El login es una navegación completa a `/oauth2/authorization/google` y la ruta de vuelta (por ejemplo, la de una invitación) se guarda en `sessionStorage`. El logout es `POST /api/logout`. Los guards (`authGuard`, `memberGuard`, `adminGuard`) leen el rol por liga de `GET /api/me/leagues`; solo ocultan, porque la autorización la hace el backend (regla 8).
- **FE-11 Anonimato.** Ninguna pantalla muestra ni deduce votos individuales de otros (RF-21): solo media y número de votos al cerrar, y los votos propios.

#### 3.3.3 Estado y datos

- **FE-12 Signals.** El estado del servidor se lee con `httpResource`/`resource` dependiente de signals (liga, partido); las escrituras son métodos del servicio y, al terminar, recargan los recursos afectados. Sin actualizaciones optimistas: casi toda escritura depende de reglas del servidor (plazos, cambios, presupuesto).
- **FE-13 Estado global mínimo.** Un `SessionStore` (usuario y ligas con rol) y la preferencia de tema. Todo lo demás vive en la página que lo usa.
- **FE-14 Estados de cada vista.** Toda vista asíncrona resuelve explícitamente carga (*skeleton*), vacío, error con reintento y contenido.
- **FE-15 Formularios.** Reactive Forms tipados (`NonNullableFormBuilder`). Los errores del servidor se pintan en su campo.

#### 3.3.4 Componentes reutilizables

- **FE-16 Reutilizar antes de crear.** Antes de escribir un componente se busca en `ui/` y `shared/`. Si un patrón de interfaz aparece en dos pantallas, se extrae.
- **FE-17 Reglas de componente.** Standalone, `ChangeDetectionStrategy.OnPush`, `inject()`, prefijo `bf-`, guía de estilo oficial de Angular vigente. Accesibles de serie: rol, etiquetas y foco gestionados con `@angular/cdk/a11y`.
- **FE-18 Catálogo inicial de `ui/`:** botón e icon button, campo de formulario (input, select, número con stepper), tarjeta, elemento de lista, chip/badge, avatar, tabs y control segmentado, hoja inferior (*bottom sheet*, con CDK Overlay; en móvil sustituye a los diálogos), diálogo de confirmación, toast, skeleton, estado vacío, cuenta atrás, barra superior y navegación (inferior en móvil, lateral en escritorio).
- **FE-19 Catálogo inicial de `shared/`:** chip de posición, tarjeta de jugador (foto, dorsal, posición y valor), input de valoración (picas 0–4 o nota 0–10, según el formato de la ventana), marcador de puntos (con signo y color), vista de campo de la alineación (con alternativa en lista; seleccionar tocando, sin arrastrar obligatorio) y pipes `bfDate`, `bfMoney` y `bfPoints`.
- **FE-20 Catálogo vivo.** Cada componente de `ui/` aparece en `/_ui` (solo en desarrollo) con todos sus estados y en los dos temas.

#### 3.3.5 Tipado

- **FE-21 TypeScript estricto:** `strict`, `noUncheckedIndexedAccess`, `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch` y `noPropertyAccessFromIndexSignature`; en Angular, `strictTemplates`, `strictInjectionParameters`, `strictInputAccessModifiers` y `typeCheckHostBindings`.
- **FE-22 Prohibido por lint:** `any` (explícito y los `no-unsafe-*`), aserción no nula `!`, `as` salvo `as const`, y `switch` no exhaustivo sobre uniones (`switch-exhaustiveness-check`).
- **FE-23 Enums del backend** como uniones de strings generadas; sus textos, en `Record<Position, string>` y similares, para que un valor nuevo en el backend rompa la compilación tras regenerar.

#### 3.3.6 Estilo visual

- **FE-24 Tokens de diseño** en CSS custom properties (`--bf-color-*`, `--bf-space-*`, `--bf-radius-*`, `--bf-font-*`, `--bf-shadow-*`) definidas en `ui/styles/`. En los componentes no hay colores ni medidas literales (Stylelint).
- **FE-25 Paleta.** Se usan los seis colores del usuario, cada uno con su escala de tonos (50–900, generada en OKLCH):

  | Color | Rol |
  | --- | --- |
  | `#00A375` verde | Marca y acción principal; también éxito y puntos positivos |
  | `#7A86EE` lavanda | Secundario: selección, enlaces y foco |
  | `#E2794E` terracota | Posición portero |
  | `#009DC2` azul | Posición defensa |
  | `#84941D` oliva | Posición medio |
  | `#DE5EA5` rosa | Posición delantero |

  Se añaden neutros cálidos (fondos, superficies, bordes y texto), un rojo de error y de puntos negativos, y un ámbar de aviso (plazos a punto de vencer).

  Sobre blanco, los seis tonos base dan unos 3:1: valen para fondos, iconos y elementos grandes, pero no para texto. En tema claro, el texto sobre ellos va en casi negro (6–7:1) y el texto de color usa los tonos 700–800. En tema oscuro, los tonos base sí sirven como texto sobre la superficie (≥ 5,5:1 sobre `#121212`).
- **FE-26 Tema claro y oscuro** con los mismos tokens: sigue a `prefers-color-scheme` y tiene un interruptor manual que se guarda en `localStorage` y se aplica antes del primer pintado.
- **FE-27 Tipografía.** Barlow Condensed para títulos, marcadores y cifras (con números tabulares); Barlow para el texto. Servidas desde el propio dominio (RGPD: nada de Google Fonts). Escala tipográfica fija en tokens; inputs con 16 px como mínimo (para que iOS no haga zoom).
- **FE-28 Dirección visual.** La identidad (tipografía, escala y composición de las pantallas clave) se cierra en la primera tarea de UI siguiendo la skill `.agents/skills/frontend-design`, y se documenta en `ui/README.md`.

#### 3.3.7 Mobile first

- **FE-29 Diseño en móvil primero.** Puntos de corte: base (< 600 px), `md` (≥ 600) y `lg` (≥ 1024, donde aparece la navegación lateral y las dos columnas). Media queries `min-width` siempre.
- **FE-30 Ergonomía.** La acción principal, abajo, al alcance del pulgar. Navegación inferior con 4–5 destinos como máximo (inicio de la jornada, alineación, valorar, clasificación y liga). Respeta las *safe areas* (`env(safe-area-inset-*)`) y usa `dvh`. Inputs con `type` e `inputmode` correctos. Nada que solo funcione con hover, y todo gesto tiene una alternativa con botón.
- **FE-31 Sin conexión.** Sin modo offline: si se pierde la red, aparece un aviso y se deshabilitan los envíos.

#### 3.3.8 Calidad

- **FE-32 ESLint** (flat config): `angular-eslint` (TypeScript y plantillas, incluidas las reglas de accesibilidad), `typescript-eslint` `strictTypeChecked` y `stylisticTypeChecked`, `eslint-plugin-boundaries` (FE-02) y `eslint-config-prettier`. **Prettier** formatea TypeScript, HTML, SCSS y JSON. CI ejecuta `lint` y `format:check`; un aviso de lint es un error.
- **FE-33 Tests unitarios y de componentes** con Vitest y Angular Testing Library. Obligatorios: cada componente de `ui/` y `shared/` (estados y accesibilidad básica), cada servicio de API (con `HttpTestingController`), las funciones puras (mapeos, fechas, formatos) y los guards. Nombres de `describe`/`it` en inglés.
- **FE-34 E2E** con Playwright en viewport móvil (Chromium y WebKit), contra el backend real con Postgres en contenedor y un login de pruebas (§5). Cubren la jornada completa: crear liga e invitar, plantilla, partido y acta, alineación, valorar, cierre y clasificación. Cada pantalla pasa `@axe-core/playwright` sin violaciones.
- **FE-35 Scripts npm:** `start` (con proxy a `localhost:8080` para `/api`, `/oauth2` y `/login`), `build`, `lint`, `format`, `format:check`, `test`, `e2e`, `api:types` y `i18n:extract`.

#### 3.3.9 Mapa de pantallas

| Pantalla | Quién | RF | Endpoints |
| --- | --- | --- | --- |
| Login | Todos | RF-01 | `/oauth2/authorization/google`, `GET /api/me` |
| Mis ligas, crear liga | Todos | RF-05, RF-08 | `GET /api/me/leagues`, `POST /api/leagues` |
| Invitación | Todos | RF-06 | `GET /api/invitations/{token}`, `POST …/join` |
| Inicio de la liga (próximo partido, plazos y avisos) | Miembro | RF-25, RF-17 | `matches`, `lineup`, `ratings` |
| Alineación | Miembro | RF-24–29 | `GET/PUT /lineup`, `GET /lineups/{matchId}` |
| Valoraciones (listado y partido) | Miembro | RF-17–21 | `GET /ratings`, `GET/PUT /ratings/{matchId}` |
| Clasificación (acumulada y por jornada) | Miembro | RF-32 | `GET /standings`, `GET /standings/{matchId}` |
| Historial de un manager | Miembro | RF-33 | `GET /managers/{userId}/history` |
| Plantilla y ficha del jugador | Miembro | RF-11 | `GET /players`, `GET /players/{id}`, `GET /players/{id}/points` |
| Liga: miembros, salir | Miembro | RF-08 | `GET /members`, `DELETE /members/me` |
| Perfil y cerrar sesión | Todos | RF-02, RF-04 | `GET /api/me`, `POST /api/logout` |
| Admin · parámetros y tabla de puntos | Admin | RF-07, RF-15 | `GET/PUT /settings` |
| Admin · invitación y miembros | Admin | RF-06 | `POST/DELETE /invitation`, `DELETE /members/{id}` |
| Admin · jugadores y foto | Admin | RF-09, RF-10 | `POST/PUT/DELETE /players…`, `…/photo/upload`, `PUT/DELETE …/photo` |
| Admin · partidos, acta, cierre y auditoría | Admin | RF-12–16 | `/matches…`, `PUT …/sheet`, `POST …/close`, `GET …/audit` |
| Admin · cancelar valoraciones de un manager | Admin | RF-41 | `PUT /ratings/{matchId}/exclusions/{userId}` |

### 3.4 *Stack técnico propuesto*

- Fila *Frontend*: «Angular (standalone, signals, zoneless) + Angular CDK y librería de componentes propia; SCSS con tokens; tipos generados del OpenAPI» | «Estáticos en CDN, mismas rutas».
- Fila nueva *Calidad del frontend*: «ESLint + Prettier, Vitest, Playwright» | «Igual».
- Frase de introducción: «… PostgreSQL y una PWA…» pasa a «… PostgreSQL y una web Angular…».

### 3.5 *Roadmap por fases*

- **Fase 0**, ítem nuevo: `- [ ] Esqueleto de frontend/: Angular, ESLint, Prettier, Vitest, tipos del OpenAPI, tokens y tema, shell con navegación, login y CI (FE-01 a FE-35)`.
- **Fase 2**, ítems nuevos antes de *Prueba real*:
  - `- [ ] Pantallas de manager: ligas e invitación, inicio, alineación, valoraciones, clasificaciones, historiales y plantilla (RF-01 a RF-11, RF-17 a RF-33)`
  - `- [ ] Pantallas de admin: parámetros y tabla de puntos, miembros e invitación, jugadores y foto, partidos y acta, cancelación de valoraciones (RF-06, RF-07, RF-09, RF-10, RF-12 a RF-16, RF-41)`
  - `- [ ] E2E de la jornada completa en móvil (FE-34)`
- **Fase 2**, ítem *Pantalla de valoración…*: quitar «la pantalla llega con el `frontend/`» y apuntar a *Pantallas de manager*.
- **Fase 3**, ítem *PWA instalable*: queda igual, con la nota «(aplazada de la Fase 0: primero la web móvil)».

### 3.6 *Decisiones abiertas*

Se añaden como decididas: plataforma sin PWA de momento; librería de componentes (CDK + propia, que cierra el «Angular Material o PrimeNG, por decidir» de `architecture.md`); tipos desde OpenAPI; claro y oscuro; textos preparados para traducir.

## 4. Cambios en otros documentos (mismo cambio)

- **`AGENTS.md`:** *Stack*: «Angular PWA (aún no existe `frontend/`)» pasa a «Angular web mobile first (aún no existe `frontend/`)». Mapa: añadir `docs/agents/frontend.md`. Reglas core: añadir una línea que remita a FE-xx para el front.
- **`docs/agents/architecture.md`:** fila *Frontend* del stack; estructura `frontend/ # Angular (web mobile first)`; sección corta *Capas del frontend* con FE-01 y FE-02.
- **`docs/agents/frontend.md` (nuevo):** la guía para agentes del front: resumen de FE-01 a FE-35, cómo añadir un endpoint (regenerar spec y tipos, alias, método en el servicio) y cómo añadir un componente (buscar en el catálogo, `ui/` o `shared/`, test y `/_ui`). Sustituye la sección *Frontend* de `conventions.md`, que pasa a enlazarla.
- **`docs/agents/testing.md`:** apartado de frontend (FE-33, FE-34).
- **`docs/agents/status.md`:** nota de que existe el plan 10 y de que el siguiente paso es el plan 11.
- `commands.md` no cambia hasta que exista `frontend/` (plan 11).

## 5. Trabajo de backend que se desprende (plan 11, no este cambio)

- **Export del spec:** un test que genera `api/openapi.json` desde `/v3/api-docs` y falla si difiere del versionado, como `migration-checksums.txt`.
- **Códigos de error en el spec:** un `OpenApiCustomizer` publica el esquema `ProblemDetail` con `code` como enum (la unión de `RatingError`, `LineupError`, `ScoringError`… y los que vengan) y `violations`, y lo referencia en las respuestas `400` y `409`.
- **Vuelta tras el login:** `oauth2Login` redirige a `/` al terminar (el front recupera la ruta guardada) y un login fallido va a `/login?error`.
- **Login de pruebas para E2E:** endpoint que abre sesión con un usuario de prueba, **solo con un perfil `e2e`** y desactivado por defecto, con un test que verifique que en producción no existe.
- **CORS del bucket** (S3/SeaweedFS) para el `PUT` a la URL prefirmada desde el navegador.
- **Desarrollo local:** `http://localhost:4200/login/oauth2/code/google` como redirect URI en Google; documentarlo en `commands.md`.
- **Caddy:** estáticos del front con *fallback* a `index.html`, `/api`, `/oauth2` y `/login` al backend, y cabeceras de seguridad y CSP (encaja con el ítem pendiente de la Fase 0).

## 6. Orden de ejecución

| # | Tarea | Resultado | Hecho |
| --- | --- | --- | --- |
| 1 | Revisar este plan con el usuario y cerrar las decisiones marcadas como *Propuesta* (§2) | Decisiones cerradas | ✅ |
| 2 | `requirements.md`: visión y alcance, RNF-01 y RNF-12 a RNF-16 (§3.1, §3.2) | Plataforma y RNF del front | ✅ |
| 3 | `requirements.md`: sección *Frontend* con FE-01 a FE-35 y mapa de pantallas (§3.3) | Reglas del front | ✅ |
| 4 | `requirements.md`: stack, roadmap y decisiones (§3.4 a §3.6) | Roadmap con el front | ✅ |
| 5 | `AGENTS.md`, `architecture.md`, `frontend.md` nuevo, `conventions.md`, `testing.md`, `status.md` (§4) | Guías de agentes coherentes | |
| 6 | Redactar `docs/11-frontend-foundation-plan.md` (esqueleto, §5 y primeras pantallas) | Siguiente plan listo | |

Las tareas 2 a 5 son un único commit `docs: add frontend requirements (RNF-01, RNF-12 to RNF-16)`.

## 7. Riesgos y notas

- **Librería propia:** cuesta más al principio que Material. Se mitiga con el CDK para lo difícil (overlay, foco, a11y) y con un catálogo inicial corto (FE-18); lo demás se añade cuando lo pida una pantalla.
- **Contraste de la paleta:** los seis tonos base no llegan a 4,5:1 con texto blanco. Por eso la regla de FE-25 (texto oscuro sobre color en tema claro, tonos 700–800 para texto de color) se comprueba con axe en E2E.
- **Spec versionado:** cada cambio de API obliga a regenerar dos ficheros. Es buscado: el diff del PR enseña el cambio de contrato, y CI impide que se olvide.
- **Cuenta atrás con el reloj del cliente:** un móvil con la hora mal puede enseñar un plazo erróneo. Es solo informativo (FE-09); si molesta, el desfase se calcula con la cabecera `Date` de las respuestas.
- **E2E con login real de Google:** no se puede automatizar; de ahí el login de pruebas por perfil (§5), que debe ser imposible de activar en producción.
- **`@angular/localize` con un solo idioma:** algo de ceremonia (ids, extracción) a cambio de no tener que tocar todas las plantillas si un día se traduce.
- **Sin PWA:** en Fase 3 habrá que añadir manifest y service worker sin romper la CSP ni la caché de `index.html`; es aditivo.
