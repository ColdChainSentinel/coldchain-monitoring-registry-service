locals {
  simulator_active = var.simulator_enabled && var.simulator_point_id != ""
}

resource "azurerm_storage_account" "functions" {
  name                            = "stcoldchain${random_string.suffix.result}"
  location                        = azurerm_resource_group.main.location
  resource_group_name             = azurerm_resource_group.main.name
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  tags                            = local.tags
}

resource "azurerm_storage_container" "functions" {
  name                  = "simulator-deployment"
  storage_account_id    = azurerm_storage_account.functions.id
  container_access_type = "private"
}

resource "azurerm_service_plan" "functions" {
  name                = "asp-coldchain-${random_string.suffix.result}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  os_type             = "Linux"
  sku_name            = "FC1"
  tags                = local.tags
}

# Code is published separately (see README); Terraform owns only the host
# and its settings.
resource "azurerm_function_app_flex_consumption" "simulator" {
  name                = "func-coldchain-sim-${random_string.suffix.result}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  service_plan_id     = azurerm_service_plan.functions.id
  tags                = local.tags

  storage_container_type      = "blobContainer"
  storage_container_endpoint  = "${azurerm_storage_account.functions.primary_blob_endpoint}${azurerm_storage_container.functions.name}"
  storage_authentication_type = "StorageAccountConnectionString"
  storage_access_key          = azurerm_storage_account.functions.primary_access_key
  runtime_name                = "python"
  runtime_version             = "3.12"
  maximum_instance_count      = 40
  instance_memory_in_mb       = 512

  app_settings = {
    TELEMETRY_BASE_URL    = "https://${azurerm_container_app.java["telemetry-ingestion"].ingress[0].fqdn}"
    MONITORING_POINT_ID   = var.simulator_point_id
    SIMULATED_TEMPERATURE = tostring(var.simulator_temperature)
    SIMULATOR_DRY_RUN     = "false"
    SIMULATOR_SCHEDULE    = var.simulator_schedule
    # Cold starts of the APIs can take ~40 s; the simulator caps this at 30.
    HTTP_TIMEOUT_SECONDS = "30"
    # The timer stays disabled until a real monitoring point is configured.
    "AzureWebJobs.emit_measurement.Disabled" = local.simulator_active ? "false" : "true"
  }

  site_config {}

  lifecycle {
    precondition {
      condition     = !var.simulator_enabled || var.simulator_point_id != ""
      error_message = "simulator_enabled requires simulator_point_id (a monitoring point UUID registered in Registry)."
    }
  }
}
