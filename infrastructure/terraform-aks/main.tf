locals {
  tags = {
    project     = "coldchain-sentinel"
    environment = "prod"
    managed_by  = "terraform"
  }
}

data "azurerm_client_config" "current" {}

resource "random_string" "suffix" {
  length  = 5
  upper   = false
  special = false
}

resource "azurerm_resource_group" "aks" {
  name     = var.resource_group_name
  location = var.location
  tags     = local.tags
}

resource "azurerm_log_analytics_workspace" "aks" {
  name                = "log-coldchain-aks-${random_string.suffix.result}"
  location            = azurerm_resource_group.aks.location
  resource_group_name = azurerm_resource_group.aks.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = local.tags
}

resource "azurerm_kubernetes_cluster" "main" {
  name                = "aks-coldchain-${random_string.suffix.result}"
  location            = azurerm_resource_group.aks.location
  resource_group_name = azurerm_resource_group.aks.name
  dns_prefix          = "coldchain-${random_string.suffix.result}"
  kubernetes_version  = var.kubernetes_version
  sku_tier            = "Free"

  # Sin cuentas locales: el acceso pasa por Microsoft Entra ID y RBAC de Kubernetes.
  local_account_disabled            = length(var.admin_group_object_ids) > 0
  role_based_access_control_enabled = true
  oidc_issuer_enabled               = true
  workload_identity_enabled         = true
  automatic_upgrade_channel         = "patch"
  node_os_upgrade_channel           = "NodeImage"

  azure_active_directory_role_based_access_control {
    admin_group_object_ids = var.admin_group_object_ids
    azure_rbac_enabled     = false
    tenant_id              = data.azurerm_client_config.current.tenant_id
  }

  dynamic "api_server_access_profile" {
    for_each = length(var.operator_ip_ranges) > 0 ? [1] : []
    content {
      authorized_ip_ranges = var.operator_ip_ranges
    }
  }

  default_node_pool {
    name                        = "system"
    vm_size                     = var.node_vm_size
    auto_scaling_enabled        = true
    node_count                  = var.node_count
    min_count                   = var.node_count
    max_count                   = var.max_node_count
    os_disk_type                = "Managed"
    os_disk_size_gb             = 64
    temporary_name_for_rotation = "rotation"
    upgrade_settings {
      max_surge = "1"
    }
  }

  identity {
    type = "SystemAssigned"
  }

  # Azure CNI + Network Policy de Azure: las NetworkPolicy del repositorio se aplican.
  network_profile {
    network_plugin    = "azure"
    network_policy    = "azure"
    load_balancer_sku = "standard"
  }

  # Ingress administrado (NGINX de App Routing): clase webapprouting.kubernetes.azure.com.
  web_app_routing {
    dns_zone_ids = []
  }

  # Secretos de Key Vault montados y sincronizados como Secrets (CSI driver).
  key_vault_secrets_provider {
    secret_rotation_enabled  = true
    secret_rotation_interval = "2m"
  }

  # Métricas y logs de contenedores hacia Azure Monitor (complementa Prometheus).
  oms_agent {
    log_analytics_workspace_id      = azurerm_log_analytics_workspace.aks.id
    msi_auth_for_monitoring_enabled = true
  }

  tags = local.tags
}

# --- Gestión de secretos -----------------------------------------------------

resource "azurerm_key_vault" "main" {
  name                       = "kv-coldchain-${random_string.suffix.result}"
  location                   = azurerm_resource_group.aks.location
  resource_group_name        = azurerm_resource_group.aks.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  purge_protection_enabled   = true
  soft_delete_retention_days = 7
  tags                       = local.tags
}

# Quien ejecuta Terraform escribe los secretos; el add-on del clúster solo los lee.
resource "azurerm_role_assignment" "kv_operator" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

resource "azurerm_role_assignment" "kv_csi_reader" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_kubernetes_cluster.main.key_vault_secrets_provider[0].secret_identity[0].object_id
}

resource "random_password" "postgres" {
  length  = 32
  special = false
}

resource "random_password" "bootstrap_admin" {
  length  = 24
  special = true
}

resource "azurerm_key_vault_secret" "postgres_password" {
  name         = "postgres-password"
  value        = random_password.postgres.result
  key_vault_id = azurerm_key_vault.main.id
  content_type = "text/plain"
  depends_on   = [azurerm_role_assignment.kv_operator]
}

resource "azurerm_key_vault_secret" "bootstrap_admin_password" {
  name         = "bootstrap-admin-password"
  value        = random_password.bootstrap_admin.result
  key_vault_id = azurerm_key_vault.main.id
  content_type = "text/plain"
  depends_on   = [azurerm_role_assignment.kv_operator]
}

# --- Control de costos -------------------------------------------------------

resource "azurerm_consumption_budget_resource_group" "aks" {
  name              = "budget-coldchain-aks"
  resource_group_id = azurerm_resource_group.aks.id
  amount            = var.budget_amount
  time_grain        = "Monthly"

  time_period {
    start_date = var.budget_start_date
  }

  notification {
    enabled        = true
    threshold      = 80
    operator       = "GreaterThan"
    contact_emails = [var.alert_email]
  }
}
