# 03 · Ligas, invitaciones y parámetros — plan

Primer ítem de la **Fase 1 · Núcleo jugable** (`requirements.md`): *Crear liga, invitar managers por enlace y configurar parámetros* (RF-05 a RF-08). Se apoya en el modelo base y el login de `02-base-model-and-google-login-plan.md`.

## 1. Alcance

Decisiones tomadas:

| Tema | Decisión |
| --- | --- |
| Invitación | Enlace con token aleatorio por liga, revocable/regenerable por el admin. Sin caducidad ni límite de usos. Quien lo abre, ya autenticado, entra como `MANAGER`. |
| Parámetros | Todos los de `requirements.md`, sembrados con sus valores por defecto al crear la liga, incluida la tabla de puntos por acción y posición. Editables desde ya aunque los módulos consumidores lleguen después. |
| Miembros | Listar miembros, expulsar (admin) y abandonar (manager). |
| Cambio de liga (RF-08) | Ya cubierto por `GET /api/me/leagues`; el "cambio" es una cuestión del frontend (la liga activa va en la URL). No se guarda "liga activa" en servidor. |

Fuera de alcance: frontend, notificación de invitación (RF-38, Fase 3), jugadores/partidos, y cualquier uso real de los parámetros (puntuación, bloqueo…).

## 2. Modelo de datos (migraciones nuevas, no se edita V1–V4)

**Unidad monetaria:** todos los valores de jugador y el presupuesto se expresan en **millones** (`NUMERIC(10,2)`), y el valor mínimo por defecto es 1 M. El presupuesto por defecto es 100 M; se mantiene el flag `lineup_budget_enabled` (activado por defecto desde el plan 04) para que el admin pueda desactivar el límite.

**V5__league_settings.sql**

- `league_settings` (1:1 con `league_league`, PK = `league_id`), una columna por parámetro, con `CHECK` de rango:
  - `rating_window_hours` (24), `rating_format` (`SPADES` | `DECIMAL`, def. `SPADES`), `rating_points_per_unit` (1), `default_rating` (NULL = sin nota por defecto)
  - `lineup_lock_hours` (2), `lineup_max_changes` (2), `lineup_size` (7), `lineup_formation_enabled` (false), `lineup_budget_enabled` (true, ver `04-squad-players-plan.md`), `lineup_budget` (100, en millones)
  - `market_k` (0.05), `market_max_change_pct` (10), `market_min_value` (1, en millones)
- `league_scoring_rule` (`league_id`, `action`, `position`, `points`, PK compuesta): 9 acciones × 4 posiciones = 36 filas sembradas por liga. `action` y `position` con `CHECK` sobre los enums.
- Alternativa descartada: JSONB para la tabla de puntos; se prefieren filas para validar con constraints y consultar sin parsear.

**V6__league_invitation.sql**

- `league_invitation` (`id`, `league_id` FK, `token_hash` UNIQUE, `created_at`, `revoked_at` NULL).
- Solo se guarda el **hash SHA-256** del token; el token en claro se devuelve una sola vez al crearlo. Una única invitación activa por liga: regenerar revoca la anterior (índice único parcial `WHERE revoked_at IS NULL` sobre `league_id`).

Los datos de `league_settings` y `league_scoring_rule` son propiedad de `league`; otros módulos los leerán por una API pública (ver §4), nunca por SQL.

## 3. Dominio (`league/domain`, sin Spring ni JPA)

- `League`: añade `settings: LeagueSettings`. `League.create(name, now)` siembra `LeagueSettings.defaults()`.
- `LeagueSettings` (value object): valida invariantes en el constructor (p. ej. `lineupSize ≥ 1`, `marketMaxChangePct` en (0,100], `ratingWindowHours ≥ 1`). Los defaults viven aquí como **fábrica del dominio** (son la siembra, no constantes de negocio dispersas; la fuente de verdad por liga es la BD).
- `ScoringRules` (value object): mapa `(ScoringAction, Position) → int`, con `defaults()` = tabla de `requirements.md`. `ScoringAction` y `Position` viven en el módulo `datadefinition` (ver §3 bis); `ScoringRules` solo los referencia.
- `Invitation`: `create`, `revoke`, `matches(token)`.
- `Membership`: se añade `remove` implícito vía repositorio; reglas en el handler/dominio: **el admin no puede abandonar ni ser expulsado** (la liga siempre tiene admin; cambiar de admin queda fuera de v1).

## 3 bis. Módulo `datadefinition` (nuevo)

Módulo de primer nivel (paquete `com.buddyfantasy.datadefinition`; el guion no es válido en Java) para **definiciones de datos compartidas sin lógica**: enums y value objects planos que usan dos o más módulos.

- Contenido inicial: `Position` (`GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`) y `ScoringAction` (las 9 acciones de la tabla de puntos).
- **Regla:** solo datos. Si un tipo necesita comportamiento (cálculos, validaciones, reglas), va al kernel `shared` o a su módulo dueño, no aquí.
- Es un módulo hoja: no depende de ningún otro y todos pueden depender de él. Se declara como API pública en su `package-info.java` (el paquete raíz es la API) y se anota en `AGENTS.md` (tabla de módulos y regla de uso).
- Test de arquitectura nuevo: `datadefinition` no importa de ningún otro módulo ni contiene beans Spring, entidades JPA ni lógica (solo enums/records); `ModularityTests` sigue en verde.

## 4. Casos de uso (`league/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `CreateLeagueCommand` | command | autenticado | Crea liga + settings + reglas + membership `ADMIN` + invitación inicial, en una transacción (RF-05). Devuelve `leagueId` y token. |
| `GetLeagueQuery` | query | miembro | Datos de la liga y rol propio. |
| `GetLeagueSettingsQuery` | query | miembro | Los managers ven las reglas; solo el admin las edita. |
| `UpdateLeagueSettingsCommand` | command | `requireAdmin` | Reemplazo completo validado (RF-07). Ver decisión abierta 3 sobre cambios con jornadas ya puntuadas. |
| `RegenerateInvitationCommand` | command | `requireAdmin` | Revoca la activa y crea una nueva; devuelve token en claro. |
| `RevokeInvitationCommand` | command | `requireAdmin` | Desactiva el enlace sin crear otro. |
| `PreviewInvitationQuery` | query | autenticado | Dado un token, devuelve nombre de la liga (y si ya eres miembro) para la pantalla "¿Unirte a X?". No expone nada más. |
| `JoinLeagueCommand` | command | autenticado | Valida token vigente; **idempotente** (si ya es miembro, devuelve OK sin duplicar). Crea membership `MANAGER` (RF-06). |
| `ListLeagueMembersQuery` | query | miembro | Nombre visible y rol (el nombre viene de `identity`, ver §5). |
| `RemoveMemberCommand` | command | `requireAdmin` | No permite expulsar al admin. |
| `LeaveLeagueCommand` | command | miembro | No permite al admin. |

Si `squad`/`lineup` necesitan leer parámetros, `league` expone una interfaz pública `LeagueSettingsProvider` (paquete raíz del módulo) con un DTO inmutable. Se crea ahora **solo si** un test de arquitectura la pide; si no, se deja para cuando exista el primer consumidor.

## 5. Dependencia con `identity` (listar miembros)

`ListLeagueMembersQuery` necesita nombre/avatar de usuarios, y no se permiten joins SQL entre módulos. Opción elegida: `identity` expone una API pública `UserDirectory.findNames(Set<UUID>)` y `league` compone el resultado en el handler (una consulta por lote, sin N+1). Requiere declarar la dependencia `league → identity` en `package-info.java` (Modulith) y que `ModularityTests` siga en verde.

## 6. API HTTP (adaptadores `adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `POST /api/leagues` | `CreateLeagueCommand` |
| `GET /api/leagues/{id}` | `GetLeagueQuery` |
| `GET /api/leagues/{id}/settings` | `GetLeagueSettingsQuery` |
| `PUT /api/leagues/{id}/settings` | `UpdateLeagueSettingsCommand` |
| `POST /api/leagues/{id}/invitation` | `RegenerateInvitationCommand` |
| `DELETE /api/leagues/{id}/invitation` | `RevokeInvitationCommand` |
| `GET /api/invitations/{token}` | `PreviewInvitationQuery` |
| `POST /api/invitations/{token}/join` | `JoinLeagueCommand` |
| `GET /api/leagues/{id}/members` | `ListLeagueMembersQuery` |
| `DELETE /api/leagues/{id}/members/{userId}` | `RemoveMemberCommand` |
| `DELETE /api/leagues/{id}/members/me` | `LeaveLeagueCommand` |

Todos exigen sesión (`401` si no) y CSRF en los métodos que mutan. Autorización siempre en el handler con `LeagueAccess` (`403`), nunca confiando en el frontend. El token viaja en la ruta; no se registra en logs (revisar que ningún filtro de logging imprima URLs completas bajo `/api/invitations/`). Errores de token inválido/revocado: `404` genérico, sin distinguir causas.

## 7. Tests (Testcontainers con PostgreSQL)

| Test | Qué verifica |
| --- | --- |
| Migraciones | V5–V6 aplican sobre Postgres limpio y `ddl-auto: validate` pasa. |
| `LeagueSettings` / `ScoringRules` (unitarios) | Defaults = tabla de `requirements.md` (penalti +3 en todas las posiciones, doble amarilla −3); rechazo de valores fuera de rango. |
| `CreateLeagueCommand` | Crea liga con settings y 36 reglas, membership `ADMIN` e invitación; el creador aparece en `/api/me/leagues` con rol `ADMIN`. Rollback completo si falla un paso. |
| `UpdateLeagueSettingsCommand` | Admin edita; manager → `403`; valores inválidos → `400`; un admin de otra liga → `403`. |
| Invitaciones | Join con token válido crea `MANAGER`; token regenerado invalida el anterior; revocado → `404`; join repetido no duplica membership; solo se persiste el hash. |
| Miembros | Admin expulsa a manager; admin no puede expulsarse ni abandonar; manager abandona y deja de ver la liga (`403`). |
| Roles por liga | Mismo usuario `ADMIN` en A y `MANAGER` en B: permisos correctos en cada una. |
| Seguridad | Todos los endpoints sin sesión → `401`; mutaciones sin CSRF → `403`. |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde. |

## 8. Orden de ejecución

| # | Tarea | Resultado |
| --- | --- | --- |
| 0 | Crear módulo `datadefinition` con `Position` y `ScoringAction`, su test de arquitectura y la entrada en `AGENTS.md` | Tipos compartidos disponibles |
| 1 | Migración V5 + `LeagueSettings`, `ScoringRules` y sus tests unitarios | Defaults verificados contra la tabla de requisitos |
| 2 | Persistencia de settings y reglas; `CreateLeagueCommand` y `POST /api/leagues` | Liga creada de punta a punta |
| 3 | `GetLeagueQuery`, `GetLeagueSettingsQuery`, `UpdateLeagueSettingsCommand` | RF-07 completo |
| 4 | Migración V6 + `Invitation`; regenerar/revocar/preview/join | RF-06 completo |
| 5 | `UserDirectory` en `identity`; listar, expulsar y abandonar | Gestión de miembros |
| 6 | Tests de seguridad/roles y arquitectura; actualizar `AGENTS.md` (estado y comandos) y marcar el ítem en `requirements.md` | Ítem de Fase 1 cerrado |

Cada tarea es un commit `feat(league): … (RF-0x)` en Conventional Commits.

## 9. Riesgos y notas

- **Parámetros sin consumidor**: editar `lineup_size` o la tabla de puntos hoy no cambia nada observable. Es intencionado, pero conviene documentar en la API que son "configuración almacenada"; los consumidores los leerán en pasos posteriores.
- **Cambiar reglas con jornadas ya puntuadas**: sin política definida podrían alterar resultados históricos (ver decisión abierta 3).
- **Enlace filtrado**: cualquiera con el enlace y una cuenta Google entra. Mitigado por revocación/regeneración y porque el grupo es cerrado; caducidad y usos máximos quedan como extensión futura.
- **Fuerza bruta de tokens**: 32 bytes aleatorios (`SecureRandom`, base64url) hacen inviable la adivinanza; no hace falta rate limiting en v1.
- **Admin como manager**: ya decidido (el admin siempre juega); no hay que hacer nada aquí salvo no excluirlo de ningún listado de miembros.

## 10. Decisiones

Cerradas:

1. **Valor mínimo de mercado:** 1 M; valores y presupuesto en millones; presupuesto por defecto 100 M (desactivable con `lineup_budget_enabled`).
2. **`Position` y similares:** nuevo módulo `datadefinition` para definiciones sin lógica; lo que tenga lógica va a `shared`.

3. **Editar parámetros tras puntuar:** permitido y **no se recalcula** automáticamente; el recálculo es una acción explícita del admin (RF-16, Fase 4).
4. **Máximo de managers por liga:** sin límite en v1.
5. **Nombre de liga:** libre, no único; el identificador es el UUID.

No quedan decisiones abiertas para este paso.
