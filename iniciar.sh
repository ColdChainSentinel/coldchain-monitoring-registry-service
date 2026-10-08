#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -f .env ]; then
  umask 077
  printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)" > .env
fi
docker compose up --build -d
echo 'Swagger: http://localhost:8080/swagger-ui.html'
