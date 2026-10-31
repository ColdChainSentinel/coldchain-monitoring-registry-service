output "frontend_url" {
  description = "Public URL of the web application (also proxies /registry, /telemetry and /alerts)."
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
