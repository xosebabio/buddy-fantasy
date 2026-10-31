# Comandos y configuración

Mantén este archivo actualizado cuando añadas comandos, servicios o variables de entorno.

## Comandos

Desde la raíz del repo:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d   # Postgres (5432) y SeaweedFS para fotos (S3 en 8333; crea el bucket)
cd backend
mvn -B verify                                           # build + todos los tests (requiere Docker para Testcontainers)
                                                        # informe de cobertura (JaCoCo, sin umbral) en target/site/jacoco/index.html
                                                        # la JVM de los tests corre en Europe/Madrid (argLine del pom.xml)
mvn test -Dtest='LineupLockTests' -Djacoco.skip=true    # una clase, sin instrumentar la cobertura
mvn test -Dtest='ModularityTests,HexagonalArchitectureTests'   # solo verificación de arquitectura (sin Docker)
mvn test -Dtest='OpenApiSpecTests' -Djacoco.skip=true  # regenera api/openapi.json (falla si había cambiado: súbelo)
S3_ENDPOINT=http://localhost:8333 AWS_ACCESS_KEY_ID=buddyfantasy AWS_SECRET_ACCESS_KEY=buddyfantasy-local \
  mvn spring-boot:run                                   # arranque local contra los servicios de arriba
```

- Docker 29+ exige Testcontainers ≥ 1.21.4 (ya fijado en `pom.xml`).
- Lint del backend: pendiente.

### Frontend

Con Node 26 LTS (el de `frontend/.nvmrc`; Angular 22 no admite Node 25), desde `frontend/`:

```bash
npm ci
npm start              # ng serve en http://localhost:4200; proxy.conf.json manda /api, /oauth2 y /login/oauth2
                       # a localhost:8080 sin reescribir Host (/login es la pantalla de Angular)
npm run build          # build de producción en dist/buddy-fantasy/browser (sin /_ui, con autoCsp)
npm run lint           # ESLint (TS y plantillas, boundaries, i18n con id) y Stylelint (solo tokens), sin avisos
npm run format         # Prettier sobre TS, HTML, SCSS y JSON
npm run format:check   # lo mismo, sin escribir (CI)
npm test               # Vitest con jsdom, una pasada; npm test -- --watch para quedarse mirando
npm run palette        # regenera src/app/ui/styles/_palette.scss (versionado) desde scripts/palette.mjs
npm run api:types      # regenera src/app/core/api/schema.d.ts (versionado) desde ../api/openapi.json
npm run i18n:extract   # regenera src/locale/messages.xlf (versionado) desde el build de producción
```

`/_ui` (catálogo de componentes) solo existe con `npm start`.

### Caddy en local

Sirve el build de producción como en producción (`deploy/Caddyfile`: estáticos, proxy al backend, cabeceras y CSP), con el backend arrancado en el host:

```bash
cd frontend && npm run build && cd ..
docker compose -f deploy/docker-compose.dev.yml --profile caddy up -d   # además, Caddy en http://localhost:8081
docker compose -f deploy/docker-compose.dev.yml --profile caddy restart caddy   # tras otro build o un cambio del Caddyfile
```

- Manda `/api`, `/oauth2`, `/login/oauth2` y `/actuator/health` al backend; el resto es la SPA, con *fallback* a `index.html` (sin caché), salvo los ficheros con hash (`immutable` un año y, si no existen, `404`).
- La política de scripts es la `<meta>` que escribe `autoCsp`; la cabecera de Caddy solo la limita al mismo origen, porque las dos se aplican a la vez (ver el comentario del `Caddyfile`).
- Para el login con Google desde aquí, autoriza también su redirect URI (*Login con Google*).

## API

La documentación OpenAPI de la API está en `/v3/api-docs` (con sesión iniciada), versionada en `api/openapi.json`, y, con `SWAGGER_UI_ENABLED=true`, en Swagger UI (`/swagger-ui.html`).

## Variables de entorno

Valores por defecto en `backend/src/main/resources/application.yml`.

| Variable | Para qué |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `SWAGGER_UI_ENABLED` | Sirve Swagger UI |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Cliente OAuth de Google Cloud; sin ellas el login no funciona, pero la app arranca. Redirect URIs autorizados: ver *Login con Google* |
| `SESSION_COOKIE_SECURE` | `true` en producción, `false` en local |
| `league.defaults` (p. ej. `LEAGUE_DEFAULTS_LINEUP_SIZE`) | Valores con los que nace una liga |
| `league.limits`, `squad.limits`, `match.limits` (p. ej. `MATCH_LIMITS_SHEETPLAYERSMAX`) | Límites de negocio |
| `squad.photos.s3.upload-ttl` | Validez de la URL de subida de fotos |
| `LINEUP_LOCK_JOB_CRON` | Job de bloqueo de alineaciones (cada minuto; `-` lo desactiva, como en los tests, que lo ejecutan a mano) |
| `RATING_CLOSE_JOB_CRON` | Job de cierre de las ventanas de valoración (cada minuto; `-` lo desactiva, como en los tests) |
| `EVENTS_RESUBMIT_JOB_CRON` | Job que reenvía los eventos cuyo listener falló (cada 5 minutos, solo los de hace al menos `shared.events.resubmit-job.min-age`; `-` lo desactiva, como en los tests) |
| `S3_BUCKET`, `S3_REGION` | Bucket de fotos |
| `S3_ENDPOINT` | Solo en local; vacía en AWS |
| `PHOTOS_BASE_URL` | Origen desde el que se sirven las fotos: CloudFront en producción, `http://localhost:8333/buddy-fantasy-photos` en local |

### Caddy

Del `deploy/Caddyfile` (en local las fija el servicio `caddy` de `deploy/docker-compose.dev.yml`):

| Variable | Para qué |
| --- | --- |
| `SITE_ADDRESS` | Dominio en producción (HTTPS automático y HSTS); por defecto `:80`, HTTP sin dominio |
| `BACKEND_UPSTREAM` | `host:puerto` del backend; por defecto `backend:8080` (en local, `host.docker.internal:8080`) |
| `PHOTOS_ORIGIN` | Origen de las fotos en `img-src` de la CSP: el de `PHOTOS_BASE_URL` (CloudFront en producción, `http://localhost:8333` en local) |
| `UPLOADS_ORIGIN` | Origen del bucket en `connect-src` de la CSP, para la subida directa de fotos (`https://<bucket>.s3.<región>.amazonaws.com` en producción, `http://localhost:8333` en local) |

Las credenciales de S3 salen de la cadena por defecto del AWS SDK (rol IAM en producción, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` en local; las de SeaweedFS están en `deploy/seaweedfs/s3.json` y solo valen en local).

## Login de pruebas (E2E)

Con el perfil `e2e` (`SPRING_PROFILES_ACTIVE=e2e`; solo para los E2E, nunca en producción: al arrancar lo avisa con un `WARN`), `POST /api/e2e/login` con `{"email", "displayName"}` registra o reutiliza al usuario (sujeto `e2e:<email>`) y abre su sesión sin Google, como el login real. Pasa por CSRF: antes, un `GET` cualquiera (`/actuator/health`) da la cookie `XSRF-TOKEN`, cuyo valor va en la cabecera `X-XSRF-TOKEN`. Responde `204`; no está en el spec. Sin el perfil la ruta no existe (`401`).

## Login con Google

El backend construye el redirect URI con el origen de la petición (`server.forward-headers-strategy: framework`: detrás de Caddy, con `X-Forwarded-Proto` y `X-Forwarded-Host`), así que en el cliente OAuth de Google van autorizados todos los orígenes desde los que se entra:

| Entorno | Redirect URI |
| --- | --- |
| Front en local (`ng serve`, su proxy conserva `Host`) | `http://localhost:4200/login/oauth2/code/google` |
| Caddy en local (perfil `caddy`) | `http://localhost:8081/login/oauth2/code/google` |
| Backend solo (sin front: empieza en `http://localhost:8080/oauth2/authorization/google`) | `http://localhost:8080/login/oauth2/code/google` |
| Producción | `https://<dominio>/login/oauth2/code/google` |

Tras el login el backend redirige siempre a `/` (la SPA recupera la ruta guardada) y, si falla, a `/login?error`. `/login` es la pantalla de Angular: el proxy solo manda al backend `/api`, `/oauth2` y `/login/oauth2`. Si el proxy reescribiera `Host`, el redirect URI saldría con `localhost:8080` y Google lo rechazaría con `redirect_uri_mismatch`.

## CORS del bucket de fotos

El navegador sube la foto con un `PUT` directo al bucket y la lee con `GET`, así que el bucket admite el origen del front:

- **En local:** `deploy/seaweedfs/cors.json` (`PUT` y `GET` desde `http://localhost:4200` y, con Caddy, `http://localhost:8081`; cabeceras `Content-Type` y `Cache-Control`).
- **En producción (S3):** una regla CORS en el bucket con `AllowedOrigins` `https://<dominio>`, `AllowedMethods` `PUT` y `GET`, `AllowedHeaders` `Content-Type` y `Cache-Control`. Se configura en el bucket (consola o `aws s3api put-bucket-cors`), no en la app.
