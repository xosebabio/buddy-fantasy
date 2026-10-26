# Plan · Modelo base y login con Google

Estado: **borrador listo para implementar** (Fase 0 · Fundamentos).
Cubre del roadmap: *"PostgreSQL con Flyway y modelo base (usuario, liga, membership)"* y *"Spring Security con login de Google y roles por liga"*.
Requisitos relacionados: RF-01, RF-04, RF-05 (solo modelo), RF-08 (solo modelo), RNF-02, RNF-10.

## 1. Decisiones tomadas

| Tema | Decisión |
| --- | --- |
| Método de login | **Solo Google** (OIDC). Sin contraseñas, sin recuperación (RF-03 no aplica) |
| Sesión | **Cookie de sesión** `HttpOnly` + **CSRF activo**. Sesiones en **Spring Session JDBC** (sin estado en disco, sobreviven a reinicios) |
| Alta de usuarios | **Cualquier cuenta Google**. El usuario se crea en el primer login y no tiene liga hasta que cree una o acepte una invitación |
| Admin y manager | **El administrador siempre juega como manager** en su liga. Resuelve la decisión abierta de `AGENTS.md`; actualizar allí |
| Ligas por administrador | **Sin límite**: un usuario puede administrar varias ligas. Cualquier tope se aplica después en `CreateLeague` |
| Identificadores | **UUID generado en la app** (en el dominio, antes de persistir) |
| Alcance extra | `LeagueAccess` (API pública de `league`) y `GET /api/me` |
| Fuera de alcance | `CreateLeague`, invitaciones, parámetros de liga (Fase 1), frontend, Docker de producción |

Consecuencia de "el admin siempre es manager": no hace falta un flag `plays` ni un conjunto de roles. Una membership tiene **un único rol** (`ADMIN` o `MANAGER`), y `ADMIN` implica también ser manager (alinea, valora y puntúa). Quien consulte "¿es manager de la liga?" debe tratar `ADMIN` como manager.

## 2. Modelo de datos

Migraciones Flyway en el único `db/migration`, con el módulo en el nombre (D5). Tablas con prefijo de módulo. **Sin claves foráneas entre módulos**: `league_membership.user_id` no apunta a `identity_user` por FK (cada módulo es dueño de sus tablas); la integridad se garantiza en la capa de aplicación. Sí hay FK dentro de `league` (membership → liga).

### V2__identity_create_tables.sql

```sql
CREATE TABLE identity_user
(
  id            UUID                     NOT NULL PRIMARY KEY,
  google_sub    VARCHAR(255)             NOT NULL UNIQUE,  -- claim "sub" de Google, identificador estable
  email         VARCHAR(320)             NOT NULL,
  display_name  VARCHAR(100)             NOT NULL,
  avatar_url    TEXT,
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

- La identidad se resuelve por `google_sub`, **no por email** (el email puede cambiar).
- Datos mínimos (RNF-10). El borrado de cuenta (Fase 4) se podrá hacer anonimizando esta fila sin romper el histórico de puntos.
- Nombre y avatar se rellenan desde Google en el primer login; la edición del perfil (RF-02) es posterior. En los logins siguientes **solo se actualiza `last_login_at`**, para no pisar lo que el usuario edite después.

### V3__league_create_tables.sql

```sql
CREATE TABLE league_league
(
  id         UUID                     NOT NULL PRIMARY KEY,
  name       VARCHAR(100)             NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE league_membership
(
  id        UUID                     NOT NULL PRIMARY KEY,
  league_id UUID                     NOT NULL REFERENCES league_league (id),
  user_id   UUID                     NOT NULL,   -- referencia lógica a identity_user (sin FK entre módulos)
  role      VARCHAR(20)              NOT NULL CHECK (role IN ('ADMIN', 'MANAGER')),
  joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT league_membership_unique UNIQUE (league_id, user_id)
);
CREATE INDEX league_membership_user_idx ON league_membership (user_id);
```

- El rol es **por liga** (AGENTS.md): el mismo usuario puede ser `ADMIN` en una y `MANAGER` en otra.
- Sin restricción de un solo admin por usuario (decisión tomada). Tampoco se fuerza "un único admin por liga" en BD; si hace falta, se valida en el comando.
- Los parámetros de liga (RF-07) se añadirán en una migración posterior, no ahora.

### V4__identity_spring_session.sql

El esquema de Spring Session JDBC (`spring_session` y `spring_session_attributes`, versión PostgreSQL) copiado a una migración Flyway, con `spring.session.jdbc.initialize-schema: never` (igual que se hizo con `event_publication`). Estas tablas las gestiona el framework, no un módulo de negocio.

## 3. Autenticación (módulo `identity`)

### 3.1 Flujo

```
Angular ──GET /oauth2/authorization/google──► Google ──callback──► /login/oauth2/code/google
                                                                        │
                                              OidcUserService propio: RegisterUser (comando)
                                                                        │
                                       sesión JDBC + cookie ◄───────────┘
```

1. El frontend redirige al navegador a `/oauth2/authorization/google`.
2. Tras el callback, un `OidcUserService` propio (adaptador `identity/adapter/in/web`) despacha por el `CommandBus` el comando **`RegisterOrLoginUserCommand(googleSub, email, name, avatarUrl)`**.
3. El handler busca por `google_sub`: si existe actualiza `last_login_at`; si no, crea el usuario (UUID nuevo, `Clock` inyectado). Es idempotente y devuelve el `UserId`.
4. El principal de la sesión lleva el `UserId` interno, no solo el subject de Google. Lo exponemos con un `CurrentUser` en `shared` (utilidad de seguridad prevista en el plan de estructura §5), para que ningún módulo importe clases de `identity`.
5. Si Google no devuelve `email_verified = true`, se rechaza el login.

### 3.2 Configuración de seguridad (`SecurityConfig`, ya existe como baseline)

- `oauth2Login` con Google (scopes `openid, profile, email`).
- **API sin redirecciones**: ante una petición `/api/**` sin autenticar, responder `401` (no redirigir a Google). La redirección solo ocurre al visitar `/oauth2/authorization/google`.
- **CSRF activo** con `CookieCsrfTokenRepository.withHttpOnlyFalse()` y el handler de SPA, para que Angular envíe `X-XSRF-TOKEN`. No se desactiva en ningún endpoint de la API.
- **Logout**: `POST /api/logout` (con CSRF), invalida la sesión y borra la cookie. Responde `204`, sin redirección.
- Cookie de sesión: `HttpOnly`, `SameSite=Lax`, `Secure` en producción (`server.servlet.session.cookie.*`).
- `server.forward-headers-strategy: framework` para que el `redirect_uri` sea correcto detrás de Caddy.
- Públicos: health (como ahora) y los endpoints del flujo OAuth2. Todo lo demás, autenticado.
- Gestión de sesiones activas (RF-04): queda **fuera**; la tabla de Spring Session permitirá listarlas más adelante.

### 3.3 Configuración por variables de entorno

| Variable | Uso |
| --- | --- |
| `GOOGLE_CLIENT_ID` | Client ID de OAuth en Google Cloud |
| `GOOGLE_CLIENT_SECRET` | Secret (nunca en el repo) |
| `SESSION_COOKIE_SECURE` | `true` en producción, `false` en local |

Prerrequisito manual (lo tienes que hacer tú): crear un cliente OAuth "Aplicación web" en Google Cloud Console con redirect URI `http://localhost:8080/login/oauth2/code/google` para desarrollo (y la del dominio real cuando exista el VPS).

### 3.4 Endpoint `GET /api/me`

Devuelve el usuario autenticado: `{ id, displayName, email, avatarUrl }`. Vive en `identity`.
Sus ligas y roles (`GET /api/me/leagues`) viven en **`league`**, no en `identity`, porque la dependencia entre módulos es `identity ← league` y `identity` no puede conocer las ligas. El frontend hace las dos llamadas.

## 4. Roles por liga (módulo `league`)

API pública en el paquete raíz (`com.buddyfantasy.league`), según D10:

```java
public interface LeagueAccess {
    /** Rol del usuario en la liga, vacío si no es miembro. */
    Optional<LeagueRole> roleOf(UUID userId, UUID leagueId);

    /** Lanza AccessDeniedException (403) si no es miembro. */
    void requireMember(UUID userId, UUID leagueId);

    /** Lanza AccessDeniedException (403) si no es ADMIN de esa liga. */
    void requireAdmin(UUID userId, UUID leagueId);
}

public enum LeagueRole { ADMIN, MANAGER }
```

- Toda comprobación recibe **la liga como contexto** (AGENTS.md); no existe ningún rol global.
- Un no miembro recibe `403` (no `404`) salvo que más adelante se decida ocultar la existencia de la liga.
- `ADMIN` también pasa `requireMember`: el admin es manager de su liga.
- Los controllers de otros módulos la usan hasta que el pipeline del `CommandBus` incorpore autorización por liga (ítem 4 de §3.1 del plan de estructura).
- Dominio en `league/domain`: `League`, `Membership`, `LeagueRole`, `LeagueId`. Entidades JPA separadas con mappers a mano en `adapter/out/persistence` (D4, D11).

`GET /api/me/leagues` → `[{ leagueId, name, role }]` (RF-08, lectura). Se implementa como `Query` + `QueryHandler` con proyección directa, ya que es una pantalla de lectura (D8).

Aunque `CreateLeague` queda fuera, los tests crean ligas y memberships directamente por el puerto de repositorio para probar `LeagueAccess` y `/api/me/leagues`.

## 5. Estructura de código resultante

```
identity/
├── IdentityApi.java?                 # solo si otro módulo necesita datos de usuario (no por ahora)
├── domain/        User, UserId
├── application/
│   ├── usecases/  registerorloginuser/ → RegisterOrLoginUserCommand, RegisterOrLoginUserHandler
│   │              getcurrentuser/      → GetCurrentUserQuery, GetCurrentUserHandler
│   └── port/out/  UserRepository
└── adapter/
    ├── in/web/    SecurityConfig, GoogleOidcUserService, MeController, LogoutHandler
    └── out/persistence/  UserEntity, JpaUserRepository, UserMapper

league/
├── LeagueAccess.java, LeagueRole.java          # API pública
├── domain/        League, Membership, LeagueId
├── application/
│   ├── LeagueAccessService.java                # implementa LeagueAccess
│   ├── usecases/  listmyleagues/ → ListMyLeaguesQuery, ListMyLeaguesHandler
│   └── port/out/  LeagueRepository, MembershipRepository
└── adapter/
    ├── in/web/    MyLeaguesController
    └── out/persistence/  LeagueEntity, MembershipEntity, repos y mappers

shared/
└── security/CurrentUser                        # principal mínimo (UserId) para cualquier módulo
```

Dependencias nuevas en `pom.xml`: `spring-session-jdbc`. (`spring-boot-starter-oauth2-client` y `spring-boot-starter-security` ya están.)

Reglas a respetar: `ModularityTests` y `HexagonalArchitectureTests` deben seguir en verde; en particular el dominio no usa Spring ni JPA, y `league` no importa internals de `identity`.

## 6. Tests (Testcontainers con PostgreSQL, no H2)

| Test | Qué verifica |
| --- | --- |
| Migraciones | Flyway aplica V2–V4 sobre Postgres limpio; `ddl-auto: validate` pasa |
| `RegisterOrLoginUserCommand` | Primer login crea el usuario; segundo login con el mismo `sub` no duplica y solo actualiza `last_login_at`; un `sub` distinto con el mismo email crea otro usuario |
| Tiempo | Los timestamps salen del `Clock` inyectado (con `Clock` fijo en el test) |
| Seguridad | `/api/me` sin sesión → `401` (sin redirección); health → `200` sin autenticar; `POST` sin token CSRF → `403`; con token → permitido |
| Login | Con `oidcLogin()` de `spring-security-test` se crea el usuario y `/api/me` devuelve sus datos; login con `email_verified = false` se rechaza |
| `LeagueAccess` | Admin pasa `requireAdmin` y `requireMember`; manager pasa solo `requireMember`; no miembro → `403`; mismo usuario `ADMIN` en la liga A y `MANAGER` en la B |
| `/api/me/leagues` | Devuelve solo las ligas del usuario con su rol |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde |

## 7. Orden de ejecución

| # | Tarea | Resultado |
| --- | --- | --- |
| 1 | Migraciones V2 (identity), V3 (league), V4 (Spring Session) y `spring-session-jdbc` en el `pom.xml` | Flyway aplica en Testcontainers |
| 2 | `identity`: dominio, puerto, adaptador JPA y `RegisterOrLoginUserCommand` con su test | Alta idempotente por `google_sub` |
| 3 | `league`: dominio, repositorios JPA y `LeagueAccess` con su test | Roles por liga comprobables |
| 4 | `SecurityConfig`: OAuth2 login, `401` en API, CSRF, logout, sesión JDBC y `OidcUserService` propio | Login completo con `oidcLogin()` en tests |
| 5 | `GET /api/me` y `GET /api/me/leagues` | Endpoints cubiertos por tests |
| 6 | `application.yml` con las variables de entorno y sección *Comandos* de `AGENTS.md` (variables nuevas, decisión admin = manager) | Documentado |
| 7 | Prueba manual con una cuenta Google real contra el Postgres local | Login de punta a punta |

Al terminar, marcar en `requirements.md` los dos ítems del roadmap (modelo base y login) y dejar las migraciones aplicadas como inmutables: cualquier cambio posterior va en una migración nueva.

## 8. Riesgos y notas

- **Sin liga tras el login**: hasta que exista `CreateLeague` (Fase 1) un usuario nuevo no puede hacer nada útil; para la prueba manual (#7) se inserta una liga y una membership a mano por SQL.
- **Cambio de email en Google**: se identifica por `sub`, así que no rompe cuentas; el email guardado es informativo y se refresca solo si decidimos hacerlo (ahora no).
- **Cuentas Google abiertas a cualquiera**: aceptable mientras la app solo sea accesible para el grupo de amigos. Si el VPS queda público y molesta, una lista blanca de emails por variable de entorno es un cambio pequeño en `GoogleOidcUserService`.
- **Una instancia o varias**: Spring Session JDBC ya permite varias instancias sin sesiones pegajosas, por lo que no bloquea la migración a cloud.
- **Fase 5**: el login pasará a un IdP gestionado o Keycloak; mantener el `OidcUserService` aislado en un único adaptador facilita ese cambio.
