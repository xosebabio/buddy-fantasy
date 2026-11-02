# Backup image (RNF-07): pg_dump, encryption with age and upload to S3. It carries no secrets: the credentials
# and the age public key come from the environment of the `backup` service of docker-compose.prod.yml.
# The same major version as the database, so pg_dump and the server understand each other.
FROM postgres:16-alpine
ARG REVISION=unknown
LABEL org.opencontainers.image.source="https://github.com/xosebabio/buddy-fantasy" \
      org.opencontainers.image.revision="${REVISION}"
RUN apk add --no-cache aws-cli age bash
COPY backup.sh /usr/local/bin/backup.sh
RUN adduser -S -H backup
USER backup
ENTRYPOINT ["/usr/local/bin/backup.sh"]
