#!/usr/bin/env bash
# Restores a backup into a PostgreSQL database. Run it from your own machine, never from the server: the age
# private key and the AWS read credentials must not be there.
# Usage: restore.sh <s3-key> <postgres-uri>
#   e.g. restore.sh daily/buddyfantasy-2026-11-02T03-00-00Z-daily.dump.age postgresql://user:pass@localhost:5433/restored
# Environment: BACKUP_S3_BUCKET, AGE_KEY_FILE (age private key), AWS credentials with s3:GetObject, and
# AWS_ENDPOINT_URL only for tests against a local S3. Needs aws, age and pg_restore installed.
# The target database must exist; its content is replaced.
set -euo pipefail

key="${1:?usage: restore.sh <s3-key> <postgres-uri>}"
target="${2:?usage: restore.sh <s3-key> <postgres-uri>}"
: "${BACKUP_S3_BUCKET:?set BACKUP_S3_BUCKET}"
: "${AGE_KEY_FILE:?set AGE_KEY_FILE}"

aws s3 cp "s3://${BACKUP_S3_BUCKET}/${key}" - --only-show-errors \
  | age --decrypt --identity "$AGE_KEY_FILE" \
  | pg_restore --dbname "$target" --clean --if-exists --no-owner --exit-on-error

echo "restored ${key}"
