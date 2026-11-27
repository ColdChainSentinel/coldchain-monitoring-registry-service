output "frontend_url" {
  description = "Public URL of the web application (also proxies /registry, /telemetry, /alerts and /identity)."
  value       = "https://${azurerm_container_app.frontend.ingress[0].fqdn}"
}

output "telemetry_url" {
  description = "Public origin of Telemetry Ingestion, used by the simulator."
  value       = "https://${azurerm_container_app.java["telemetry-ingestion"].ingress[0].fqdn}"
}

output "resource_group_name" {
  value = azurerm_resource_group.main.name
}

output "postgres_fqdn" {
  value = azurerm_postgresql_flexible_server.main.fqdn
}

output "simulator_function_name" {
  value = azurerm_function_app_flex_consumption.simulator.name
}

output "bootstrap_admin_username" {
  description = "ADMIN created by Identity Access on its first start."
  value       = var.bootstrap_admin_username
}

output "bootstrap_admin_password" {
  description = "Password of the bootstrap ADMIN. Read it with `terraform output -raw bootstrap_admin_password`."
  value       = random_password.bootstrap_admin.result
  sensitive   = true
}
