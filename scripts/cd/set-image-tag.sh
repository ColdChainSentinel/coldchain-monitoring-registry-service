#!/usr/bin/env sh
# Uso: set-image-tag.sh <overlay> <tag>
# Fija la etiqueta de las cinco imágenes de ColdChain en el kustomization.yaml.
set -eu
overlay="$1"; tag="$2"
echo "$tag" | grep -Eq '^[0-9a-f]{7,40}$' || { echo "Etiqueta inválida: $tag" >&2; exit 1; }
sed -i -E "s#^(    newTag: ).*#\1$tag#" "$overlay/kustomization.yaml"
grep -c "newTag: $tag" "$overlay/kustomization.yaml" | grep -qx 5 || { echo "No se actualizaron las 5 imágenes" >&2; exit 1; }
