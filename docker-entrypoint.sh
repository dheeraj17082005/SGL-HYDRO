#!/bin/sh
set -eu

# Render supplies PostgreSQL connection strings as postgresql:// URLs; Spring/JDBC expects jdbc:postgresql://.
if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    postgresql://*) export SPRING_DATASOURCE_URL="jdbc:${DATABASE_URL}" ;;
    postgres://*) export SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_URL#postgres://}" ;;
  esac
fi

exec java -jar /app/app.jar
