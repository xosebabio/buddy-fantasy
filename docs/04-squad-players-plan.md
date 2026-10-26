# 04 · Jugadores del equipo (`squad`) — plan

Segundo ítem de la **Fase 1 · Núcleo jugable** (`requirements.md`): *CRUD de jugadores con valor inicial* (RF-09 a RF-11). Se apoya en `league` (roles por liga, `LeagueAccess`) y en `datadefinition` (`Position`).

## 1. Alcance

| Tema | Decisión |
| --- | --- |
| Alta/edición | Solo el administrador de la liga. Campos: nombre, dorsal (opcional), posición (obligatoria), valor inicial y foto (opcional) (RF-09, RF-10). |
| Baja | **Borrado lógico** (`retired_at`). El jugador dado de baja desaparece de la plantilla activa y de futuras alineaciones/convocatorias, pero se conserva para partidos, puntos y alineaciones pasadas. Sin reactivación en v1. |
| Dorsal | Opcional. Único entre jugadores **activos** de la liga (índice único parcial); la baja libera el dorsal. |
| Foto | Paso aparte tras el alta: Angular la redimensiona a **WebP ~400 px**, pide una **URL prefirmada (PUT)**, sube directo a S3 y **confirma** al backend, que guarda la clave. El fichero nunca pasa por Spring Boot. Bucket privado servido solo por **CloudFront (OAC)**, que además actúa de caché. |
| Visibilidad | Cualquier miembro de la liga ve la plantilla y las fichas; el admin ve además los jugadores dados de baja. |
| Papel del valor | **Presupuesto de la alineación** (RF-29): la suma de los valores de los jugadores alineados no puede superar `lineup_budget` cuando la liga lo tiene activo (**activado por defecto**). El valor se comprueba al guardar y se vuelve a comprobar al bloquear, con el valor vigente en cada momento (lo gestionan `lineup` y `market`, no `squad`). |
| Escala de valores | La define el admin sin ayuda ni validación del servidor (en millones, `> 0`). |
| Ficha (RF-11) | Esta fase entrega la ficha base: datos del jugador y valor inicial/actual. Historial de puntos por jornada (`scoring`) y gráfica de valor (`market`) llegan con esos módulos. |

Fuera de alcance: frontend, estadísticas y puntos del jugador, algoritmo de mercado, convocatorias, reactivar jugadores, recorte/redimensionado de imágenes en servidor o con Lambda (lo hace Angular antes de subir), infraestructura como código de AWS (se documenta cómo montarla, §5).

## 2. Modelo de datos (migración nueva, no se edita V1–V6)

**V7__squad_create_tables.sql**

- `squad_player`:
  - `id` UUID PK, `league_id` UUID NOT NULL (referencia lógica a `league_league`, sin FK entre módulos, como `league_membership.user_id`).
  - `name` VARCHAR(100) NOT NULL, `shirt_number` SMALLINT NULL `CHECK (shirt_number BETWEEN 0 AND 99)`.
  - `position` VARCHAR(20) NOT NULL `CHECK` sobre `Position`.
  - `initial_value` NUMERIC(10,2) NOT NULL `CHECK (initial_value > 0)` (millones).
  - `photo_key` VARCHAR(200) NULL: **clave del objeto, nunca la URL** (`leagues/{leagueId}/players/{playerId}/{uuid}.webp`). La URL se compone al responder, así se puede cambiar de bucket, CDN o proveedor sin tocar la BD.
  - `created_at`, `retired_at` TIMESTAMPTZ (`retired_at` NULL = activo).
- Índices: `(league_id)` y único parcial `(league_id, shirt_number) WHERE retired_at IS NULL AND shirt_number IS NOT NULL`.

**Valor del jugador y propiedad de los datos:** `squad` es dueño del *valor inicial* (atributo del jugador, RF-10). El histórico de valores de mercado (RF-34–36) será de `market`, que al nacer se alimentará del evento `PlayerRegistered(leagueId, playerId, initialValue, at)` publicado por `squad` para sembrar su primer punto. Mientras `market` no exista, el valor actual = valor inicial. Así no hay tablas compartidas ni joins entre módulos.

## 3. Dominio (`squad/domain`, sin Spring ni JPA)

- `Player` (aggregate root): `register(leagueId, name, shirtNumber, position, initialValue, now)`, `update(...)` (sin foto), `changePhoto(photoKey)`, `removePhoto()`, `retire(now)`. Invariantes en el dominio: nombre no vacío (≤100), dorsal 0–99, valor inicial > 0, no editar un jugador dado de baja.
- `PlayerPhotoKey` (VO): genera la clave `leagues/{leagueId}/players/{playerId}/{uuid}.webp` y comprueba que una clave recibida pertenece a ese jugador (`belongsTo(leagueId, playerId)`).
- `PlayerId`, `ShirtNumber` (VO opcional). La unicidad del dorsal se garantiza con el índice único; el handler la comprueba antes para devolver `409` limpio y captura la violación de constraint como red de seguridad por si hay concurrencia.
- Eventos de dominio (`shared.domain.DomainEvent`): `PlayerRegistered`, `PlayerRetired`. Sin consumidores todavía; son contrato para `market`/`lineup`.

## 4. Casos de uso (`squad/application/usecases/<caso>/`)

| Caso | Tipo | Permiso | Notas |
| --- | --- | --- | --- |
| `RegisterPlayerCommand` | command | `requireAdmin` | Alta con valor inicial, sin foto (RF-09, RF-10). Publica `PlayerRegistered`. |
| `UpdatePlayerCommand` | command | `requireAdmin` | Reemplazo completo de los campos editables salvo la foto. Ver la decisión confirmada 1 sobre editar el valor inicial. |
| `RetirePlayerCommand` | command | `requireAdmin` | Borrado lógico, idempotente. Publica `PlayerRetired`. |
| `ListPlayersQuery` | query | `requireMember` | Plantilla activa, ordenada por posición y dorsal; `includeRetired` solo si el solicitante es admin. Lectura con una sola consulta (sin N+1). |
| `GetPlayerQuery` | query | `requireMember` | Ficha base (RF-11). `404` si el jugador es de otra liga: el `leagueId` de la ruta debe coincidir. |
| `RequestPlayerPhotoUploadCommand` | command | `requireAdmin` | Valida tipo y tamaño, genera la clave y devuelve la URL prefirmada (§5). No toca la BD. Jugador dado de baja → `409`. |
| `ConfirmPlayerPhotoCommand` | command | `requireAdmin` | Comprueba que la clave es de ese jugador y que el objeto existe con tipo y tamaño válidos; guarda la clave y borra la foto anterior (best effort). Idempotente con la misma clave. |
| `RemovePlayerPhotoCommand` | command | `requireAdmin` | Quita la clave y borra el objeto (best effort). Idempotente. |

Cada handler recibe `userId` y `leagueId` y llama a `LeagueAccess` primero. El `GetPlayerQuery` y los de escritura cargan el jugador **por `(leagueId, playerId)`** para impedir acceso cruzado entre ligas (IDOR).

## 5. Fotos: S3 + CloudFront

### Flujo

1. **Redimensionar en el navegador.** Angular recorta y comprime la imagen a **WebP de ~400 px** (avatar) antes de subirla, con `canvas.toBlob('image/webp', ~0.8)`. Suele quedar en decenas de KB. No hay Lambda ni procesado en servidor: es una pieza más que de momento no hace falta.
2. **Pedir la URL.** `POST /api/leagues/{id}/players/{playerId}/photo/upload` con `{contentType, sizeBytes}`. El backend solo acepta `image/webp` y como máximo **512 KB**, y genera la clave `leagues/{leagueId}/players/{playerId}/{uuid}.webp` (UUID aleatorio, **inmutable**). Devuelve `{uploadUrl, headers, photoKey, expiresAt}`.
3. **La URL limita tipo y tamaño.** Es un **PUT prefirmado** (caduca en 5 min) que **firma** `Content-Type`, `Content-Length` y `Cache-Control: public, max-age=31536000, immutable`. Si el cliente envía otro tipo, otro tamaño o un fichero mayor del declarado, S3 rechaza la subida. Nadie puede subir 50 MB ni algo que no sea la imagen declarada.
4. **Subir.** El navegador hace `PUT` directo a S3 con las cabeceras devueltas. El fichero nunca pasa por Spring Boot, lo que ahorra memoria y ancho de banda en el VPS.
5. **Confirmar.** `PUT /api/leagues/{id}/players/{playerId}/photo` con `{photoKey}`. El backend comprueba que la clave es de ese jugador (`PlayerPhotoKey.belongsTo`) y hace `HeadObject` para verificar que existe, es `image/webp` y no pasa de 512 KB. Si no, `400`. Guarda la clave y borra la foto anterior.
6. **Servir.** La API devuelve `photoUrl = PHOTOS_BASE_URL + "/" + photoKey` (CloudFront).

**Por qué PUT y no POST prefirmado:** un POST con *policy* permite `content-length-range` sin declarar el tamaño antes, pero el AWS SDK v2 para Java no lo genera y habría que firmar la policy SigV4 a mano. El PUT con `Content-Length` firmado da la misma garantía: el backend comprueba el tamaño declarado contra el máximo y S3 exige ese tamaño exacto. Además, la confirmación lo vuelve a verificar con `HeadObject`.

**Por qué clave nueva en cada subida:** cambiar la foto crea otra clave, por lo que la caché de CloudFront y de los navegadores nunca queda obsoleta ni hace falta invalidar. La caché dura un año con seguridad.

### Puerto y adaptador

- Puerto **`ImageStorage`** en `squad/application/port/out` (`createUpload(key, contentType, sizeBytes)`, `find(key)` —tipo y tamaño vía `HeadObject`—, `delete(key)`), con los records `ImageUpload` (`uploadUrl`, `headers`, `key`, `expiresAt`) y `StoredImage` (`contentType`, `sizeBytes`). Es genérico: no sabe de jugadores; la clave la decide `PlayerPhotoKey`.
- Adaptador `S3ImageStorage` en `adapter/out/storage` con el **AWS SDK v2** (`software.amazon.awssdk:s3`, presignado con `S3Presigner`). Se descarta Spring Cloud AWS: para tres operaciones, el SDK directo es una dependencia menos y sin autoconfiguración que revisar. El dominio y la capa de aplicación no conocen S3.
- Vive en `squad` mientras sea su único usuario. Si otro módulo necesita imágenes (p. ej. avatares en `identity`), se sube a `shared` en ese momento.
- Reloj inyectable (`Clock`) para `expiresAt`; nunca `Instant.now()` directo.

### Configuración (variables de entorno, sin secretos en el repo)

| Variable | Uso |
| --- | --- |
| `S3_BUCKET`, `S3_REGION` | Bucket de fotos. |
| `S3_ENDPOINT` | Opcional; si está definida (SeaweedFS en local, `http://localhost:8333`) se usa con *path-style*. |
| `PHOTOS_BASE_URL` | Origen CloudFront en producción; en local, `http://localhost:8333/<bucket>`. |
| Credenciales | Cadena por defecto del SDK (rol IAM en producción, variables `AWS_*` en local). Nada en disco. |

### Infraestructura AWS (manual por ahora; IaC queda para Fase 0/5)

- Bucket S3 con *Block Public Access* activo y sin ACLs.
- Distribución CloudFront con **Origin Access Control** al bucket; política del bucket que solo permite `s3:GetObject` a esa distribución. HTTPS, compresión activa, política de caché gestionada `CachingOptimized`.
- CORS del bucket: método `PUT`, cabeceras `Content-Type` y `Cache-Control`, origen = dominio de la app.
- Identidad de la app con `s3:PutObject`, `s3:GetObject` (para `HeadObject`) y `s3:DeleteObject` limitado al prefijo `leagues/*/players/*`. Nada de `ListBucket` ni permisos globales.
- Privacidad: el bucket no es accesible directamente; las fotos solo se sirven por CloudFront. Las claves son UUID inadivinables (decisión: CDN público, sin cookies firmadas). Cualquiera con la URL exacta puede ver esa foto.

### Desarrollo local

`deploy/docker-compose.dev.yml` añade **SeaweedFS** (`chrislusf/seaweedfs`, S3 en el puerto 8333) con credenciales solo locales y lectura anónima (`deploy/seaweedfs/s3.json`), y un job de inicialización (`amazon/aws-cli`) que crea el bucket y su CORS para `http://localhost:4200` (`deploy/seaweedfs/cors.json`). Se descartó MinIO porque sus imágenes oficiales ya no se pueden descargar de Docker Hub ni de quay.io. Documentar las variables en `AGENTS.md` (*Comandos*).

## 6. API HTTP (`squad/adapter/in/web`)

| Método y ruta | Caso |
| --- | --- |
| `POST /api/leagues/{id}/players` | `RegisterPlayerCommand` → `201` |
| `GET /api/leagues/{id}/players` (`?includeRetired=true` solo admin) | `ListPlayersQuery` |
| `GET /api/leagues/{id}/players/{playerId}` | `GetPlayerQuery` |
| `PUT /api/leagues/{id}/players/{playerId}` | `UpdatePlayerCommand` |
| `DELETE /api/leagues/{id}/players/{playerId}` | `RetirePlayerCommand` → `204` |
| `POST /api/leagues/{id}/players/{playerId}/photo/upload` | `RequestPlayerPhotoUploadCommand` |
| `PUT /api/leagues/{id}/players/{playerId}/photo` | `ConfirmPlayerPhotoCommand` |
| `DELETE /api/leagues/{id}/players/{playerId}/photo` | `RemovePlayerPhotoCommand` → `204` |

Todos exigen sesión (`401`) y CSRF en los que mutan. Autorización en el handler (`403`); jugador inexistente o de otra liga → `404`. `409` por dorsal duplicado. `400` por datos inválidos (posición, valor ≤ 0, tipo distinto de WebP, más de 512 KB, clave de otro jugador o sin objeto subido). `includeRetired=true` de un manager se ignora.

## 7. Tests

| Test | Qué verifica |
| --- | --- |
| Migración V7 | Aplica sobre Postgres limpio y `ddl-auto: validate` pasa; el índice único parcial libera el dorsal al dar de baja. |
| `Player` (unitarios) | Invariantes (nombre, dorsal 0–99, valor > 0, no editar dado de baja); `retire` idempotente. |
| Alta/edición | Admin crea y edita; manager → `403`; admin de otra liga → `403`; posición o valor inválidos → `400`; dorsal repetido entre activos → `409`; dorsal reutilizable tras la baja. |
| Baja lógica | El jugador deja de salir en la lista de un manager y sigue saliendo en la del admin con `includeRetired`; la ficha sigue accesible. |
| Aislamiento entre ligas | Acceder/editar un jugador de la liga A usando la ruta de la B → `404`. |
| Fotos (adaptador) | Contra **SeaweedFS con Testcontainers** (verifica firmas SigV4 como S3): la URL prefirmada permite subir exactamente lo declarado y rechaza otro tipo/tamaño o sin `Cache-Control`; `find` y `delete` funcionan. El resto de tests usan un fake de `ImageStorage`. |
| Fotos (casos de uso) | Solo `image/webp` ≤ 512 KB → si no, `400`; la clave generada sigue el formato `leagues/{leagueId}/players/{playerId}/{uuid}.webp`; confirmar una clave de otro jugador o de otra liga, o sin objeto subido → `400`; confirmar borra la foto anterior y repetir la misma confirmación no borra nada; quitar la foto es idempotente; jugador dado de baja → `409`; manager → `403`. |
| Eventos | `PlayerRegistered` y `PlayerRetired` se publican una vez por operación. |
| Seguridad | Todos los endpoints sin sesión → `401`; mutaciones sin CSRF → `403` (ampliar `ApiSecurityTests`). |
| Arquitectura | `ModularityTests` y `HexagonalArchitectureTests` en verde (`squad` depende de `league`, `datadefinition`, `shared`). |

## 8. Orden de ejecución

| # | Tarea | Resultado |
| --- | --- | --- |
| 1 | Migración V7, `Player` y eventos, con tests unitarios; actualizar `allowedDependencies` de `squad` | Dominio verificado |
| 2 | Persistencia, `RegisterPlayerCommand`, `GetPlayerQuery`, `ListPlayersQuery` y sus endpoints (sin foto) | RF-09/RF-10/RF-11 base |
| 3 | `UpdatePlayerCommand` y `RetirePlayerCommand` (con dorsal y baja lógica) | CRUD completo |
| 4 | SeaweedFS en el compose dev, puerto de almacenamiento y adaptador S3 con su test | Subida y borrado de objetos (hecho) |
| 4 bis | Ajustar lo hecho a la ampliación: renombrar `PlayerPhotoStorage` → `ImageStorage`, `S3PlayerPhotoStorage` → `S3ImageStorage`, `PhotoUpload`/`StoredPhoto` → `ImageUpload`/`StoredImage`; sacar la foto de `Player.register`/`update` y añadir `changePhoto`/`removePhoto` y `PlayerPhotoKey` | Puerto genérico y foto como paso aparte |
| 5 | `RequestPlayerPhotoUploadCommand`, `ConfirmPlayerPhotoCommand`, `RemovePlayerPhotoCommand`, sus endpoints y `photoUrl` en las respuestas | Fotos de punta a punta |
| 6 | Tests de seguridad y arquitectura; `AGENTS.md` (estado, comandos, variables) y marcar el ítem en `requirements.md` | Ítem de Fase 1 cerrado |

Cada tarea es un commit `feat(squad): … (RF-09)` en Conventional Commits.

## 9. Riesgos y notas

- **Objetos huérfanos en S3:** una subida que nunca se confirma queda huérfana. Pesan pocos KB (WebP de 400 px) y son pocos; no se limpia en v1. Como la clave lleva el `playerId`, un job con ShedLock puede borrar más adelante todo lo que haya bajo el prefijo de un jugador y no sea su foto actual.
- **Límite de tamaño:** la firma de `Content-Length` obliga a declarar el tamaño exacto al pedir la URL. Si el navegador envía otro, S3 rechaza el `PUT`. Como Angular genera el WebP antes de pedir la URL, conoce el tamaño exacto.
- **Soporte de WebP:** todos los navegadores actuales codifican WebP con `canvas.toBlob`. Si alguno no lo hiciera, `toBlob` devuelve PNG y el backend lo rechaza (`400`); el frontend debe avisar en vez de fallar en silencio.
- **Alta en dos pasos:** la foto necesita el `playerId`, así que el frontend da de alta al jugador y después sube la foto. Si la subida falla, el jugador queda creado sin foto, que es un estado válido.
- **Foto visible con la URL:** consecuencia de la decisión de CDN público; aceptable para fotos de un grupo de amigos. Si más adelante se quiere restringir a logueados, se migra a cookies firmadas de CloudFront sin cambiar el modelo de datos.
- **Coste:** S3 + CloudFront para este volumen cabe en la capa gratuita; añade una dependencia de cuenta AWS que el VPS aún no tenía (la Fase 5 ya la contemplaba).
- **Valor inicial editable:** ver propuesta 1. Al ser presupuesto, un cambio puede invalidar alineaciones ya guardadas.
- **Presupuesto y mercado:** como el valor sube y baja tras cada jornada, una alineación válida puede dejar de serlo. Qué pasa en ese caso (rechazo o mantener la última válida) se decide al planificar `lineup`.
- **Rendimiento:** la plantilla es de ~15–25 jugadores; una consulta por liga y sin N+1 cumple RNF-09.

## 10. Decisiones

Cerradas:

1. **Baja:** borrado lógico, sin reactivación en v1.
2. **Foto:** S3 privado + CloudFront con OAC y caché; subida directa desde el navegador por **PUT prefirmado** que firma tipo y tamaño (no POST con policy); confirmación al backend; claves `leagues/{leagueId}/players/{playerId}/{uuid}.webp` inmutables, guardando la clave y no la URL; acceso público a CloudFront (sin cookies firmadas).
3. **Dev local:** SeaweedFS en el compose dev y Testcontainers (sustituye a MinIO, cuyas imágenes oficiales ya no están disponibles).
4. **Dorsal:** opcional y único entre activos.
5. **Ficha:** base ahora; historial de puntos y gráfica de valor con `scoring` y `market`.

6. **Papel del valor:** presupuesto de la alineación (cierra la decisión abierta de `AGENTS.md`). Se usa el valor vigente al guardar y al bloquear. `squad` solo guarda y publica el valor inicial; la validación del presupuesto es de `lineup`, que leerá el valor vigente de `market` por su API pública. Hasta que exista `market`, el valor vigente es el inicial.
7. **Escala de valores iniciales:** sin ayuda ni validación del servidor. Con 7 jugadores y 100 M de presupuesto, la media por jugador ronda los 14 M, pero cuadrarlo queda en manos del admin.
8. **Presupuesto activado por defecto:** `lineup_budget_enabled` pasa a `true` en `LeagueSettings.defaults()` (las ligas nuevas lo traen activo, 100 M). El flag se mantiene para que el admin pueda desactivarlo; entonces el valor es solo informativo en esa liga. Las ligas ya existentes conservan su valor (V5 las sembró en `false`); no hay datos reales, así que no se migran.
9. **Formato y tamaño:** Angular redimensiona a WebP ~400 px; el backend solo acepta `image/webp` ≤ 512 KB. Sin Lambda de miniaturas.
10. **Almacenamiento:** puerto genérico `ImageStorage` en `squad` con el AWS SDK v2 (sin Spring Cloud AWS); SeaweedFS en local.

Confirmadas:

1. **Editar el valor inicial:** permitido mientras no haya jornadas cerradas. Como `match`/`market` aún no existen, hoy no se comprueba; cuando exista la primera jornada cerrada se bloqueará la edición del valor inicial y solo `market` lo moverá. Con el presupuesto en juego, un cambio posterior podría dejar alineaciones fuera de presupuesto; por eso se bloquea.
2. **`includeRetired` para un manager:** se **ignora** (devuelve solo activos) en vez de dar `403`, para no complicar al frontend.
3. **Valor mínimo de liga (`market_min_value`):** el valor inicial solo exige `> 0`. Exigir `≥ market_min_value` obligaría a crear una API pública `LeagueSettingsProvider` en `league`; se hará cuando `market` la necesite.

No quedan decisiones abiertas para este paso.
