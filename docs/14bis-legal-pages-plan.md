# 14 bis · Política de privacidad y condiciones de servicio — plan

Plan de dos páginas de contenido fijo, públicas y sin sesión: la **política de privacidad** y las **condiciones de servicio**. Va pegado al plan 14 porque las dos son necesarias para abrir `buddyfantasy.com` a los amigos: la pantalla de consentimiento de Google en _In production_ (plan 14, decisión 16) pide las URL de la página principal, de la política de privacidad y de las condiciones, y el RGPD obliga a informar a cada usuario de quién trata sus datos, para qué y cómo ejercer sus derechos (art. 13) desde el primer login, no desde la Fase 4. Las reglas son las de `docs/requirements.md` (RNF-10, RNF-15, RNF-16, FE-01 a FE-04, FE-26, FE-34). Este plan no las repite: dice qué se escribe, dónde va y en qué orden.

> Los textos los redacta el agente como borrador a partir de cómo funciona la app hoy. No son asesoramiento jurídico: el usuario los revisa (tarea 4) y decide si se los enseña a alguien que sepa de esto.

## 1. Alcance

| Tema             | Decisión                                                                                                                                                                                                                           |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Páginas          | `/privacy` (_Política de privacidad_) y `/terms` (_Condiciones de servicio_), públicas, sin sesión y sin llamadas al API (§4).                                                                                                     |
| Contenido        | El que exige el art. 13 del RGPD para la privacidad, con una sección de cookies y almacenamiento local; y unas condiciones de uso cortas para un servicio gratuito entre amigos (§3).                                              |
| Enlaces          | Desde el login (con una frase de aceptación), desde _Tu perfil_ y entre las dos páginas (§5).                                                                                                                                      |
| Google           | El usuario añade las tres URL a la pantalla de consentimiento (§6).                                                                                                                                                                |
| Fuera de alcance | El borrado de cuenta desde la app y el consentimiento guardado (RNF-10, Fase 4): hasta entonces, la política dice cómo pedirlo por correo. Un banner de cookies (no hace falta, §3.1). Otros idiomas (RNF-15: solo español en v1). |

## 2. Decisiones

Las marcadas como _Usuario_ las cerró el usuario al revisar el plan. Las _Propuesta_ se revisaron con él (tarea 1) y se aceptaron todas tal cual.

| #   | Tema                          | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Origen              |
| --- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------- |
| 1   | Cómo se sirven                | Rutas de Angular `privacy` y `terms`, lazy, fuera del shell como el login, en una feature nueva `features/legal` con el texto en la plantilla. Ganan los tokens, el tema oscuro, `$localize`, el lint y el axe del E2E, y no duplican estilos. Se descartan los HTML estáticos en `frontend/public/`: se leerían sin JavaScript, pero quedarían fuera de los tokens (FE-24), del i18n (RNF-15) y del lint. A Google le vale: su comprobador ejecuta JavaScript. | Usuario             |
| 2   | Responsable y contacto        | Contacto: `docksidegarage@gmail.com`, un correo dedicado al proyecto (no el personal, así que no choca con el §8 del plan 14). Responsable: Xosé Babío Sabín, persona física y sin ánimo de lucro; ni NIF ni dirección, que la LSSI solo exige a quien ejerce una actividad económica.                                                                                                                                                                          | Usuario             |
| 3   | Aceptación                    | Una frase bajo _Entrar con Google_ («Al entrar aceptas las condiciones de servicio y la política de privacidad», con los dos enlaces), sin casilla ni nada guardado. La base de los datos de la cuenta es prestar el servicio que el usuario pide (art. 6.1.b), no el consentimiento, así que no hace falta casilla. El consentimiento guardado y versionado se queda en el ítem _RGPD_ de la Fase 4.                                                           | Usuario             |
| 4   | Jugadores que no son usuarios | El admin registra jugadores reales (nombre, dorsal, posición, estadísticas, valoraciones y, más adelante, foto) que puede que nunca entren en la app. Las condiciones obligan al admin a registrar solo a quien esté de acuerdo, y la política dice qué se guarda de un jugador y que puede pedir que se borre o se cambie su nombre escribiendo al contacto de la decisión 2. Sin cambios en la pantalla de alta de jugador.                                   | Usuario             |
| 5   | Edad mínima                   | 14 años, la edad del consentimiento digital en España (LOPDGDD, art. 7). Se dice en las dos páginas; no se comprueba (Google no da la edad con los scopes actuales).                                                                                                                                                                                                                                                                                            | Propuesta, aceptada |
| 6   | Cookies                       | Sin banner: `SESSION` y `XSRF-TOKEN` son técnicas y el `localStorage` (tema y última liga) y el `sessionStorage` (ruta de vuelta tras el login) también, así que están exentas de consentimiento (LSSI, art. 22.2). Se listan en una tabla de la política, con su finalidad y duración. Si algún día entra analítica, esto cambia.                                                                                                                              | Propuesta, aceptada |
| 7   | Encargados y terceros         | Hetzner Online GmbH (alojamiento, Alemania), Amazon Web Services EMEA SARL (copias cifradas en un bucket de la UE) y Google (login, como responsable independiente). Se dice que la foto de perfil se carga desde los servidores de Google (`googleusercontent.com`), que ven la IP de quien la mira. Sin GitHub: no trata datos de los usuarios.                                                                                                               | Propuesta, aceptada |
| 8   | Conservación                  | Datos de la cuenta y de juego mientras exista la cuenta; las copias, hasta 30 días después (plan 14, decisión 13); la sesión, hasta que caduca o se cierra; los logs, lo que tarda en rotarlos Docker (plan 14, decisión 17). La tarea 2 comprueba qué entra en los logs (IP, correo) antes de escribirlo.                                                                                                                                                      | Propuesta, aceptada |
| 9   | Ejercicio de derechos         | Por correo al contacto de la decisión 2, con respuesta en un mes como máximo (art. 12.3), y derecho a reclamar ante la AEPD. Mientras no exista el borrado desde la app, el usuario lo hace a mano (§7).                                                                                                                                                                                                                                                        | Propuesta, aceptada |
| 10  | Fecha y cambios               | Cada página lleva _Última actualización: <fecha>_ en el texto. Los cambios se hacen por commit; los importantes se avisan en la app cuando existan los avisos de la Fase 3. Las condiciones no prometen ninguna licencia del código: el repo no tiene `LICENSE` todavía.                                                                                                                                                                                        | Propuesta, aceptada |
| 11  | Roadmap                       | Se añade a la Fase 0 de `requirements.md` el ítem _Política de privacidad y condiciones de servicio (RNF-10)_, porque bloquea la prueba real igual que el VPS. El ítem _RGPD_ de la Fase 4 se queda con el consentimiento guardado y el borrado de cuenta.                                                                                                                                                                                                      | Propuesta, aceptada |

## 3. Contenido

Texto en español, frases cortas y de tú, como el resto de la interfaz. Títulos `h2` por sección para que se pueda enlazar cada una (`/privacy#cookies`).

### 3.1 Política de privacidad (`/privacy`)

1. **Quién es el responsable:** nombre y contacto (decisión 2). Proyecto personal, gratuito y sin ánimo de lucro.
2. **Qué datos se tratan:**
   - De tu cuenta de Google: el identificador de Google, el correo, el nombre y la foto de perfil (su URL), y las fechas de alta y del último acceso (`identity_user`).
   - De tu juego: las ligas en las que estás y tu rol, tus alineaciones, tus valoraciones y tus puntos.
   - De los jugadores del equipo (decisión 4): nombre, dorsal, posición, estadísticas de cada partido, valoraciones y valor.
   - Técnicos: la sesión y lo que quede en los logs (decisión 8).
   - Lo que **no**: ni analítica, ni publicidad, ni rastreadores, ni fuentes de terceros (FE-27).
3. **Para qué y con qué base:** prestar el servicio (art. 6.1.b); la seguridad, los logs y las copias (interés legítimo, art. 6.1.f); los datos de los jugadores, el interés legítimo de la liga en llevar su fantasy, con la oposición de la decisión 4.
4. **Quién los ve:** los miembros de tu liga ven tu nombre y tu foto, tus alineaciones tras el bloqueo y tus puntos. Tus votos son anónimos: nadie, tampoco el admin, ve los votos de otro (RF-21). Nadie fuera de tu liga ve nada.
5. **Encargados y terceros:** los de la decisión 7, con lo que hace cada uno.
6. **Transferencias fuera de la UE:** los datos están en la UE; Google y AWS son empresas de EE. UU. adheridas al Marco de Privacidad de Datos UE-EE. UU.
7. **Cuánto tiempo:** decisión 8.
8. **Tus derechos:** acceso, rectificación, supresión, oposición, limitación y portabilidad; cómo pedirlos (decisión 9) y la AEPD (`aepd.es`). Darse de baja de una liga no borra la cuenta.
9. **Cookies y almacenamiento local:** tabla con `SESSION`, `XSRF-TOKEN`, la clave del tema, la de la última liga y la de la ruta de vuelta: qué es, para qué, dónde (cookie, `localStorage`, `sessionStorage`) y cuánto dura (decisión 6). Se sacan los nombres exactos del código en la tarea 2.
10. **Menores:** decisión 5.
11. **Cambios:** decisión 10.

### 3.2 Condiciones de servicio (`/terms`)

1. **Qué es Buddy Fantasy y quién lo ofrece:** el fantasy de una liga de peñas, gratuito, sin pagos ni premios, ofrecido por el responsable de la decisión 2.
2. **Tu cuenta:** se entra con Google, una cuenta por persona, desde los 14 años (decisión 5). Lo que haces con tu cuenta es cosa tuya.
3. **Ligas y roles:** el admin de cada liga es quien la crea; registra el equipo, los jugadores, los partidos y las actas, y puede expulsar miembros y cancelar valoraciones (RF-41). Solo registra a jugadores que estén de acuerdo (decisión 4) y los quita o los cambia si se lo piden.
4. **Uso aceptable:** nada de suplantar a nadie, ni nombres de liga o de jugador ofensivos, ni automatizar, sobrecargar o atacar el servicio.
5. **Valoraciones:** son opiniones deportivas, anónimas para los demás; se pide respeto.
6. **Disponibilidad:** se ofrece tal cual, sin garantía de que esté siempre disponible ni de que no se pierdan datos, aunque haya copias diarias. Puede cambiar o cerrar, avisando antes si es posible.
7. **Responsabilidad:** limitada a lo que permita la ley, sin tocar los derechos que te da la normativa de consumidores.
8. **Tu contenido:** lo que escribes (nombres de liga, de jugadores…) es tuyo; nos dejas guardarlo y enseñárselo a tu liga para que el servicio funcione.
9. **Baja y suspensión:** puedes dejar una liga cuando quieras y pedir que se borre tu cuenta (`/privacy`); el responsable puede suspender una cuenta que incumpla estas condiciones.
10. **Cambios:** decisión 10.
11. **Ley aplicable:** la española; si eres consumidor, los juzgados de tu domicilio.

## 4. Implementación

- **Feature `features/legal`** con `legal.routes.ts` (`legalRoutes`), `privacy-page` y `terms-page`. Sin servicios de API ni estado: solo plantilla y estilos. No importa ninguna otra feature (FE-02).
- **Rutas** en `app.routes.ts`, junto a `loginRoutes`: fuera del shell y **sin guard** (ni `authGuard` ni `guestGuard`), para que las vea cualquiera, con sesión o sin ella. Título por ruta con `$localize` (_Política de privacidad_ y _Condiciones de servicio_).
- **Maquetación:** una columna de lectura (ancho máximo en tokens), el `bf-wordmark` arriba enlazando a `/`, los títulos de sección como `h2` con `id`, la tabla de cookies con la de `ui/` si existe (si no, una tabla semántica con los mixins de `ui/styles`) y el pie con el enlace a la otra página. Solo tokens (FE-24), tema claro y oscuro (FE-26).
- **Textos:** con `i18n` y un `@@id` por párrafo (`legal.privacy.*`, `legal.terms.*`), como pide el lint de i18n. El contacto (decisión 2) va en el texto, no en configuración: es contenido, no un límite de negocio.
- **Prerender:** no. Con el `try_files` de Caddy, `/privacy` sirve `index.html` y Angular pinta la página.
- **CSP y Caddyfile:** sin cambios; no hay recursos externos.

## 5. Enlaces

- **Login:** la frase de la decisión 3 bajo el botón, en texto pequeño, con los dos enlaces (`routerLink`, no import de la feature).
- **Tu perfil:** una sección _Legal_ al final, con _Política de privacidad_ y _Condiciones de servicio_.
- **Entre las páginas:** cada una enlaza a la otra en el pie, y la de condiciones a `/privacy` donde habla de datos.
- Sin pie global en el shell: la navegación inferior del móvil ya ocupa ese sitio.

## 6. Google y el dominio (usuario)

En _Google Auth Platform → Branding_ del proyecto de Google Cloud:

- **Página principal:** `https://buddyfantasy.com` (lleva al login, que enlaza las dos páginas).
- **Política de privacidad:** `https://buddyfantasy.com/privacy`.
- **Condiciones del servicio:** `https://buddyfantasy.com/terms`.
- **Dominios autorizados:** `buddyfantasy.com`.

Con solo `openid`, `profile` y `email` no hace falta verificación (plan 14, decisión 16). Si se sube un logo, Google sí pide verificar la marca, y para eso exige estas URL y que el dominio esté verificado en Search Console. Como correo de asistencia de esa pantalla se pone también el de la decisión 2. Todo esto va en `deploy/README.md`, sección de Google.

## 7. Borrado de cuenta a mano (hasta la Fase 4)

La política promete atender una petición de borrado en un mes, así que hace falta saber cómo. La tarea 2 escribe en `deploy/README.md` el procedimiento a mano con `psql` en el VPS, por módulo y sin joins entre módulos, de las tablas de cada uno: qué se borra (la fila de `identity_user`, sus sesiones, sus membresías) y qué se anonimiza o se queda (alineaciones, votos y puntos ya contados, que salen como _Antiguo miembro_, igual que un miembro expulsado). Si el procedimiento no se puede escribir sin romper invariantes, se para y se le pregunta al usuario si adelantar el borrado de la Fase 4. Las copias de S3 caducan solas a los 30 días; la política lo dice.

## 8. Tests y comprobaciones

- **Unitarios (FE-33):** las rutas `privacy` y `terms` existen, no llevan guard y cargan su página (en `app.routes.spec.ts`); el login muestra los dos enlaces; el perfil, su sección _Legal_.
- **E2E (FE-34):** un spec corto `legal.spec.ts`, sin login: desde el login abre cada página, comprueba el título y pasa axe, en los cuatro proyectos (claro, oscuro, Chromium y WebKit).
- **Producción:** tras el despliegue, `curl` a `/privacy` y `/terms` devuelve `200` con `index.html`. Se puede añadir a la comprobación de humo del plan 14 (§5.3).
- Sin tests de Java: el backend no cambia.

## 9. Cambios en la documentación

En el cambio de la tarea que los provoca:

- `requirements.md`: el ítem nuevo de la Fase 0 (decisión 11) y, si la decisión 1 añade la feature, la ruta en la tabla de pantallas.
- `frontend.md`: la feature `legal` y que sus rutas son públicas.
- `deploy/README.md`: las URL de Google (§6) y el borrado a mano (§7).
- `status.md` y _Estado_ de `AGENTS.md`.

## 10. Orden de ejecución

| #   | Tarea                                                                                                                                                            | Resultado                                  | Hecho |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ | ----- |
| 1   | Cerrar con el usuario las propuestas y el nombre del responsable (§2)                                                                                            | Decisiones cerradas                        | ✅    |
| 2   | Inventario de datos: tablas con datos personales por módulo, cookies y claves de almacenamiento, qué entra en los logs; procedimiento de borrado a mano (§3, §7) | Hechos para el texto y borrado documentado | ✅    |
| 3   | Borrador de los dos textos en Markdown (en este plan o aparte) para que el usuario los revise antes de maquetarlos (§3)                                          | Textos aprobados                           | ✅    |
| 4   | Feature `legal` con las dos páginas, rutas públicas y tests unitarios (§4, §8)                                                                                   | `/privacy` y `/terms`                      | ✅    |
| 5   | Enlaces del login y del perfil, E2E `legal.spec.ts` con axe (§5, §8)                                                                                             | Páginas enlazadas y probadas               |       |
| 6   | `deploy/README.md` con Google (§6); el usuario rellena _Branding_ en Google tras el despliegue                                                                   | Consentimiento de Google completo          |       |
| 7   | Documentación de §9, `status.md`, _Estado_ de `AGENTS.md` y marcar el ítem en `requirements.md`                                                                  | Plan cerrado                               |       |

Cada tarea es un commit en Conventional Commits citando RNF-10: `feat(legal): …` para las páginas, `docs: …` para textos y guías. Las tareas 4 y 5 pueden ir juntas si salen pequeñas.

## 11. Riesgos y notas

- **No es asesoramiento jurídico.** El texto se ajusta a cómo funciona la app hoy; si cambia (fotos en S3, avisos por email de la Fase 3, analítica), la política se actualiza en el mismo cambio. Se añade esta regla a `conventions.md`.
- **Promesas que hay que cumplir:** la política dice qué datos hay, quién los ve y cuánto duran. Si el inventario de la tarea 2 encuentra algo que no encaja (por ejemplo, el correo en los logs), se corrige el código o el texto, pero no se publica algo falso.
- **Fotos de los jugadores:** cuando se activen (plan 14, decisión 4), hay que añadir AWS como encargado de las fotos y CloudFront a la política, y probablemente el permiso del jugador para su foto.
- **Avisos por email (Fase 3):** el email pasa a usarse para algo más que identificar; hará falta revisar la base y las preferencias (RF-39).
- **Registro abierto (Fase 5):** con desconocidos, unas condiciones tan cortas probablemente no basten.

## 12. Inventario de datos (tarea 2)

Hecho el 2 de noviembre de 2026 contra el código de `develop` (commit `45086ad`). Describe lo que hay, no lo que debería haber: si el texto de §3 promete otra cosa, se corrige el texto o el código.

### 12.1 Tablas con datos personales

«Persona» es quién aparece en la fila: un usuario con cuenta, un jugador del equipo (decisión 4) o el admin.

| Módulo | Tabla | Datos personales | Persona | Notas |
| --- | --- | --- | --- | --- |
| identity | `identity_user` | `google_sub`, `email`, `display_name`, `avatar_url` (URL de la foto de Google), `created_at`, `last_login_at` | Usuario | La única tabla con el correo. |
| identity | `spring_session` | `principal_name` (= `sub` de Google) | Usuario | Caduca tras 30 min sin actividad (default de Spring Boot) o al cerrar sesión. |
| identity | `spring_session_attributes` | Contexto de seguridad serializado: `CurrentUser` con el ID token (correo, nombre, foto) | Usuario | Se borra en cascada con su sesión. |
| league | `league_membership` | `user_id`, `role`, `joined_at` | Usuario | Referencia lógica a `identity_user`, sin FK. |
| league | `league_league` | `name` (lo escribe el admin) | — | Sin datos de personas salvo lo que escriba el admin. |
| league | `league_invitation` | `token_hash` (SHA-256) | — | El token en claro no se guarda. |
| squad | `squad_player` | `name`, `shirt_number`, `position`, `initial_value`, `photo_key` | Jugador (no usuario) | `photo_key` es una clave de S3; no hay fotos en producción todavía. Retirar un jugador es un borrado lógico (`retired_at`): su nombre se queda. |
| match | `match_match` | `opponent` | — | Nombre del rival: no es una persona. |
| match | `match_player_performance` | `player_id` y estadísticas | Jugador | |
| match | `match_audit_entry` | `actor_user_id` (el admin), `before` y `after` en JSON | Admin | Auditoría (RNF-04), sin FK. |
| lineup | `lineup_version`, `lineup_version_player` | `user_id`, `saved_at`, jugadores de la alineación | Usuario | |
| lineup | `lineup_locked_lineup`, `lineup_locked_player` | `user_id`, valor y presupuesto al bloquear | Usuario | |
| scoring | `scoring_lineup`, `scoring_lineup_player` | `user_id`, `over_budget`, puntos | Usuario | |
| scoring | `scoring_player_score`, `scoring_player_rating` | `player_id` y copia de estadísticas y puntos | Jugador | |
| rating | `rating_vote` | `user_id`, `player_id`, `value`, `updated_at` | Usuario | Anónimo para los demás (RF-21). |
| rating | `rating_exclusion` | `user_id`, `excluded_by` (admin), `excluded_at` | Usuario y admin | |
| rating | `rating_player`, `rating_window` | Votos agregados y fechas | Jugador | Sin `user_id`. |
| shared | `event_publication` | `serialized_event` en JSON | Usuario y jugador (solo ids) | Ver 12.4. |
| shared | `shedlock` | `locked_by` (instancia del job) | — | Sin datos personales. |

Los eventos de dominio y sus datos anidados (`PlayerPerformanceData`, `LockedLineupData`, `PlayerRatingData`, `PlayerScoreData`, `ManagerScoreData`) llevan solo UUID, números, booleanos, enums, importes y fechas: ningún nombre ni correo.

### 12.2 Cookies y almacenamiento del navegador

| Clave | Dónde | Para qué | Duración | Código |
| --- | --- | --- | --- | --- |
| `SESSION` | cookie | Id de sesión de Spring Session. `HttpOnly`, `SameSite=Lax`, `Secure` en producción | Cookie de sesión (sin `Max-Age` por el default de Spring Session; no lo he visto en la cookie real): se va al cerrar el navegador. En el servidor, 30 min sin actividad. `POST /api/logout` la borra | `SecurityConfig`, `application.yml`, `SessionTests` |
| `XSRF-TOKEN` | cookie | Token CSRF que la SPA copia en `X-XSRF-TOKEN`. No es `HttpOnly` porque la lee el JavaScript | Cookie de sesión: `CookieCsrfTokenRepository` usa `maxAge` -1 por defecto | `SecurityConfig`, `provide-api-http.ts` |
| `bf.theme` | `localStorage` | Tema elegido: `light` o `dark`. Con `system` se borra | Hasta que se cambie el tema. El cierre de sesión no la borra | `theme.ts`, `index.html` |
| `bf.lastLeague` | `localStorage` | Id de la última liga, para que `/` lleve a ella | Hasta que se guarde otra liga. El cierre de sesión no la borra | `last-league.ts` |
| `bf.returnUrl` | `sessionStorage` | Ruta a la que volver tras el login | Se borra al leerla y al cerrar la pestaña | `return-url.ts` |

- No hay cookies de terceros ni analítica: `frontend/src` no carga ningún script ni URL externa, y no hay service worker ni `indexedDB`.
- Google pone sus propias cookies durante el login, en su dominio. No las controlamos. Esto es lo habitual; no lo he comprobado en un login real.

### 12.3 Logs

- **Código (`log.*`).** Siete llamadas. Ninguna escribe correo, nombre ni IP. Las de error (`ApiExceptionHandler`, `SpringCommandBus` y los jobs) escriben la traza o `e.toString()`. Los mensajes de excepción del código llevan solo ids (liga, partido, usuario, jugador).
- **Riesgo residual.** Un error de base de datos no previsto puede llevar en el mensaje el valor de la clave que chocó. Dos primeros inicios de sesión simultáneos podrían dejar el `google_sub` en el log. No lo he provocado; es lo que devuelve PostgreSQL en un duplicado.
- **Spring Boot.** Sin access log: `server.tomcat.accesslog.enabled` no está configurado y su default es `false` (metadatos de `spring-boot-autoconfigure` 3.5.6).
- **Caddy.** Sin la directiva `log` no hay access log: según su documentación, los access logs se activan explícitamente. Los errores de proceso van a stderr. **Por comprobar en el VPS:** si el error de una petición (p. ej. un fallo de `reverse_proxy`) incluye la IP del cliente. La búsqueda no lo confirma. Hasta entonces, el texto no puede decir «no guardamos tu IP».
- **Docker.** `json-file` con `max-size: 10m` y `max-file: 3` en todos los servicios (plan 14, decisión 17): unos 30 MB por servicio. Cuánto tiempo cubren depende del volumen; no se sabe hasta tener tráfico.

Para el texto (decisión 8): la app no escribe el correo ni la IP en sus logs; la traza de un error inesperado puede llevar un id o, en el caso raro de arriba, el `google_sub`; y los logs de Caddy quedan pendientes de la comprobación.

### 12.4 Hechos que cambian el texto

1. **Los puntos y las alineaciones se quedan al borrar una cuenta.** `lineup_*`, `scoring_lineup*` y `rating_vote` guardan el `user_id`. Sin `identity_user` no se puede volver a la persona, y la liga la muestra como «Antiguo miembro» (`roster.ts`). La decisión 8 solo dice «mientras exista la cuenta», y eso no cubre lo que se queda: hay que añadirlo a §3.1.7 y a §3.1.8, para que quien pide el borrado lo sepa.
2. **`bf.lastLeague` sobrevive al cierre de sesión.** Se dice en la tabla de cookies y almacenamiento.
3. **`event_publication` no se vacía.** Spring Modulith 1.4.3 usa `completion-mode: update` por defecto (verificado en el jar): los eventos completados quedan con su JSON. Solo llevan ids, enums, números y fechas (ver 12.1). Es un registro pseudonimizado, como las tablas de juego; §3.1.7 tiene que nombrarlo. Ver 12.5.
4. **Google pone cookies en el login**, fuera de nuestro dominio. Se dice como cosa de Google.

### 12.5 Decisiones pendientes

1. **Admin de una liga.** Un admin no puede salir ni ser expulsado (`ADMIN_CANNOT_LEAVE`, `Membership.requireRemovable`). Borrar su cuenta dejaría la liga sin admin, así que §7 manda parar. `deploy/README.md` §10 excluye a los admins. Hay que decidir qué pasa con su liga (p. ej., traspasar el admin a mano a otro miembro antes del borrado, o cerrar la liga). **Decidido (tarea 3): se cierra la liga.** Se lee como borrado completo: la liga desaparece para todos sus miembros, con sus partidos, alineaciones, valoraciones y puntos, y el texto lo dice. El procedimiento SQL para cerrarla está por escribir; hasta entonces no se hace a mano. Para el admin no hay plazo fijo.
2. **Retención de `event_publication`.** (a) Se deja y el texto lo dice; (b) `spring.modulith.events.completion-mode: delete` en `application.yml`, que vacía los eventos completados. La opción (b) es un cambio de código y no lo he hecho. Antes hay que comprobar que nada depende de los eventos completados.
3. **IP en los logs de Caddy.** Comprobar en el VPS tras el primer despliegue: provocar un error de proxy y leer `docker compose logs caddy`. Si sale la IP, el texto lo dice; si no, la frase «no guardamos tu IP» vale.

Decidido en la tarea 3, junto con 12.5.1: la base para conservar alineaciones, puntos y votos tras un borrado es el interés legítimo (art. 6.1.f), y para los jugadores sin cuenta basta con publicar la política y el contacto (art. 14 RGPD).
