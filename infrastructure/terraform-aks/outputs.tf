output "cluster_name" {
  value = azurerm_kubernetes_cluster.main.name
}

output "resource_group_name" {
  value = azurerm_resource_group.aks.name
}

output "get_credentials" {
  description = "Comando para obtener el kubeconfig (autenticación con Entra ID)."
  value       = "az aks get-credentials -g ${azurerm_resource_group.aks.name} -n ${azurerm_kubernetes_cluster.main.name}"
}

output "key_vault_name" {
  value = azurerm_key_vault.main.name
}

output "csi_identity_client_id" {
  description = "userAssignedIdentityID del SecretProviderClass (overlays/cloud)."
  value       = azurerm_kubernetes_cluster.main.key_vault_secrets_provider[0].secret_identity[0].client_id
}

output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}
