#!/usr/bin/env sh
# Uso: current-tag.sh <overlay>. Imprime la etiqueta común de las imágenes.
set -eu
tags=$(sed -n -E 's#^    newTag: (.*)#\1#p' "$1/kustomization.yaml" | sort -u)
[ "$(echo "$tags" | wc -l)" -eq 1 ] || { echo "El overlay mezcla etiquetas: $tags" >&2; exit 1; }
echo "$tags"
