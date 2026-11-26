#!/usr/bin/env sh
# Equivalente local del job "build" de cd-gitops.yml para el clúster k3s en Docker:
# construye las cinco imágenes con el SHA de HEAD y las importa en containerd.
# Uso: scripts/cd/build-local-images.sh [contenedor-k3s]
set -eu
node="${1:-coldchain-k3s}"
tag=$(git rev-parse --short=12 HEAD)
registry=ghcr.io/coldchainsentinel
images=""
for service in identity-access monitoring-registry telemetry-ingestion alert-management frontend; do
  context="services/$service"; [ "$service" = frontend ] && context=frontend
  docker build -q --build-arg "APP_VERSION=$tag" -t "$registry/$service:$tag" "$context"
  images="$images $registry/$service:$tag"
done
# shellcheck disable=SC2086
docker save $images | docker exec -i "$node" ctr -n k8s.io images import -
echo "$tag"
