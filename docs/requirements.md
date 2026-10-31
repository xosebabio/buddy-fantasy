# Fantasy Liga de Peñas — Requisitos y Roadmap

Oct 26, 2026 · @Pepe

## Visión y alcance

Una web en Angular, pensada para el móvil, de fantasy para una liga de peñas de domingo. Un administrador registra un único equipo real, sus partidos, resultados y estadísticas. Los managers puntúan a los jugadores tras cada partido y compiten con sus alineaciones en una clasificación global.

- **Dentro del alcance (v1):** una liga por administrador, un equipo real por liga, valoraciones de managers, puntuación combinada, alineaciones con límite de cambios, valor de mercado dinámico, clasificación, cuentas de usuario y notificaciones.
- **Fuera del alcance (v1):** apps nativas, pagos, chat entre usuarios, varias competiciones reales por liga y compraventa de jugadores entre managers. De momento, también la PWA instalable y el modo offline (se valoran en la Fase 3 con Web Push).
- **Objetivo inicial:** que funcione para el grupo de amigos en un VPS barato, con una arquitectura que permita migrar a cloud sin reescribir.

## Roles y permisos

Los roles se asignan **por liga**, no de forma global: un mismo usuario puede administrar una liga y ser manager en otra.

| Acción | Administrador | Manager |
| --- | --- | --- |
| Crear y configurar la liga | Sí | No |
| Invitar o expulsar managers | Sí | No |
| Dar de alta jugadores y su valor inicial | Sí | No |
| Crear partidos, resultados y estadísticas | Sí | No |
| Valorar jugadores tras un partido | Sí, opcional (juega como manager) | Sí, opcional |
| Cancelar las valoraciones de un manager en un partido | Sí, con la ventana abierta | No |
| Editar su alineación | Si también juega como manager | Sí |
| Ver clasificación, puntos y valores | Sí | Sí |
| Recalcular o corregir una jornada | Sí | No |

## Requisitos funcionales por módulo

Cada requisito lleva un código (RF-xx) para poder referenciarlo en tickets y en el roadmap.

### Usuarios y autenticación

- RF-01: Registro e inicio de sesión (login social con Google y/o email + contraseña).
- RF-02: Perfil de usuario con nombre visible y avatar.
- RF-03: Recuperación de contraseña, si se usa login con email.
- RF-04: Cierre de sesión y gestión de sesiones activas.

### Ligas

- RF-05: Un usuario puede crear una liga y queda como su administrador.
- RF-06: El administrador invita managers mediante enlace o código de invitación.
- RF-07: El administrador configura los parámetros de la liga (ver *Parámetros configurables*).
- RF-08: Un usuario puede pertenecer a varias ligas y cambiar entre ellas.

### Jugadores

- RF-09: El administrador da de alta, edita y da de baja jugadores del equipo (nombre, dorsal, posición obligatoria —portero, defensa, medio o delantero— y foto opcional).
- RF-10: El administrador asigna un valor inicial a cada jugador.
- RF-11: Cada jugador tiene ficha con historial de puntos por jornada y evolución de su valor.

### Partidos y estadísticas

- RF-12: El administrador crea partidos (rival, fecha y hora, local o visitante, jornada).
- RF-13: El administrador registra el resultado y la convocatoria: quién jugó y, opcionalmente, minutos.
- RF-14: El administrador registra estadísticas por jugador (goles, goles de penalti, asistencias, goles en propia y tarjetas).
- RF-15: Cada acción suma o resta puntos según la tabla de puntuación por acción (abajo), estilo Biwenger. Los goles puntúan según la posición del jugador. La tabla viene precargada y el administrador puede ajustarla.
- RF-16: El administrador puede corregir datos de un partido y forzar el recálculo de la jornada.

#### Tabla de puntuación por acción

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

Reglas confirmadas: el gol de penalti vale +3 sea cual sea la posición, y la doble amarilla vale −3 en total, sin sumar las dos amarillas. El penalti parado (+3) y el fallado (−2) puntúan igual en cualquier posición.

### Valoraciones

- RF-17: Al cerrar el partido se abre una ventana de valoración (por defecto 24 h, configurable).
- RF-18: Cada manager puede valorar, de forma opcional, a los jugadores que jugaron.
- RF-19: Formato de valoración configurable por liga: nota decimal (0–10) o picas al estilo Biwenger (0–4).
- RF-20: Un manager puede modificar sus valoraciones mientras la ventana siga abierta.
- RF-21: Las valoraciones individuales son anónimas para el resto; solo se publica la media.
- RF-22: Al cerrar la ventana, la media de valoraciones se convierte en puntos (ver *Conversión de la valoración a puntos*), se redondea al entero más cercano (mitad hacia arriba) y se suma a los puntos por acciones. No hay reparto porcentual entre ambas partes.
- RF-23: Si un jugador no recibe ninguna valoración, o recibe menos que el mínimo de votos de la liga, cuenta solo con los puntos por acciones (o con una nota por defecto configurable).
- RF-41: Mientras la ventana siga abierta, el administrador puede cancelar las valoraciones de un manager en un partido (por ejemplo, si sobrevalora a sus jugadores o vota sin haber visto el partido). Se borran todas sus valoraciones de ese partido y queda excluido de valorarlo; puede cancelarse también a quien aún no ha votado. Es definitivo. El administrador no ve los votos individuales (RF-21 se mantiene); el manager excluido ve que lo está, sin motivo.

#### Conversión de la valoración a puntos

| Formato | Conversión por defecto | Ejemplos |
| --- | --- | --- |
| Picas (0–4) | 0 picas = −2, 1 = +2, 2 = +6, 3 = +10, 4 = +14 (puntos = 4 × picas − 2) | Media 2,5 picas → 8 puntos; media 2,6 → 8,4 → 8 puntos |
| Nota decimal (0–10) | 1 punto por punto de nota | Media 7,5 → 8 puntos; media 7,4 → 7 puntos |

La conversión se aplica a la media de la jornada (no a cada voto) y el redondeo al resultado en puntos. Un jugador sin valoraciones suma 0 puntos por valoración, no los −2 de 0 picas, salvo que la liga tenga nota por defecto.

### Alineaciones

- RF-24: Cada manager tiene una alineación de N jugadores (N configurable, con o sin formación por posiciones).
- RF-25: La alineación se puede editar hasta X horas antes del partido (configurable); después queda bloqueada.
- RF-26: Un manager puede hacer como máximo Y cambios por jornada (configurable).
- RF-27: La alineación bloqueada es la que puntúa en esa jornada.
- RF-28: Si un manager no toca su alineación, se mantiene la de la jornada anterior.
- RF-29: Presupuesto máximo (activado por defecto, desactivable por liga) de la alineación según el valor de los jugadores.

### Puntuación y clasificación

- RF-30: Puntos del jugador en la jornada = puntos por acciones + puntos por valoración.
- RF-31: Puntos del manager en la jornada = suma de los puntos de sus jugadores alineados.
- RF-32: Clasificación global acumulada y clasificación por jornada.
- RF-33: Historial de jornadas por manager, con el detalle de puntos de cada jugador.

### Valor de mercado

- RF-34: Tras cerrar cada jornada, el valor de cada jugador sube o baja según su rendimiento.
- RF-35: Algoritmo inicial propuesto: Δvalor = k × (puntos − media de la jornada) × valor, con un tope de ±10 % por jornada y un valor mínimo.
- RF-36: Gráfica de la evolución del valor en la ficha del jugador.

### Notificaciones

- RF-37: Push (Web Push en la PWA) y email como respaldo.
- RF-38: Avisos: valoraciones abiertas, valoraciones a punto de cerrar, alineación a punto de bloquearse, puntos de la jornada publicados, invitación a una liga.
- RF-39: Cada usuario puede activar o desactivar cada tipo de aviso.
- RF-40: Centro de notificaciones dentro de la app.

## Parámetros configurables

Todos se guardan por liga y los edita el administrador. Los valores por defecto son propuestas.

| Parámetro | Por defecto | Notas |
| --- | --- | --- |
| Duración de la ventana de valoración | 24 h tras el partido | Empieza al cerrar el partido |
| Formato de valoración | Picas (0–4) | Alternativa: nota decimal 0–10 |
| Conversión de la valoración a puntos | Picas: −2, 2, 6, 10, 14; nota: 1 punto por punto | Ver *Conversión de la valoración a puntos*. Redondeo al entero, mitad hacia arriba. Se suma a los puntos por acciones, sin porcentajes |
| Mínimo de votos para que cuente la valoración | 1 | Por debajo, el jugador cuenta como sin valoraciones (RF-23) |
| Nota por defecto sin valoraciones | Ninguna (0 puntos) |  |
| Bloqueo de la alineación | 2 h antes del partido |  |
| Cambios máximos por jornada | 2 |  |
| Tamaño de la alineación | 7 | Típico en fútbol 7; ajustar al formato de la peña |
| Formación por posiciones | Desactivada |  |
| Presupuesto de la alineación | Activado (100 M) | El admin puede desactivarlo |
| Puntos por acción | Tabla editable | Precargada con la tabla estilo Biwenger; gol según posición |
| Factor k del valor de mercado | 0,05 |  |
| Tope de variación del valor por jornada | ±10 % |  |

## Requisitos no funcionales

- RNF-01 **Plataforma:** web responsive en Angular, *mobile first*: se diseña primero para 360 × 640 y se adapta a tableta y escritorio. Sin apps nativas. La instalación como PWA queda para la Fase 3.
- RNF-02 **Seguridad:** HTTPS obligatorio, autorización por rol y liga en cada endpoint, contraseñas con hash (bcrypt o argon2) y protección CSRF.
- RNF-03 **Integridad:** todos los plazos (bloqueo de alineación, cierre de valoraciones) se aplican en el servidor, nunca solo en el cliente. Los jobs programados son idempotentes y no se ejecutan dos veces (ShedLock).
- RNF-04 **Auditoría:** registro de cambios del administrador en partidos y estadísticas para poder recalcular.
- RNF-05 **Zona horaria:** fechas en UTC en base de datos y en Europe/Madrid en la interfaz.
- RNF-06 **Portabilidad:** la app se empaqueta en contenedores y se configura por variables de entorno, sin estado en disco, para migrar del VPS a cloud sin tocar código.
- RNF-07 **Backups:** copia diaria de PostgreSQL fuera del servidor, con restauración probada.
- RNF-08 **Observabilidad:** logs estructurados, health checks (Actuator) y alertas básicas de caída.
- RNF-09 **Rendimiento:** respuesta por debajo de 300 ms en las pantallas principales con unos 50 usuarios concurrentes.
- RNF-10 **Privacidad:** cumplimiento básico del RGPD (consentimiento, borrado de cuenta, mínimos datos personales).
- RNF-11 **Calidad:** tests de la lógica de puntuación, plazos y valor de mercado, más CI que construya y despliegue la imagen.
- RNF-12 **Accesibilidad:** WCAG 2.2 AA: contraste de 4,5:1 en texto y 3:1 en elementos gráficos, en los dos temas; objetivos táctiles de al menos 44 × 44 px; foco visible; todo usable con teclado y lector de pantalla; respeta `prefers-reduced-motion`.
- RNF-13 **Rendimiento del cliente:** rutas cargadas bajo demanda por feature; presupuestos de `angular.json` (bundle inicial: aviso a 400 kB y error a 600 kB, sin comprimir); LCP < 2,5 s en un móvil medio con 4G.
- RNF-14 **Compatibilidad:** las dos últimas versiones de Safari iOS, Chrome Android y los navegadores de escritorio principales (Chrome, Firefox, Safari, Edge).
- RNF-15 **Internacionalización:** textos marcados con `@angular/localize` (`i18n` en plantillas, `$localize` en código), con id explícito; idioma de origen `es`. Fechas, números e importes con el locale `es-ES` y la zona `Europe/Madrid` (RNF-05).
- RNF-16 **Seguridad en el cliente:** la sesión vive solo en la cookie `HttpOnly` del backend (nada en `localStorage`); CSRF con la cookie `XSRF-TOKEN` y la cabecera `X-XSRF-TOKEN`; sin `innerHTML` con contenido de usuario ni `bypassSecurityTrust*`; CSP estricta servida por Caddy (build con `autoCsp`).

## Frontend

Reglas de arquitectura, código y diseño de `frontend/`. Cada una lleva un código (FE-xx) para citarla en commits y revisiones, como RF y RNF.

### Arquitectura

- FE-01 **Estructura:** `frontend/src/app/` en cuatro capas:

  ```
  core/       # singletons: auth, http (interceptores), api (tipos generados y un servicio por módulo del backend), layout (shell y navegación), i18n, time
  ui/         # sistema de diseño: tokens, estilos globales y componentes genéricos sin dominio (bf-button, bf-sheet…)
  shared/     # piezas de dominio reutilizables: chip de posición, tarjeta de jugador, input de valoración, pipes de puntos e importes
  features/   # una carpeta por módulo del backend: identity, league, squad, match, lineup, rating, scoring
  ```

- FE-02 **Límites entre capas**, comprobados con `eslint-plugin-boundaries`, igual que Modulith en el backend:
  - `ui` no importa nada de la app (solo Angular y CDK).
  - `shared` puede importar `ui` y los tipos de `core/api`.
  - Una feature usa `core`, `ui` y `shared`, **nunca otra feature**. Para pasar a otra feature se navega por ruta.
- FE-03 **Rutas:** lazy por feature; la liga activa va en la URL (`/leagues/:leagueId/...`) y es lo que permite cambiar de liga (RF-08). Los parámetros llegan a los componentes como `input()` (`withComponentInputBinding`). Las pantallas de admin están en su feature bajo `admin/` y llevan `adminGuard`.
- FE-04 **Páginas y componentes:** las páginas (`*-page`) son las únicas que inyectan servicios de API y orquestan. Los componentes de `ui/` y `shared/` son de presentación: solo `input()`, `output()` y `model()`.

### Contrato con el backend

- FE-05 **OpenAPI es la fuente de los tipos:** un test del backend escribe `api/openapi.json` (versionado). `npm run api:types` genera `core/api/schema.d.ts` con `openapi-typescript` (también versionado). CI regenera los dos y falla si cambian.
- FE-06 **Nada de tipos del backend a mano:** cada servicio de `core/api/<módulo>/` exporta alias con el mismo nombre que el DTO (`type PlayerResponse = components['schemas']['PlayerResponse']`). Las features no redeclaran formas del backend: si necesitan un modelo de vista, lo derivan (`Pick`, `&`) con una función de mapeo probada.
- FE-07 **Un servicio de API por módulo del backend** (`LeagueApi`, `SquadApi`, `MatchApi`, `LineupApi`, `RatingApi`, `ScoringApi`, `IdentityApi`): un método por endpoint, tipado con `paths` del schema, URLs relativas (`/api/...`). Sin lógica de negocio.
- FE-08 **Errores:** el backend responde `ProblemDetail` (RFC 9457) con `code` (`ErrorCode`) y `violations`. Un interceptor resuelve los globales: `401` lleva al login guardando la ruta de vuelta, `403` a «Sin acceso», `404` a «No encontrado», y `5xx` o un fallo de red muestran un aviso con reintento. Los `400` y `409` los trata la pantalla: los `violations`, en su campo del formulario; el `code`, con su mensaje de un `Record<ErrorCode, string>` exhaustivo (por eso el spec publica los códigos).
- FE-09 **Plazos y límites en el servidor:** el cliente solo refleja (RNF-03): las cuentas atrás de bloqueo y de cierre son orientativas, y un `409` refresca la vista y lo explica. Ningún límite ni valor por defecto de negocio se repite en el front como constante: si se quiere validar antes de enviar, el valor se lee del API; si el API no lo da, valida solo el servidor.
- FE-10 **Autenticación:** al arrancar, `GET /api/me` decide si hay sesión. El login es una navegación completa a `/oauth2/authorization/google` y la ruta de vuelta (por ejemplo, la de una invitación) se guarda en `sessionStorage`: el backend siempre vuelve a `/` (o a `/login?error` si el login falla) y el front la recupera. El logout es `POST /api/logout`. Los guards (`authGuard`, `memberGuard`, `adminGuard`) leen el rol por liga de `GET /api/me/leagues`; solo ocultan, porque la autorización la hace el backend.
- FE-11 **Anonimato:** ninguna pantalla muestra ni deduce votos individuales de otros (RF-21): solo media y número de votos al cerrar, y los votos propios.

### Estado y datos

- FE-12 **Signals:** el estado del servidor se lee con `httpResource`/`resource` dependiente de signals (liga, partido); las escrituras son métodos del servicio y, al terminar, recargan los recursos afectados. Sin actualizaciones optimistas: casi toda escritura depende de reglas del servidor (plazos, cambios, presupuesto).
- FE-13 **Estado global mínimo:** un `SessionStore` (usuario y ligas con rol) y la preferencia de tema. Todo lo demás vive en la página que lo usa.
- FE-14 **Estados de cada vista:** toda vista asíncrona resuelve explícitamente carga (*skeleton*), vacío, error con reintento y contenido.
- FE-15 **Formularios:** Reactive Forms tipados (`NonNullableFormBuilder`). Los errores del servidor se pintan en su campo.

### Componentes reutilizables

- FE-16 **Reutilizar antes de crear:** antes de escribir un componente se busca en `ui/` y `shared/`. Si un patrón de interfaz aparece en dos pantallas, se extrae.
- FE-17 **Reglas de componente:** standalone, `ChangeDetectionStrategy.OnPush`, `inject()`, prefijo `bf-`, guía de estilo oficial de Angular vigente. Accesibles de serie: rol, etiquetas y foco gestionados con `@angular/cdk/a11y`.
- FE-18 **Catálogo inicial de `ui/`:** botón e icon button, campo de formulario (input, select, número con stepper), tarjeta, elemento de lista, chip/badge, avatar, tabs y control segmentado, hoja inferior (*bottom sheet*, con CDK Overlay; en móvil sustituye a los diálogos), diálogo de confirmación, toast, skeleton, estado vacío, cuenta atrás, barra superior y navegación (inferior en móvil, lateral en escritorio).
- FE-19 **Catálogo inicial de `shared/`:** chip de posición, tarjeta de jugador (peto con el dorsal, nombre, posición y valor; sin foto en v1), input de valoración (picas 0–4 o nota 0–10, según el formato de la ventana), marcador de puntos (con signo y color), vista de campo de la alineación (con alternativa en lista; seleccionar tocando, sin arrastrar obligatorio) y pipes `bfDate`, `bfMoney` y `bfPoints`.
- FE-20 **Catálogo vivo:** cada componente de `ui/` aparece en `/_ui` (solo en desarrollo) con todos sus estados y en los dos temas.

### Tipado

- FE-21 **TypeScript estricto:** `strict`, `noUncheckedIndexedAccess`, `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch` y `noPropertyAccessFromIndexSignature`; en Angular, `strictTemplates`, `strictInjectionParameters`, `strictInputAccessModifiers` y `typeCheckHostBindings`.
- FE-22 **Prohibido por lint:** `any` (explícito y los `no-unsafe-*`), aserción no nula `!`, `as` salvo `as const`, y `switch` no exhaustivo sobre uniones (`switch-exhaustiveness-check`).
- FE-23 **Enums del backend** como uniones de strings generadas; sus textos, en `Record<Position, string>` y similares, para que un valor nuevo en el backend rompa la compilación tras regenerar.

### Estilo visual

- FE-24 **Tokens de diseño** en CSS custom properties (`--bf-color-*`, `--bf-space-*`, `--bf-radius-*`, `--bf-font-*`, `--bf-shadow-*`) definidas en `ui/styles/`. En los componentes no hay colores ni medidas literales (Stylelint).
- FE-25 **Paleta:** seis colores base, cada uno con su escala de tonos (50–900, generada en OKLCH):

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
- FE-26 **Tema claro y oscuro** con los mismos tokens: sigue a `prefers-color-scheme` y tiene un interruptor manual que se guarda en `localStorage` y se aplica antes del primer pintado.
- FE-27 **Tipografía:** Barlow Condensed para títulos, marcadores y cifras (con números tabulares); Barlow para el texto. Servidas desde el propio dominio con `@fontsource` (RGPD: nada de Google Fonts). Escala tipográfica fija en tokens; inputs con 16 px como mínimo (para que iOS no haga zoom).
- FE-28 **Dirección visual:** la identidad (tipografía, escala y composición de las pantallas clave) se cierra en la primera tarea de UI siguiendo la skill `.agents/skills/frontend-design`, y se documenta en `ui/README.md`.

### Mobile first

- FE-29 **Diseño en móvil primero:** puntos de corte base (< 600 px), `md` (≥ 600) y `lg` (≥ 1024, donde aparece la navegación lateral y las dos columnas). Media queries `min-width` siempre.
- FE-30 **Ergonomía:** la acción principal, abajo, al alcance del pulgar. Navegación inferior con 4–5 destinos como máximo (inicio de la jornada, alineación, valorar, clasificación y liga). Respeta las *safe areas* (`env(safe-area-inset-*)`) y usa `dvh`. Inputs con `type` e `inputmode` correctos. Nada que solo funcione con hover, y todo gesto tiene una alternativa con botón.
- FE-31 **Sin conexión:** sin modo offline: si se pierde la red, aparece un aviso y se deshabilitan los envíos.

### Calidad

- FE-32 **Lint y formato:** ESLint (flat config) con `angular-eslint` (TypeScript y plantillas, incluidas las reglas de accesibilidad), `typescript-eslint` `strictTypeChecked` y `stylisticTypeChecked`, `eslint-plugin-boundaries` (FE-02) y `eslint-config-prettier`. Prettier formatea TypeScript, HTML, SCSS y JSON. CI ejecuta `lint` y `format:check`; un aviso de lint es un error.
- FE-33 **Tests unitarios y de componentes** con Vitest y Angular Testing Library. Obligatorios: cada componente de `ui/` y `shared/` (estados y accesibilidad básica), cada servicio de API (con `HttpTestingController`), las funciones puras (mapeos, fechas, formatos) y los guards. Nombres de `describe`/`it` en inglés.
- FE-34 **E2E** con Playwright en viewport móvil (Chromium y WebKit), contra el backend real con Postgres en contenedor y un login de pruebas que solo existe con un perfil `e2e`. Cubren la jornada completa: crear liga e invitar, plantilla, partido y acta, alineación, valorar, cierre y clasificación. Cada pantalla pasa `@axe-core/playwright` sin violaciones.
- FE-35 **Scripts npm:** `start` (con proxy a `localhost:8080` para `/api`, `/oauth2` y `/login/oauth2`, sin reescribir `Host`; `/login` es la pantalla de Angular), `build`, `lint`, `format`, `format:check`, `test`, `e2e`, `api:types` y `i18n:extract`.

### Mapa de pantallas

Las rutas del API cuelgan de `/api/leagues/{id}` salvo que empiecen por `/api` o `/oauth2`.

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

## Stack técnico propuesto

Un monolito modular en Spring Boot con PostgreSQL y una web Angular, desplegado con Docker Compose en un VPS y preparado para pasar a cloud.

| Capa | Fase 1 (VPS) | Fase 2 (cloud) |
| --- | --- | --- |
| Backend | Spring Boot 3 + Spring Modulith, Java 21+ | La misma imagen Docker |
| Base de datos | PostgreSQL en contenedor + Flyway | PostgreSQL gestionado (RDS, Cloud SQL o Azure) |
| Autenticación | Spring Security + OAuth2 login (Google) | Keycloak o un IdP gestionado |
| Jobs | @Scheduled + ShedLock | Igual, o un scheduler gestionado |
| Frontend | Angular (standalone, signals, zoneless) + Angular CDK y librería de componentes propia; SCSS con tokens; tipos generados del OpenAPI | Estáticos en CDN, mismas rutas |
| Calidad del frontend | ESLint + Prettier, Vitest, Playwright | Igual |
| Notificaciones | webpush-java + SMTP (Brevo, Resend…) | Igual, o una cola (SQS o Pub/Sub) |
| Proxy y HTTPS | Caddy | Load balancer del proveedor |
| Despliegue | Docker Compose + GitHub Actions | Cloud Run, ECS Fargate o Container Apps |
| Backups | pg\_dump diario a almacenamiento S3-compatible | Snapshots gestionados |

Si más adelante se quiere app móvil, hay dos caminos con Angular: **Capacitor** (o Ionic), que empaqueta la misma web casi sin cambios, o **NativeScript con Angular**, que da UI nativa pero obliga a reescribir las vistas. Para reutilizar el máximo de código, Capacitor es el paso natural.

Módulos del monolito: `identity`, `league`, `squad` (jugadores), `match` (partidos y estadísticas), `rating`, `lineup`, `scoring`, `market` y `notification`. Se comunican por eventos de dominio (por ejemplo, `MatchClosed`, `RatingWindowClosed` y `MatchdayScored`), lo que facilita extraer un módulo a servicio si algún día hace falta.

## Roadmap por fases

Con dedicación parcial, la versión jugable con los amigos (fases 0–2) llega en unas 6–8 semanas, y la v1 completa (fases 0–4) en unas 10–13. Son estimaciones aproximadas.

&#91;embedded content: roadmap · 6 fases y sus puertas\]

Cada fase termina en una puerta: no se pasa a la siguiente hasta cumplir su criterio.

### Fase 0 · Fundamentos

- [ ] Repositorio, Spring Boot 3 con Spring Modulith y estructura de módulos
- [x] PostgreSQL con Flyway y modelo base (usuario, liga, membership)
- [x] Spring Security con login de Google y roles por liga
- [ ] Dockerfile, Docker Compose (app + Postgres + Caddy) y pipeline de CI
- [ ] VPS con dominio, HTTPS y backup diario de la base de datos
- [x] Esqueleto de `frontend/`: Angular, ESLint, Prettier, Vitest, tipos del OpenAPI, tokens y tema, shell con navegación, login y CI (FE-01 a FE-35)

### Fase 1 · Núcleo jugable

- [x] Crear liga, invitar managers por enlace y configurar parámetros (RF-05 a RF-08)
- [x] CRUD de jugadores con valor inicial (RF-09 a RF-11)
- [x] Partidos, convocatoria, resultado y estadísticas con su tabla de puntos (RF-12 a RF-16). `match` guarda y publica el acta; el cálculo de puntos de RF-15 lo hace *Cálculo de puntos por acción según posición y clasificaciones* (`scoring`)
- [x] Alineaciones: edición, bloqueo X horas antes y límite de cambios (RF-24 a RF-29). `lineup` bloquea las alineaciones y publica `LineupsLocked`; los puntos del manager (RF-31), incluido el 0 de una alineación fuera de presupuesto, los calcula *Cálculo de puntos por acción según posición y clasificaciones* (`scoring`)
- [x] Cálculo de puntos por acción según posición y clasificaciones (RF-30 a RF-33). `scoring` puntúa cada partido al tener el acta y las alineaciones bloqueadas, con la tabla de la liga congelada, y publica `MatchdayScored`; los puntos por valoración de RF-30 valen 0 hasta *Combinación de valoración y estadísticas al cierre* (Fase 2)

### Fase 2 · Valoraciones

- [x] Apertura y cierre de la ventana de valoración con job programado y ShedLock (RF-17)
- [x] Pantalla de valoración en picas o nota decimal, editable mientras siga abierta, y cancelación de las valoraciones de un manager por el admin (RF-18 a RF-21, RF-41). `rating` sirve los votos, las vistas y la cancelación por API (plan 09, tareas 4 y 4b); la pantalla, en *Pantallas de manager*
- [x] Combinación de valoración y estadísticas al cierre, con recálculo idempotente (RF-22, RF-23). Incluye adaptar los parámetros de `league`: sustituir `ratingPointsPerUnit` por una conversión por formato (picas −2/2/6/10/14, nota 1:1) y añadir el mínimo de votos. `scoring` suma los puntos por valoración al cerrar la ventana y recalcula con `RatingsCorrected` (plan 09)
- [ ] Pantallas de manager: ligas e invitación, inicio, alineación, valoraciones, clasificaciones, historiales y plantilla (RF-01 a RF-11, RF-17 a RF-33)
- [ ] Pantallas de admin: parámetros y tabla de puntos, miembros e invitación, jugadores y foto, partidos y acta, cancelación de valoraciones (RF-06, RF-07, RF-09, RF-10, RF-12 a RF-16, RF-41)
- [ ] E2E de la jornada completa en móvil (FE-34)
- [ ] Prueba real: una jornada con los amigos y ajuste de los pesos

### Fase 3 · Mercado y notificaciones

- [ ] Algoritmo de valor tras cada jornada y gráfica de evolución (RF-34 a RF-36)
- [ ] PWA instalable (manifest y service worker), aplazada de la Fase 0: primero la web móvil
- [ ] Web Push y email con preferencias por usuario (RF-37 a RF-40)

### Fase 4 · Pulido y operación

- [ ] Correcciones del administrador con auditoría y recálculo (RF-16, RNF-04). Hechas en Fase 1 las correcciones del acta con auditoría y el evento `MatchCorrected`, y el recálculo automático en `scoring` con la tabla congelada del partido; hecho en Fase 2 el recálculo en `rating` (los jugadores quitados del acta pierden sus votos y, con la ventana cerrada, `RatingsCorrected` recalcula los puntos; plan 09); falta el recálculo forzado por el admin (por ejemplo, para aplicar una tabla editada a jornadas pasadas)
- [ ] Logs, health checks y alertas de caída (RNF-08)
- [ ] Tests de puntuación, plazos y mercado; restauración de backup probada
- [ ] RGPD: consentimiento y borrado de cuenta (RNF-10)

### Fase 5 · Escalado a cloud

- [ ] Migrar a PostgreSQL gestionado y a contenedores gestionados
- [ ] Registro abierto y creación libre de ligas
- [ ] IdP gestionado o Keycloak, CDN para el frontend y colas para notificaciones

## Decisiones abiertas

- [x] ¿El administrador puede ser también manager en su propia liga? Tiene acceso a datos antes que nadie. Sí: el administrador siempre juega como manager en su liga (decidido).
- [x] ¿Picas o nota decimal por defecto? ¿Cómo se convierte la media en puntos? Picas por defecto; 0 picas = −2, 1 = 2, 2 = 6, 3 = 10, 4 = 14 puntos, y en nota decimal 1 punto por punto de nota. La media convertida se redondea al entero, mitad hacia arriba (decidido).
- [x] ¿Pueden dos managers alinear al mismo jugador? En una liga de un solo equipo parece imprescindible que sí. Sí, sin exclusividad (decidido); más adelante será un parámetro de la liga.
- [x] Papel del valor del jugador: límite de presupuesto de la alineación (RF-29), con el valor vigente al guardar y al bloquear (decidido). Si al bloquear supera el presupuesto, la alineación no puntúa esa jornada.
- [x] ¿Valoraciones con un mínimo de votos para contar, para evitar que una sola nota decida? Parámetro por liga, por defecto 1 (sin mínimo efectivo); por debajo, el jugador cuenta como sin valoraciones (decidido).
- [x] Login: solo Google (decidido en v1).
- [x] ¿PWA instalable desde el principio? No: primero una web Angular *mobile first*; la PWA llega en la Fase 3 con Web Push (decidido).
- [x] ¿Angular Material o PrimeNG? Ninguna: Angular CDK y una librería de componentes propia con tokens de diseño (FE-18, decidido).
- [x] Contrato entre frontend y backend: tipos generados del OpenAPI versionado y servicios propios por módulo (FE-05 a FE-07, decidido).
- [x] Tema: claro y oscuro, según el sistema y con interruptor manual (FE-26, decidido).
- [x] Idioma: interfaz en español, con los textos preparados para traducir con `@angular/localize` (RNF-15, decidido).

