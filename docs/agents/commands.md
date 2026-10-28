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
- Lint: pendiente.

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

Las credenciales de S3 salen de la cadena por defecto del AWS SDK (rol IAM en producción, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` en local; las de SeaweedFS están en `deploy/seaweedfs/s3.json` y solo valen en local).

## Login de pruebas (E2E)

Con el perfil `e2e` (`SPRING_PROFILES_ACTIVE=e2e`; solo para los E2E, nunca en producción: al arrancar lo avisa con un `WARN`), `POST /api/e2e/login` con `{"email", "displayName"}` registra o reutiliza al usuario (sujeto `e2e:<email>`) y abre su sesión sin Google, como el login real. Pasa por CSRF: antes, un `GET` cualquiera (`/actuator/health`) da la cookie `XSRF-TOKEN`, cuyo valor va en la cabecera `X-XSRF-TOKEN`. Responde `204`; no está en el spec. Sin el perfil la ruta no existe (`401`).

## Login con Google

El backend construye el redirect URI con el origen de la petición (`server.forward-headers-strategy: framework`: detrás de Caddy, con `X-Forwarded-Proto` y `X-Forwarded-Host`), así que en el cliente OAuth de Google van autorizados todos los orígenes desde los que se entra:

| Entorno | Redirect URI |
| --- | --- |
| Front en local (`ng serve`, su proxy conserva `Host`) | `http://localhost:4200/login/oauth2/code/google` |
| Backend solo (sin front: empieza en `http://localhost:8080/oauth2/authorization/google`) | `http://localhost:8080/login/oauth2/code/google` |
| Producción | `https://<dominio>/login/oauth2/code/google` |

Tras el login el backend redirige siempre a `/` (la SPA recupera la ruta guardada) y, si falla, a `/login?error`. `/login` es la pantalla de Angular: el proxy solo manda al backend `/api`, `/oauth2` y `/login/oauth2`. Si el proxy reescribiera `Host`, el redirect URI saldría con `localhost:8080` y Google lo rechazaría con `redirect_uri_mismatch`.

## CORS del bucket de fotos

El navegador sube la foto con un `PUT` directo al bucket y la lee con `GET`, así que el bucket admite el origen del front:

- **En local:** `deploy/seaweedfs/cors.json` (`PUT` y `GET` desde `http://localhost:4200`, cabeceras `Content-Type` y `Cache-Control`).
- **En producción (S3):** una regla CORS en el bucket con `AllowedOrigins` `https://<dominio>`, `AllowedMethods` `PUT` y `GET`, `AllowedHeaders` `Content-Type` y `Cache-Control`. Se configura en el bucket (consola o `aws s3api put-bucket-cors`), no en la app.
