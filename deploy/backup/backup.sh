#!/usr/bin/env bash
# Dumps the database, encrypts it with the age public key and uploads it to S3.
# Usage: backup.sh <daily|deploy>
# Environment: PGHOST, PGUSER, PGPASSWORD, PGDATABASE (read by pg_dump), BACKUP_S3_BUCKET, BACKUP_AGE_RECIPIENT,
# and the AWS credentials and region. AWS_ENDPOINT_URL is only for tests against a local S3.
set -euo pipefail

kind="${1:?usage: backup.sh <daily|deploy>}"
case "$kind" in daily | deploy) ;; *) echo "unknown kind: $kind" >&2; exit 2 ;; esac
: "${BACKUP_S3_BUCKET:?set BACKUP_S3_BUCKET}"
: "${BACKUP_AGE_RECIPIENT:?set BACKUP_AGE_RECIPIENT}"

key="${kind}/buddyfantasy-$(date -u +%Y-%m-%dT%H-%M-%SZ)-${kind}.dump.age"

# Step by step through a temporary file and not in one pipeline: a failed pg_dump must never leave an
# (empty or truncated) object in the bucket that looks like a backup.
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

pg_dump --format=custom --no-owner --file "$tmp/dump"
age --recipient "$BACKUP_AGE_RECIPIENT" --output "$tmp/dump.age" "$tmp/dump"
aws s3 cp "$tmp/dump.age" "s3://${BACKUP_S3_BUCKET}/${key}" --only-show-errors

echo "backup uploaded: s3://${BACKUP_S3_BUCKET}/${key}"
