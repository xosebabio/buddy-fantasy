# 14 · Despliegue en el VPS — plan

Plan de los ítems pendientes de la **Fase 0**: _Dockerfile, Docker Compose (app + Postgres + Caddy) y pipeline de CI_ y _VPS con dominio, HTTPS y backup diario de la base de datos_. Cierra también _Repositorio, Spring Boot 3 con Spring Modulith y estructura de módulos_, hecho en el plan 01 y sin marcar a falta de la imagen y el despliegue. Es lo único que falta para la puerta de la Fase 2: la _Prueba real_ con los amigos necesita la app publicada en `buddyfantasy.com`. Las reglas son las de `docs/requirements.md` (RNF-02, RNF-03, RNF-06, RNF-07, RNF-11, RNF-16). Este plan no las repite: dice cómo y en qué orden se aplican.

## 1. Alcance

| Tema             | Decisión                                                                                                                                                                                                                                                                                                                     |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Imágenes         | Tres imágenes en GHCR: el backend, la web (Caddy con el build de Angular dentro) y el backup (§3).                                                                                                                                                                                                                           |
| Compose          | `deploy/docker-compose.prod.yml` con backend, Postgres, Caddy y el backup bajo un perfil; `deploy/.env.example` versionado, sin valores reales (§4).                                                                                                                                                                         |
| Ramas y CI/CD    | Rama `develop` para integrar y `main` para producción. La CI construye las imágenes en cada PR y las publica y despliega en cada push a `main` con los tres jobs en verde (§5).                                                                                                                                              |
| VPS              | Servidor de Hetzner Cloud con Docker, firewall de Hetzner, dominio `buddyfantasy.com` con HTTPS automático de Caddy y redirect URI de producción en Google (§6).                                                                                                                                                             |
| Backups          | `pg_dump` diario y antes de cada despliegue, cifrado, a un bucket de AWS S3, con retención y una restauración ensayada (§7).                                                                                                                                                                                                 |
| Secretos         | Solo en el `.env` del VPS y en el entorno `production` de GitHub; nada en el repo ni en las imágenes, que son públicos (§8).                                                                                                                                                                                                 |
| Fuera de alcance | Las fotos de los jugadores (decisión 4: sin S3 ni CloudFront para fotos por ahora). Un entorno de staging (decisión 8). Logs estructurados, alertas de caída y la restauración probada como rutina (Fase 4, RNF-08). La PWA (Fase 3). Cloud gestionado (Fase 5). |

## 2. Decisiones

Las marcadas como _Usuario_ se cerraron al redactar el plan. Las _Propuesta_ se revisaron con el usuario (tarea 1) y se aceptaron todas tal cual.

| #   | Tema                      | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Origen    |
| --- | ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| 1   | Proveedor y dominio       | VPS en Hetzner Cloud y el dominio `buddyfantasy.com`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Usuario   |
| 2   | Registro                  | GHCR (`ghcr.io/xosebabio/…`), con el `GITHUB_TOKEN` del workflow.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Usuario   |
| 3   | Despliegue                | Automático en cada push a `main`. Se crea la rama `develop` para el trabajo diario (decisión 7).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Usuario   |
| 4   | Fotos                     | Se dejan para el futuro, con S3. En producción no se configura `S3_*` ni `PHOTOS_BASE_URL`, y `PHOTOS_ORIGIN` y `UPLOADS_ORIGIN` de Caddy van vacías. El backend ya arranca sin S3 (el job `e2e` de CI lo hace): el cliente de AWS resuelve las credenciales en la primera llamada, así que solo fallarían los endpoints de foto, que no tienen interfaz (plan 13, decisión 1).                                                                                                                                                                                                                                         | Usuario   |
| 5   | Destino de los backups    | Un bucket de AWS S3 en la UE, privado, solo para backups.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Usuario   |
| 6   | Secretos en el VPS        | Un `.env` en el servidor (`/opt/buddy-fantasy/.env`, `chmod 600`), fuera del repo. El repo es público y _open source_: ver §8.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Usuario   |
| 7   | Ramas                     | `develop` pasa a ser la rama por defecto de GitHub: las ramas de trabajo abren PR contra ella. Publicar es un PR de `develop` a `main`. `main` queda protegida: solo por PR y con los jobs `backend`, `frontend` y `e2e` en verde. Los agentes trabajan sobre `develop` o una rama suya, nunca sobre `main` (se cambia _Flujo de trabajo_ en `AGENTS.md`).                                                                                                                                                                                                                                                             | Propuesta, aceptada |
| 8   | Staging                   | Ninguno: `develop` no se despliega. El E2E de CI corre contra las mismas imágenes que van a producción (decisión 11), y eso basta para un grupo de amigos.                                                                                                                                                                                                                                                                                                                                                                                                                                                             | Propuesta, aceptada |
| 9   | Imágenes y etiquetas      | `buddy-fantasy-backend`, `buddy-fantasy-web` y `buddy-fantasy-backup`, públicas como el repo (no llevan nada secreto, §8). Cada una con la etiqueta `sha-<7>` del commit y `latest`. El Compose usa `IMAGE_TAG` y el despliegue fija el `sha`, así que volver atrás es desplegar el `sha` anterior. Solo x86-64: el servidor también lo es, y así no hace falta QEMU en CI.                                                                                                                                                                                                                                            | Propuesta, aceptada |
| 10  | Cómo se despliega         | El job `deploy` (solo en push a `main`, tras los tres jobs) entra por SSH con un usuario `deploy` y una clave solo para eso, copia `docker-compose.prod.yml`, hace el backup previo (decisión 13), `docker compose pull` y `up -d --wait` con el `sha` nuevo, y termina con una comprobación de humo (§5.3). Con `concurrency` para que dos despliegues no se pisen. Unos segundos sin servicio mientras arranca el backend nuevo: aceptable para la liga.                                                                                                                                                           | Propuesta, aceptada |
| 11  | E2E contra las imágenes   | El job `e2e` deja de arrancar el jar y el Caddy de desarrollo: construye las imágenes de backend y web y corre el E2E contra ellas con `docker-compose.prod.yml` y un override de CI (`docker-compose.e2e.yml`) que activa el perfil `e2e`, sirve en `http://localhost:8081` y no tiene backup. Así se prueba el artefacto que se publica, con su Caddyfile y su CSP. Las imágenes que pasan son las que se suben, no se reconstruyen.                                                                                                                                                                               | Propuesta, aceptada |
| 12  | Perfil de producción      | `docker-compose.prod.yml` fija `SPRING_PROFILES_ACTIVE: prod` como literal, no desde el `.env`, para que el perfil `e2e` (login sin Google y reloj ajustable) no pueda entrar en producción por error. `application-prod.yml` solo trae lo propio de producción (`SESSION_COOKIE_SECURE: true`). La comprobación de humo del despliegue verifica que `/api/e2e/clock` no existe.                                                                                                                                                                                                                                       | Propuesta, aceptada |
| 13  | Backups                   | `pg_dump -Fc` diario a las 04:00 de Madrid (temporizador de systemd en el host que lanza el servicio `backup` del Compose) y antes de cada despliegue. Se cifra en el VPS con `age` y la **clave pública**; la privada la guarda el usuario fuera del servidor. Se sube a `s3://<bucket>/daily/` o `…/deploy/`. El usuario IAM del VPS solo tiene `s3:PutObject` en ese bucket: si alguien entra en el servidor, no puede leer ni borrar los backups. La retención la hace una regla de ciclo de vida del bucket (30 días en `daily/`, 14 en `deploy/`), y el bucket tiene el bloqueo de acceso público activado. | Propuesta, aceptada |
| 14  | Servidor                  | Hetzner Cloud, x86 de vCPU compartida con 4 GB y ubicación en la UE (Núremberg o Falkenstein), Ubuntu LTS con Docker CE. Se entra solo con clave, sin root por SSH y con `unattended-upgrades`. El firewall es el **Cloud Firewall de Hetzner** (22, 80, 443 TCP y 443 UDP para HTTP/3), no `ufw`: Docker publica puertos saltándose `ufw`. Sin los backups de Hetzner: la máquina se recrea con la guía (§6) y los datos salen del backup de la base de datos.                                                                                                                                                       | Propuesta, aceptada |
| 15  | `www`                     | `buddyfantasy.com` es el canónico y `www.buddyfantasy.com` redirige a él con un `301` desde Caddy, así que Google solo necesita un redirect URI. El bloque de `www` se activa con una variable, para no romper el Caddyfile en local (se resuelve en la tarea 4).                                                                                                                                                                                                                                                                                                                                                        | Propuesta, aceptada |
| 16  | Login de Google           | Además del redirect URI `https://buddyfantasy.com/login/oauth2/code/google`, la pantalla de consentimiento pasa de _Testing_ a _In production_: en _Testing_ solo entran los usuarios de prueba que se den de alta a mano. Con los scopes `openid`, `profile` y `email`, Google no pide verificación.                                                                                                                                                                                                                                                                                                                   | Propuesta, aceptada |
| 17  | Logs                      | Driver `json-file` de Docker con rotación (`max-size: 10m`, `max-file: 3`) en todos los servicios, para que no llenen el disco. Los logs estructurados y las alertas son de la Fase 4.                                                                                                                                                                                                                                                                                                                                                                                                                                | Propuesta, aceptada |

## 3. Imágenes

| Imagen                 | Contexto   | Construcción                                                                                                                                                                                                                                                                                                       |
| ---------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `buddy-fantasy-backend` | `backend/` | Multi-stage. Compila con Maven y Java 21 sin tests (ya los pasa el job `backend`) y extrae las capas del jar (`-Djarmode=tools extract --layers`). Corre sobre `eclipse-temurin:21-jre` sin root, con `-XX:MaxRAMPercentage=75` y `-Duser.timezone=UTC`. `HEALTHCHECK` contra `/actuator/health/readiness`.         |
| `buddy-fantasy-web`     | raíz       | Multi-stage. Hace el build de producción con Node 26 (`npm ci` y `npm run build`) y lo copia a `/srv` de `caddy:2.11-alpine` junto con `deploy/Caddyfile`. El Caddyfile no cambia, salvo `www` (decisión 15).                                                                                                    |
| `buddy-fantasy-backup`  | `deploy/backup/` | `postgres:16-alpine` (el mismo `pg_dump` que la base de datos) con `aws-cli` y `age`, y `backup.sh`: volcado, cifrado y subida, fallando si cualquier paso falla (`set -euo pipefail`). Junto a él, `restore.sh`, que se ejecuta desde la máquina del usuario y no desde el VPS (§7).                                   |

Cada imagen lleva su `.dockerignore` (sin `target/`, `node_modules/`, `dist/`, `test-results/` ni ningún `.env`). Las etiquetas OCI (`org.opencontainers.image.source`, `revision`) enlazan el paquete de GHCR con el repo. En CI se construyen con `docker/build-push-action` y la caché de GitHub Actions.

## 4. Compose de producción

`deploy/docker-compose.prod.yml`, en `/opt/buddy-fantasy/` del VPS junto al `.env`:

- **`postgres`**: `postgres:16-alpine` con el volumen `postgres-data`, `healthcheck` con `pg_isready` y **sin puertos publicados**.
- **`backend`**: `ghcr.io/xosebabio/buddy-fantasy-backend:${IMAGE_TAG}`. `depends_on` espera a que Postgres esté sano, con `SPRING_PROFILES_ACTIVE: prod` literal (decisión 12) y el resto del `.env`. Sin puertos publicados: solo lo ve Caddy por la red interna. Flyway migra al arrancar, como ahora.
- **`caddy`**: `ghcr.io/xosebabio/buddy-fantasy-web:${IMAGE_TAG}`, que publica `80`, `443` y `443/udp`. Volúmenes `caddy-data` (certificados: si se pierden, Let's Encrypt puede limitar las emisiones) y `caddy-config`. `SITE_ADDRESS=buddyfantasy.com`, y `BACKEND_UPSTREAM` por defecto (`backend:8080`).
- **`backup`**: `ghcr.io/xosebabio/buddy-fantasy-backup:${IMAGE_TAG}` con `profiles: [backup]`, así que `up -d` no lo arranca. Lo lanzan el temporizador y el despliegue con `docker compose --profile backup run --rm backup`.
- `restart: unless-stopped` y logs con rotación (decisión 17) en todos.

`deploy/.env.example` lista cada variable con un valor de ejemplo que no sirve (`DB_PASSWORD=change-me`) y un comentario. La tabla de variables está en §8.

## 5. Ramas y CI/CD

### 5.1 Ramas (decisión 7)

1. Se crea `develop` desde `main` y se sube. En GitHub pasa a ser la rama por defecto.
2. Se protege `main`: PR obligatorio, los checks `backend`, `frontend` y `e2e` obligatorios y sin push directo. Lo configura el usuario en GitHub, y se documenta.
3. `ci.yml` corre en `pull_request` y en `push` a `develop` y `main`.

### 5.2 Workflow

| Job        | Cuándo                         | Qué cambia                                                                                                                                                                                                                                         |
| ---------- | ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `backend`  | Siempre                        | Nada.                                                                                                                                                                                                                                              |
| `frontend` | Siempre                        | Nada.                                                                                                                                                                                                                                              |
| `images`   | Siempre                        | Nuevo. Construye las tres imágenes y las guarda como artefacto (`docker save`) para `e2e` y `publish`. Sin push.                                                                                                                                   |
| `e2e`      | Tras `backend`, `frontend` e `images` | Carga las imágenes y levanta `docker-compose.prod.yml` con `docker-compose.e2e.yml` (decisión 11) en vez del jar y el Caddy de desarrollo. El resto de pasos no cambia.                                                                   |
| `publish`  | Solo en push a `main`, tras `e2e` | Nuevo. Sube las imágenes ya probadas a GHCR con `sha-<7>` y `latest` (`permissions: packages: write` solo en este job).                                                                                                                       |
| `deploy`   | Solo en push a `main`, tras `publish` | Nuevo. En el entorno `production` de GitHub, con `concurrency: production`. Hace lo de la decisión 10 y la comprobación de §5.3.                                                                                                       |

Los jobs `publish` y `deploy` no corren en `pull_request`, así que un PR desde un fork nunca toca los secretos ni el registro. No se usa `pull_request_target`.

### 5.3 Comprobación tras el despliegue

Desde el job, contra `https://buddyfantasy.com`:

- `/actuator/health` responde `UP`.
- `/` sirve `index.html` con la CSP y HSTS.
- `/api/e2e/clock` responde `401` (decisión 12).
- `/api/me` sin sesión responde `401`.

Si algo falla, el job falla y avisa por el correo de GitHub Actions. Volver atrás es relanzar el despliegue con el `sha` anterior (`workflow_dispatch` con `image_tag`).

## 6. VPS

La guía `deploy/README.md` deja recrear el servidor desde cero. Los pasos del usuario en paneles externos van marcados como tales:

1. **(Usuario, Hetzner)**: servidor (decisión 14) con su clave SSH, Cloud Firewall, IPv4 e IPv6.
2. **(Usuario, registrador)**: registros `A` y `AAAA` de `buddyfantasy.com` y `www` a la IP del servidor.
3. **En el servidor** (`deploy/README.md`, con los comandos exactos): usuario `deploy` en el grupo `docker`, SSH solo con clave y sin root, `unattended-upgrades`, Docker CE con el plugin de Compose, `/opt/buddy-fantasy/` con el `.env` (`chmod 600`) y el temporizador de systemd del backup (`deploy/systemd/buddy-fantasy-backup.{service,timer}`, versionados).
4. **(Usuario, Google Cloud)**: redirect URI de producción y consentimiento en _In production_ (decisión 16).
5. **(Usuario, AWS)**: bucket, ciclo de vida, bloqueo de acceso público y usuario IAM con la política de la decisión 13 (la política va versionada en `deploy/backup/iam-policy.json`, con el nombre del bucket como marcador).
6. **(Usuario, GitHub)**: entorno `production` con los secretos de §8, `develop` como rama por defecto y la protección de `main`.
7. Primer despliegue: un push a `main` (o `workflow_dispatch`). Caddy pide el certificado al arrancar.

## 7. Backups

- **Qué:** `pg_dump -Fc` de la base de datos entera, que incluye las sesiones de Spring Session, comprimido por el propio formato y cifrado con `age -r <clave pública>`. Nombre: `buddyfantasy-<UTC ISO>-<daily|deploy>.dump.age`.
- **Cuándo:** a diario a las 04:00 de Madrid (`OnCalendar` con `Europe/Madrid` y `Persistent=true`, por si el servidor estaba apagado) y antes de cada despliegue. Si el backup previo falla, el despliegue se para.
- **Dónde:** el bucket S3 de la decisión 5, con la retención de la decisión 13.
- **Restaurar:** `deploy/backup/restore.sh` desde la máquina del usuario, con sus credenciales de AWS (de lectura) y la clave privada de `age`. Descarga, descifra y hace `pg_restore` contra el Postgres que se le indique. El procedimiento va en `deploy/README.md`.
- **Ensayo (tarea 9):** se restaura un backup real en un Postgres local, se arranca el backend contra él y se comprueba que la liga y la clasificación están. La restauración probada como rutina sigue siendo de la Fase 4.
- **Fallos:** hasta las alertas de la Fase 4, un backup diario que falla solo queda en `journalctl`. Se mira a mano tras la primera semana (§12).

## 8. Secretos y repo público (decisión 6)

El repo y las imágenes son públicos. Reglas:

- **Nada real en el repo.** `.gitignore` ya ignora `.env`. Se añade `deploy/.env.example` con valores falsos, y ningún ejemplo, test o documento lleva un secreto, un correo personal ni la IP del servidor.
- **Nada secreto en las imágenes.** Toda la configuración entra en tiempo de ejecución por el `.env`. La tarea 3 lo comprueba con `docker history` y revisando las capas.
- **GitHub Actions:** los secretos van solo en el entorno `production`, que solo puede usar `main`. Nada de `set -x` ni `echo` en pasos con secretos. `GITHUB_TOKEN` con permisos mínimos por job (`contents: read` por defecto).
- **Escaneo:** se activan _Secret scanning_ y _Push protection_ de GitHub, gratuitos en repos públicos (lo hace el usuario en la configuración del repo).
- **La clave privada de `age`** y las credenciales de lectura de AWS nunca pasan por el VPS ni por GitHub.

| Dónde                          | Variables                                                                                                                                                                                                                                         |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.env` del VPS                 | `IMAGE_TAG` (lo escribe el despliegue), `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `SITE_ADDRESS`, `BACKUP_S3_BUCKET`, `BACKUP_S3_REGION`, `BACKUP_AWS_ACCESS_KEY_ID`, `BACKUP_AWS_SECRET_ACCESS_KEY`, `BACKUP_AGE_RECIPIENT` |
| Entorno `production` de GitHub | `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (clave privada solo para desplegar), `DEPLOY_KNOWN_HOSTS` (huella del servidor, para no aceptar cualquiera)                                                                                         |
| Fijas en el Compose            | `SPRING_PROFILES_ACTIVE: prod`, `DB_URL` (`jdbc:postgresql://postgres:5432/${DB_NAME}`), `PHOTOS_ORIGIN` y `UPLOADS_ORIGIN` vacías (decisión 4)                                                                                                   |

Las credenciales del backup llevan el prefijo `BACKUP_` y solo las recibe el servicio `backup`. El backend no tiene credenciales de AWS, y así las fotos siguen sin configurar.

## 9. Tests y comprobaciones

- **Local, antes de CI (tarea 3):** se construyen las tres imágenes y se levanta `docker-compose.prod.yml` con el override `e2e` en `localhost`, y `npm run e2e` pasa contra él, igual que hará CI.
- **CI:** el E2E completo (humo, `matchday` y `admin-matchday`) contra las imágenes (decisión 11). Si un cambio rompe el Dockerfile o el Caddyfile, falla el PR, no el despliegue.
- **Backup:** `backup.sh` se prueba en local contra un bucket de prueba o SeaweedFS (`aws --endpoint-url`), y `restore.sh` con ese mismo volcado.
- **Producción:** la comprobación de humo de §5.3 en cada despliegue, y un login real con Google tras el primero.
- Sin tests nuevos de Java: el backend solo gana `application-prod.yml`. Si la tarea 3 descubre que hace falta tocar código (por ejemplo, el `HEALTHCHECK`), se añaden sus tests en esa tarea.

## 10. Cambios en la documentación

En el cambio de la tarea que los provoca:

- `deploy/README.md` (nuevo): la guía del servidor, los pasos del usuario en cada panel, desplegar y volver atrás, y backup y restauración.
- `commands.md`: construir y levantar las imágenes en local, la tabla de §8 y los secretos de GitHub, y el job de CI con sus jobs nuevos.
- `AGENTS.md`: _Flujo de trabajo_ con `develop` (decisión 7), una fila de `deploy/README.md` en el mapa y _Estado_.
- `testing.md`: el E2E de CI contra las imágenes.
- `architecture.md`: el despliegue (tres imágenes, Compose y Caddy delante) si no lo describe ya.
- `requirements.md`: se marcan los tres ítems de la Fase 0. Si cambia algo de la tabla de stack, también.
- `status.md`.

## 11. Orden de ejecución

| #   | Tarea                                                                                                                                       | Resultado                                | Hecho |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- | ----- |
| 1   | Revisar este plan con el usuario y cerrar las propuestas (§2)                                                                              | Decisiones cerradas                      | ✅    |
| 2   | Crear `develop`, hacerla rama por defecto y cambiar `ci.yml` a `develop` y `main`; _Flujo de trabajo_ de `AGENTS.md` (§5.1)                | Ramas de la decisión 7                   | ✅    |
| 3   | Dockerfiles y `.dockerignore` de backend y web, `application-prod.yml`, `docker-compose.prod.yml`, override `e2e` y `.env.example`; E2E local contra las imágenes (§3, §4, §9) | Imágenes y Compose (RNF-06)              | ✅    |
| 4   | Redirección de `www` en el Caddyfile sin romper el local (decisión 15)                                                                      | Dominio canónico                         |       |
| 5   | CI: jobs `images` y `e2e` contra las imágenes (§5.2)                                                                                        | El artefacto que se publica, probado     |       |
| 6   | Imagen de backup, `backup.sh`, `restore.sh`, política IAM y temporizador de systemd, probados en local (§7)                               | Backups listos (RNF-07)                  |       |
| 7   | `deploy/README.md` y pasos del usuario: Hetzner, DNS, Google, AWS y GitHub (§6, §8)                                                         | Servidor y servicios externos preparados |       |
| 8   | CI: jobs `publish` y `deploy` con la comprobación de humo y `workflow_dispatch` para volver atrás; primer despliegue y login real (§5.2, §5.3) | `https://buddyfantasy.com` en marcha     |       |
| 9   | Primer backup real en S3 y ensayo de la restauración (§7)                                                                                    | Restauración ensayada                    |       |
| 10  | Documentación de §10, `status.md`, _Estado_ de `AGENTS.md` y marcar los tres ítems de la Fase 0 en `requirements.md`                        | Fase 0 cerrada                           |       |

Cada tarea es un commit en Conventional Commits citando sus códigos: `build(deploy): …` para imágenes y Compose, `ci: …`, `feat(deploy): …` para backups, `docs: …`. El backup va antes que el despliegue porque el despliegue lo usa (decisión 13). La tarea 7 la hace casi toda el usuario en paneles externos: el agente prepara la guía y los ficheros.

## 12. Riesgos y notas

- **Migraciones y volver atrás:** Flyway migra al arrancar. Si un despliegue aplica una migración y hay que volver al `sha` anterior, la imagen vieja puede no entender el esquema nuevo. Para eso está el backup previo al despliegue: volver atrás con migración es restaurar ese backup y desplegar el `sha` anterior.
- **Arranque fallido:** si el backend nuevo no llega a sano, `up -d --wait` falla y el job también. La web queda caída hasta volver atrás. Es aceptable para la liga, y se ve en el correo de Actions.
- **Grupo `docker` = root:** el usuario `deploy` puede hacer de todo en el servidor a través de Docker. Por eso su clave solo vive en el entorno `production` de GitHub y la huella del servidor está fijada.
- **Certificados:** perder `caddy-data` obliga a pedir certificados otra vez. Si se recrea el servidor muchas veces seguidas, Let's Encrypt puede limitar. Para ensayar la guía, mejor usar el CA de staging de Let's Encrypt.
- **Backups silenciosos:** hasta la Fase 4 no hay alertas. Tras la primera semana se comprueba a mano que hay siete volcados en `daily/` y que la regla de ciclo de vida funciona.
- **Memoria:** backend, Postgres y Caddy caben en 4 GB con `MaxRAMPercentage=75` solo si no hay más contenedores. Si el backend crece, se ajusta su límite en el Compose antes que el tamaño del servidor.
- **Fotos más adelante:** al activarlas, el backend necesitará credenciales de AWS propias (sin el prefijo `BACKUP_`), `PHOTOS_ORIGIN` y `UPLOADS_ORIGIN` en Caddy y la regla CORS del bucket (`commands.md`, _CORS del bucket de fotos_).
