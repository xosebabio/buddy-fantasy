# Despliegue en el VPS

Guía para recrear el servidor desde cero (plan `docs/14-deploy-plan.md`, RNF-06 y RNF-07). El repo y las imágenes son públicos: **aquí no va ningún secreto, correo personal ni IP**. Los valores reales viven solo en el `.env` del servidor y en el entorno `production` de GitHub.

Los pasos marcados **(Usuario)** se hacen a mano en un panel externo; el resto son comandos en el servidor. Haz los pasos en este orden: el DNS tiene que haberse propagado antes del primer despliegue, porque Caddy pide el certificado al arrancar.

## Resumen

| Pieza | Dónde |
| --- | --- |
| Servidor | Hetzner Cloud, x86 compartido 4 GB, UE, Ubuntu LTS |
| Dominio | `buddyfantasy.com` (canónico) y `www.buddyfantasy.com` (301 al canónico) |
| Imágenes | `ghcr.io/xosebabio/buddy-fantasy-{backend,web,backup}`, etiquetas `sha-<7>` y `latest` |
| Stack | `/opt/buddy-fantasy/docker-compose.prod.yml` + `/opt/buddy-fantasy/.env` |
| Backups | `pg_dump` cifrado con `age` a un bucket privado de S3, diario y antes de cada despliegue |

## 1. Servidor de Hetzner (Usuario)

1. Crea un servidor x86 de vCPU compartida con 4 GB, ubicación Núremberg o Falkenstein, Ubuntu LTS, con IPv4 e IPv6 y tu clave SSH pública.
2. Crea un **Cloud Firewall** y asígnalo al servidor. Entrada permitida: TCP 22, TCP 80, TCP 443 y UDP 443 (HTTP/3). Todo lo demás, cerrado. No uses `ufw`: Docker publica puertos saltándoselo.
3. No actives los backups de Hetzner: los datos salen del backup de la base de datos (§6).

## 2. DNS (Usuario)

En el registrador, apunta a la IP del servidor:

| Tipo | Nombre | Valor |
| --- | --- | --- |
| `A` | `@` | IPv4 del servidor |
| `AAAA` | `@` | IPv6 del servidor |
| `A` | `www` | IPv4 del servidor |
| `AAAA` | `www` | IPv6 del servidor |

Comprueba que resuelve antes de seguir: `dig +short buddyfantasy.com`.

## 3. Preparar el servidor

Entra como `root` la primera vez y haz lo siguiente.

### Usuario `deploy` y SSH

```bash
adduser --disabled-password --gecos "" deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
```

Genera **en tu máquina** una clave solo para desplegar (`ssh-keygen -t ed25519 -f deploy_key -C buddy-fantasy-deploy`), copia la pública al servidor y guarda la privada para el secreto `DEPLOY_SSH_KEY` (§5). Bórrala de tu máquina cuando la hayas subido a GitHub.

```bash
echo '<contenido de deploy_key.pub>' >> /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys && chmod 600 /home/deploy/.ssh/authorized_keys
```

Añade también tu propia clave pública a ese fichero para entrar tú como `deploy`. Después, endurece SSH (solo clave, sin `root`):

```bash
cat > /etc/ssh/sshd_config.d/10-hardening.conf <<'EOF'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
EOF
sshd -t && systemctl reload ssh
```

**Antes de cerrar la sesión de `root`**, abre otra terminal y comprueba que `ssh deploy@<ip>` funciona.

### Actualizaciones automáticas

```bash
apt-get update && apt-get install -y unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades
```

### Docker CE

Sigue la guía oficial para Ubuntu (<https://docs.docker.com/engine/install/ubuntu/>, repositorio `apt` de Docker): instala `docker-ce`, `docker-ce-cli`, `containerd.io` y `docker-compose-plugin`. Después:

```bash
usermod -aG docker deploy
```

El grupo `docker` equivale a `root` en el servidor: por eso la clave de `deploy` solo vive en el entorno `production` de GitHub y la huella del servidor va fijada (§5).

### Directorio de la aplicación y `.env`

```bash
install -d -m 755 -o deploy -g deploy /opt/buddy-fantasy
```

Como `deploy`, crea `/opt/buddy-fantasy/.env` a partir de [`.env.example`](.env.example) con los valores reales, y protégelo:

```bash
nano /opt/buddy-fantasy/.env
chmod 600 /opt/buddy-fantasy/.env
```

| Variable | Valor |
| --- | --- |
| `IMAGE_TAG` | Déjalo en `latest`; el despliegue lo reescribe con el `sha-<7>` |
| `DB_NAME`, `DB_USER` | Los del ejemplo valen |
| `DB_PASSWORD` | Contraseña larga y aleatoria (`openssl rand -base64 32`) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Del cliente OAuth de §4 |
| `SITE_ADDRESS` | `buddyfantasy.com` |
| `BACKUP_S3_BUCKET`, `BACKUP_S3_REGION` | Los del bucket de §6 |
| `BACKUP_AWS_ACCESS_KEY_ID`, `BACKUP_AWS_SECRET_ACCESS_KEY` | Las del usuario IAM de solo escritura de §6 |
| `BACKUP_AGE_RECIPIENT` | La clave **pública** de `age` (`age1…`) |

No pongas `S3_*`, `PHOTOS_BASE_URL` ni credenciales de AWS para el backend: las fotos quedan fuera por ahora (decisión 4 del plan).

### Temporizador del backup diario

Con `deploy` perteneciendo al grupo `docker` y el `.env` ya en su sitio, instala las unidades versionadas en `deploy/systemd/` (cópialas desde el repo, por ejemplo con `scp` desde tu máquina):

```bash
install -m 644 buddy-fantasy-backup.service buddy-fantasy-backup.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now buddy-fantasy-backup.timer
systemctl list-timers buddy-fantasy-backup.timer
```

Se ejecuta a las 04:00 de Madrid (`Persistent=true`: si el servidor estaba apagado, lo lanza al arrancar). No funcionará hasta el primer despliegue, que es quien baja las imágenes y copia el Compose.

## 4. Google Cloud (Usuario)

En el cliente OAuth ya existente, en _APIs y servicios → Credenciales_:

1. Añade el URI de redirección autorizado `https://buddyfantasy.com/login/oauth2/code/google`. Los de local se quedan.
2. En _Pantalla de consentimiento de OAuth_, pasa el estado de _Testing_ a **_In production_**. En _Testing_ solo entran los usuarios de prueba dados de alta a mano. Con los scopes `openid`, `profile` y `email`, Google no pide verificación.
3. Copia el ID y el secreto del cliente al `.env` del servidor.
4. En _Google Auth Platform → Branding_, rellena los datos de la pantalla de consentimiento (RNF-10; textos en [`docs/14bis-legal-texts.md`](../docs/14bis-legal-texts.md)):
   - **Página principal:** `https://buddyfantasy.com`
   - **Política de privacidad:** `https://buddyfantasy.com/privacy`
   - **Condiciones del servicio:** `https://buddyfantasy.com/terms`
   - **Dominios autorizados:** `buddyfantasy.com`
   - **Correo de asistencia:** `docksidegarage@gmail.com`, el mismo que el contacto de los textos.

   Hazlo después del despliegue (§7), cuando las URL ya respondan. Con `openid`, `profile` y `email` no hace falta verificación. Si subes un logo, Google pide verificar la marca, y para eso exige el dominio verificado en Search Console, con estas mismas URL.

## 5. GitHub (Usuario)

En _Settings_ del repo:

1. **Ramas:** `develop` como rama por defecto. Protege `main`: PR obligatorio, checks `backend`, `frontend` y `e2e` obligatorios y sin push directo.
2. **Seguridad:** activa _Secret scanning_ y _Push protection_ (_Code security_), gratuitos en repos públicos.
3. **Entorno `production`** (_Environments_), limitado a la rama `main`, con estos secretos:

| Secreto | Valor |
| --- | --- |
| `DEPLOY_HOST` | IP o nombre del servidor |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_SSH_KEY` | La clave privada de §3 (`deploy_key`) |
| `DEPLOY_KNOWN_HOSTS` | `ssh-keyscan -t ed25519 <ip>`, verificando la huella contra la del panel de Hetzner o `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` en el servidor |

4. **Paquetes en GHCR:** tras la primera publicación (tarea 8), las tres imágenes nacen privadas. Cambia cada una a **pública** (_Packages → Package settings → Change visibility_) para que el servidor las baje sin credenciales. No llevan nada secreto.

## 6. AWS: bucket y usuarios de backup (Usuario)

Todo se hace desde tu máquina con la AWS CLI, con un perfil que pueda administrar S3 e IAM en la cuenta. El servidor no necesita AWS ni credenciales de administración. Los bloques son para `bash` o `zsh`, y se ejecutan desde la raíz del repo.

```bash
export AWS_PROFILE="<perfil-admin>"                      # perfil de la CLI con permisos de administración
aws sts get-caller-identity                              # comprueba que es la cuenta correcta
REGION=eu-west-1                                         # una región de la UE
BUCKET="buddy-fantasy-backups-$(openssl rand -hex 4)"    # nombre global único
```

Las variables se pierden al cerrar la terminal. Apunta `BUCKET` antes de cerrarla: es el `BACKUP_S3_BUCKET` del `.env`. Si lo vuelves a generar, obtienes otro nombre y otro bucket.

### Clave de `age`

En tu máquina (no es AWS): `age-keygen -o buddy-fantasy-age.key`. La línea `Public key: age1…` es `BACKUP_AGE_RECIPIENT`. **La privada (`buddy-fantasy-age.key`) se guarda fuera del servidor y de GitHub**, por ejemplo en tu gestor de contraseñas y una copia offline: sin ella, los backups no se pueden restaurar.

### Bucket

Privado, con el bloqueo de acceso público activado y cifrado por defecto.

```bash
aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" \
  --create-bucket-configuration LocationConstraint="$REGION"

aws s3api put-public-access-block --bucket "$BUCKET" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

aws s3api put-bucket-encryption --bucket "$BUCKET" --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

### Ciclo de vida

`daily/` expira a los 30 días y `deploy/` a los 14. Además, aborta las subidas incompletas a los 7 días y limpia los delete markers caducados.

```bash
aws s3api put-bucket-lifecycle-configuration --bucket "$BUCKET" --lifecycle-configuration '{
  "Rules": [
    {"ID": "expire-daily-30d", "Status": "Enabled", "Filter": {"Prefix": "daily/"}, "Expiration": {"Days": 30}},
    {"ID": "expire-deploy-14d", "Status": "Enabled", "Filter": {"Prefix": "deploy/"}, "Expiration": {"Days": 14}},
    {"ID": "abort-incomplete-multipart", "Status": "Enabled", "Filter": {"Prefix": ""}, "AbortIncompleteMultipartUpload": {"DaysAfterInitiation": 7}},
    {"ID": "expired-delete-markers", "Status": "Enabled", "Filter": {"Prefix": ""}, "Expiration": {"ExpiredObjectDeleteMarker": true}}
  ]
}'
```

### Usuario de escritura

`buddy-fantasy-backup`, sin contraseña de consola, con una sola política en línea: `s3:PutObject` sobre el bucket. Se genera a partir de [`backup/iam-policy.json`](backup/iam-policy.json), sustituyendo `BUCKET_NAME`. Quien entre en el servidor no puede leer ni borrar backups.

```bash
aws iam create-user --user-name buddy-fantasy-backup

POLICY=$(mktemp)
sed "s/BUCKET_NAME/$BUCKET/" deploy/backup/iam-policy.json > "$POLICY"
aws iam put-user-policy --user-name buddy-fantasy-backup \
  --policy-name upload-backups-only --policy-document "file://$POLICY"
rm "$POLICY"
```

La clave de acceso se guarda en un fichero con permisos restringidos, sin mostrarla en pantalla:

```bash
install -d -m 700 ~/.config/buddy-fantasy
read -r KEY_ID SECRET <<< "$(aws iam create-access-key --user-name buddy-fantasy-backup \
  --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)"
(umask 077 && printf 'BACKUP_S3_BUCKET=%s\nBACKUP_S3_REGION=%s\nBACKUP_AWS_ACCESS_KEY_ID=%s\nBACKUP_AWS_SECRET_ACCESS_KEY=%s\n' \
  "$BUCKET" "$REGION" "$KEY_ID" "$SECRET" > ~/.config/buddy-fantasy/backup-aws.env)
unset KEY_ID SECRET
```

Copia esas cuatro líneas al `.env` del servidor (§3) y borra `backup-aws.env` de tu máquina cuando estén allí.

### Usuario de restauración

`buddy-fantasy-restore`, solo lectura (`s3:GetObject` y `s3:ListBucket`), para `restore.sh` (§8). Sus credenciales se quedan en tu máquina y nunca van al servidor.

```bash
aws iam create-user --user-name buddy-fantasy-restore

POLICY=$(mktemp)
cat > "$POLICY" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {"Sid": "ReadBackups", "Effect": "Allow", "Action": "s3:GetObject", "Resource": "arn:aws:s3:::$BUCKET/*"},
    {"Sid": "ListBackups", "Effect": "Allow", "Action": "s3:ListBucket", "Resource": "arn:aws:s3:::$BUCKET"}
  ]
}
EOF
aws iam put-user-policy --user-name buddy-fantasy-restore \
  --policy-name read-backups-only --policy-document "file://$POLICY"
rm "$POLICY"

read -r KEY_ID SECRET <<< "$(aws iam create-access-key --user-name buddy-fantasy-restore \
  --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)"
(umask 077 && printf 'AWS_ACCESS_KEY_ID=%s\nAWS_SECRET_ACCESS_KEY=%s\nAWS_DEFAULT_REGION=%s\nBACKUP_S3_BUCKET=%s\n' \
  "$KEY_ID" "$SECRET" "$REGION" "$BUCKET" > ~/.config/buddy-fantasy/restore-aws.env)
unset KEY_ID SECRET
```

Para restaurar (§8), carga esas credenciales en la sesión con `set -a; . ~/.config/buddy-fantasy/restore-aws.env; set +a`.

### Comprobar

```bash
aws s3api get-public-access-block --bucket "$BUCKET"
aws s3api get-bucket-encryption --bucket "$BUCKET"
aws s3api get-bucket-lifecycle-configuration --bucket "$BUCKET"
aws iam list-user-policies --user-name buddy-fantasy-backup
aws iam list-user-policies --user-name buddy-fantasy-restore
```

Si repites un paso que ya estaba hecho, la CLI responde con `EntityAlreadyExists` o `BucketAlreadyOwnedByYou` y puedes seguir con el siguiente. La excepción es la clave de acceso: cada `create-access-key` crea una nueva, y cada usuario admite dos como máximo. Para revocar una: `aws iam list-access-keys --user-name <usuario>` y después `aws iam delete-access-key --user-name <usuario> --access-key-id <id>`.

## 7. Desplegar y volver atrás

El primer despliegue es un push (PR de `develop` a `main`) o un `workflow_dispatch` del workflow de CI, una vez hechas las secciones anteriores. El job `deploy` copia el Compose, hace el backup previo (si falla, se para), baja las imágenes del `sha` nuevo, ejecuta `up -d --wait` y comprueba `https://buddyfantasy.com`. Caddy pide el certificado a Let's Encrypt al arrancar; si recreas el servidor muchas veces para ensayar, ten en cuenta sus límites de emisión.

Para **volver atrás**, relanza el workflow con `workflow_dispatch` y el `image_tag` del `sha` anterior. Si el despliegue que fallaba aplicó una migración de Flyway, la imagen vieja puede no entender el esquema: restaura primero el backup `deploy/` previo (§8) y despliega el `sha` anterior.

Comandos útiles en el servidor, como `deploy`, desde `/opt/buddy-fantasy`:

```bash
docker compose --env-file .env -f docker-compose.prod.yml ps
docker compose --env-file .env -f docker-compose.prod.yml logs -f backend
journalctl -u buddy-fantasy-backup.service -n 50     # último backup diario
sudo systemctl start buddy-fantasy-backup.service    # backup a mano (como root)
```

## 8. Restaurar un backup

Se hace **desde tu máquina**, con `aws`, `age` y `pg_restore` instalados, las credenciales de lectura de AWS y la clave privada de `age`. El servidor no tiene nada de eso.

```bash
# Lista los backups (los más recientes al final)
aws s3 ls s3://<bucket>/daily/ ; aws s3 ls s3://<bucket>/deploy/

# Postgres destino vacío (aquí uno local; la base de datos debe existir)
docker run -d --name restore-pg -e POSTGRES_PASSWORD=restore -e POSTGRES_DB=restored -p 5433:5432 postgres:16-alpine

BACKUP_S3_BUCKET=<bucket> AGE_KEY_FILE=buddy-fantasy-age.key \
  deploy/backup/restore.sh daily/buddyfantasy-<fecha>-daily.dump.age \
  postgresql://postgres:restore@localhost:5433/restored
```

Para restaurar sobre producción tras una migración fallida, para el backend (`docker compose … stop backend`), abre un túnel SSH al Postgres del servidor (no publica puertos) o ejecuta el `pg_restore` dentro del contenedor `postgres`, y vuelve a levantar el `sha` anterior. El contenido de la base de datos se sustituye por completo.

## 9. Tras la primera semana

Hasta las alertas de la Fase 4, un backup que falla solo queda en `journalctl`. Comprueba a mano que hay siete volcados en `daily/` y que la regla de ciclo de vida funciona.

## 10. Borrar una cuenta a mano (hasta la Fase 4)

Hasta que la app permita borrar la cuenta (RNF-10; plan `docs/14bis-legal-pages-plan.md`, §7), el responsable lo hace a mano en el plazo de un mes desde la petición, como dice la política. Se hace en el servidor, con el backend levantado. Cada borrado va en una sola transacción.

**No uses este procedimiento si la cuenta es admin de alguna liga.** Ver el paso 2.

Abre `psql` en el contenedor de Postgres como `deploy`, desde `/opt/buddy-fantasy`:

```bash
docker compose --env-file .env -f docker-compose.prod.yml exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

**1. Localizar la cuenta.** Con el correo de la cuenta de Google que pide el borrado:

```sql
SELECT id, google_sub FROM identity_user WHERE email = '<correo>';
```

- Ninguna fila: la cuenta ya no existe. Responde a la persona y termina.
- Más de una fila: para. No borres a ojo; decide con la persona cuál es la cuenta.

Guarda `id` y `google_sub` para los pasos siguientes. No los copies a ningún sitio público, ni al repo ni a un issue.

**2. Comprobar que no es admin.**

```sql
SELECT league_id FROM league_membership WHERE user_id = '<id>' AND role = 'ADMIN';
```

Si devuelve alguna fila, **para aquí**. Un admin no puede salir de su liga (`ADMIN_CANNOT_LEAVE`), y sin él la liga se queda sin admin. Decidido: al borrar la cuenta de un admin, su liga se cierra (desaparece para todos sus miembros, con sus datos de juego). El procedimiento SQL para cerrarla todavía no está escrito: hasta que lo esté, para aquí y no lo hagas a mano.

**3. Borrar.** Primero las sesiones, porque `principal_name` es el `sub` de Google. `spring_session_attributes` se borra en cascada.

```sql
BEGIN;
DELETE FROM spring_session WHERE principal_name = '<google_sub>';
DELETE FROM league_membership WHERE user_id = '<id>';
DELETE FROM identity_user WHERE id = '<id>';
COMMIT;
```

Si algún paso da error, escribe `ROLLBACK;` y no sigas.

**4. Comprobar.** Las tres consultas devuelven `0`:

```sql
SELECT count(*) FROM identity_user WHERE id = '<id>';
SELECT count(*) FROM spring_session WHERE principal_name = '<google_sub>';
SELECT count(*) FROM league_membership WHERE user_id = '<id>';
```

**5. Lo que se queda, a propósito.**

- Las alineaciones (`lineup_*`), los puntos y los votos (`scoring_lineup*`, `rating_vote`, `rating_exclusion`) y la auditoría de partidos (`match_audit_entry`) guardan el `user_id`. Sin `identity_user` no se puede volver a relacionar con la persona. En la liga sale como «Antiguo miembro», igual que quien ha salido.
- `event_publication` conserva los eventos ya completados, con ids y estadísticas, sin nombres ni correos.
- Si la persona vuelve a entrar con la misma cuenta de Google, se crea una cuenta nueva, sin ligas.
- Las copias de S3 caducan solas: `daily/` a los 30 días y `deploy/` a los 14 (§6).
