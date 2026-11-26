#!/usr/bin/env sh
# Uso: commit-overlay.sh "<mensaje>". Confirma el nuevo estado deseado en Git.
set -eu
git config user.name "coldchain-cd[bot]"
git config user.email "coldchain-cd@users.noreply.github.com"
git add infrastructure/kubernetes/overlays
if git diff --cached --quiet; then echo "Sin cambios: el ambiente ya está en esa versión."; exit 0; fi
git commit -m "chore(cd): $1"
git push
