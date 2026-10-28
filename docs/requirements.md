# Fantasy Liga de Peñas — Requisitos y Roadmap

Oct 26, 2026 · @Pepe

## Visión y alcance

Una web app en Angular (PWA) de fantasy para una liga de peñas de domingo. Un administrador registra un único equipo real, sus partidos, resultados y estadísticas. Los managers puntúan a los jugadores tras cada partido y compiten con sus alineaciones en una clasificación global.

- **Dentro del alcance (v1):** una liga por administrador, un equipo real por liga, valoraciones de managers, puntuación combinada, alineaciones con límite de cambios, valor de mercado dinámico, clasificación, cuentas de usuario y notificaciones.
- **Fuera del alcance (v1):** apps nativas, pagos, chat entre usuarios, varias competiciones reales por liga y compraventa de jugadores entre managers.
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

- RNF-01 **Plataforma:** web responsive en Angular, *mobile first*, instalable como PWA. Sin apps nativas.
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

## Stack técnico propuesto

Un monolito modular en Spring Boot con PostgreSQL y una PWA, desplegado con Docker Compose en un VPS y preparado para pasar a cloud.

| Capa | Fase 1 (VPS) | Fase 2 (cloud) |
| --- | --- | --- |
| Backend | Spring Boot 3 + Spring Modulith, Java 21+ | La misma imagen Docker |
| Base de datos | PostgreSQL en contenedor + Flyway | PostgreSQL gestionado (RDS, Cloud SQL o Azure) |
| Autenticación | Spring Security + OAuth2 login (Google) | Keycloak o un IdP gestionado |
| Jobs | @Scheduled + ShedLock | Igual, o un scheduler gestionado |
| Frontend | Angular + @angular/pwa (Angular Material o PrimeNG) | Estáticos en CDN |
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

### Fase 1 · Núcleo jugable

- [x] Crear liga, invitar managers por enlace y configurar parámetros (RF-05 a RF-08)
- [x] CRUD de jugadores con valor inicial (RF-09 a RF-11)
- [x] Partidos, convocatoria, resultado y estadísticas con su tabla de puntos (RF-12 a RF-16). `match` guarda y publica el acta; el cálculo de puntos de RF-15 lo hace *Cálculo de puntos por acción según posición y clasificaciones* (`scoring`)
- [x] Alineaciones: edición, bloqueo X horas antes y límite de cambios (RF-24 a RF-29). `lineup` bloquea las alineaciones y publica `LineupsLocked`; los puntos del manager (RF-31), incluido el 0 de una alineación fuera de presupuesto, los calcula *Cálculo de puntos por acción según posición y clasificaciones* (`scoring`)
- [x] Cálculo de puntos por acción según posición y clasificaciones (RF-30 a RF-33). `scoring` puntúa cada partido al tener el acta y las alineaciones bloqueadas, con la tabla de la liga congelada, y publica `MatchdayScored`; los puntos por valoración de RF-30 valen 0 hasta *Combinación de valoración y estadísticas al cierre* (Fase 2)

### Fase 2 · Valoraciones

- [x] Apertura y cierre de la ventana de valoración con job programado y ShedLock (RF-17)
- [x] Pantalla de valoración en picas o nota decimal, editable mientras siga abierta, y cancelación de las valoraciones de un manager por el admin (RF-18 a RF-21, RF-41). `rating` sirve los votos, las vistas y la cancelación por API (plan 09, tareas 4 y 4b); la pantalla llega con el `frontend/`
- [x] Combinación de valoración y estadísticas al cierre, con recálculo idempotente (RF-22, RF-23). Incluye adaptar los parámetros de `league`: sustituir `ratingPointsPerUnit` por una conversión por formato (picas −2/2/6/10/14, nota 1:1) y añadir el mínimo de votos. `scoring` suma los puntos por valoración al cerrar la ventana y recalcula con `RatingsCorrected` (plan 09)
- [ ] Prueba real: una jornada con los amigos y ajuste de los pesos

### Fase 3 · Mercado y notificaciones

- [ ] Algoritmo de valor tras cada jornada y gráfica de evolución (RF-34 a RF-36)
- [ ] PWA instalable (manifest y service worker)
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

