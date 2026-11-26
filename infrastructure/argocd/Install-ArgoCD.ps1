#Requires -Version 5.1
<#
.SYNOPSIS
  Instala Argo CD (versión fijada), lo expone en argocd.localhost y registra el
  proyecto y las aplicaciones coldchain-dev y coldchain-prod.
#>
param(
    [string]$Context = 'docker-desktop',
    [string]$Version = 'v2.13.3'
)
$ErrorActionPreference = 'Stop'
function Invoke-Kubectl {
    & kubectl --context $Context @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}
if (-not (Invoke-Kubectl get namespace argocd --ignore-not-found -o name)) {
    Invoke-Kubectl create namespace argocd
}
Invoke-Kubectl apply -n argocd --server-side --force-conflicts `
    -f "https://raw.githubusercontent.com/argoproj/argo-cd/$Version/manifests/install.yaml"
Invoke-Kubectl -n argocd patch configmap argocd-cmd-params-cm --type merge -p '{"data":{"server.insecure":"true"}}'
Invoke-Kubectl -n argocd rollout restart deployment/argocd-server
foreach ($d in @('argocd-server', 'argocd-repo-server', 'argocd-applicationset-controller')) {
    Invoke-Kubectl -n argocd rollout status "deployment/$d" --timeout=300s
}
Invoke-Kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s
Invoke-Kubectl apply -k $PSScriptRoot
Write-Output 'Argo CD: http://argocd.localhost:8088 (usuario admin; contraseña en el Secret argocd-initial-admin-secret)'
