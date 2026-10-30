resource "random_password" "pg_admin" {
  length  = 32
  special = false
}

resource "random_password" "service_db" {
  for_each = local.databases

  length  = 32
  special = false
}

# B1MS + 32 GB matches the 12-month free allowance of Azure for Students.
resource "azurerm_postgresql_flexible_server" "main" {
  name                          = "psql-coldchain-${random_string.suffix.result}"
  location                      = azurerm_resource_group.main.location
  resource_group_name           = azurerm_resource_group.main.name
  version                       = "16"
  sku_name                      = "B_Standard_B1ms"
  storage_mb                    = 32768
  auto_grow_enabled             = false
  backup_retention_days         = 7
  geo_redundant_backup_enabled  = false
  public_network_access_enabled = true
  administrator_login           = "coldchainadmin"
  administrator_password        = random_password.pg_admin.result
  tags                          = local.tags

  lifecycle {
    # Azure picks the availability zone; keep Terraform from fighting it.
    ignore_changes = [zone]
  }
}

resource "azurerm_postgresql_flexible_server_database" "service" {
  for_each = local.databases

  name      = each.value.name
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

# 0.0.0.0 is Azure's convention for "allow Azure services" (Container Apps
# consumption environments have no fixed outbound IP without a VNet).
resource "azurerm_postgresql_flexible_server_firewall_rule" "azure_services" {
  name             = "allow-azure-services"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "operator" {
  for_each = toset(var.operator_ip_addresses)

  name             = "operator-${replace(each.value, ".", "-")}"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = each.value
  end_ip_address   = each.value
}

# Creates one login role per service and hands it ownership of its database,
# so Flyway can create the schema while other roles cannot connect.
resource "terraform_data" "db_roles" {
  triggers_replace = [
    filesha256("${path.module}/scripts/bootstrap-db-roles.sql"),
    sha256(jsonencode({ for key, password in random_password.service_db : key => password.result })),
    [for database in azurerm_postgresql_flexible_server_database.service : database.id],
  ]

  provisioner "local-exec" {
    command = "psql -X -v ON_ERROR_STOP=1 -f ${path.module}/scripts/bootstrap-db-roles.sql"

    environment = {
      PGHOST                = azurerm_postgresql_flexible_server.main.fqdn
      PGPORT                = "5432"
      PGDATABASE            = "postgres"
      PGUSER                = azurerm_postgresql_flexible_server.main.administrator_login
      PGPASSWORD            = random_password.pg_admin.result
      PGSSLMODE             = "require"
      REGISTRY_DB_PASSWORD  = random_password.service_db["registry"].result
      TELEMETRY_DB_PASSWORD = random_password.service_db["telemetry"].result
      ALERTS_DB_PASSWORD    = random_password.service_db["alerts"].result
    }
  }

  depends_on = [azurerm_postgresql_flexible_server_firewall_rule.operator]
}
