locals {
  private_registry = var.registry_username != "" && var.registry_password != ""

  jdbc_base_url = "jdbc:postgresql://${azurerm_postgresql_flexible_server.main.fqdn}:5432"

  # Spring Boot reads this as an extra property source, so rules change with
  # `terraform apply` instead of a mounted file or an image rebuild.
  thermal_rules_json = jsonencode({
    coldchain = {
      alerts = {
        rules = {
          for id, rule in var.thermal_rules : lower(id) => {
            "threshold-min" = rule.threshold_min
            "threshold-max" = rule.threshold_max
          }
        }
      }
    }
  })

  # Express environments reject plain-HTTP ingress and give internal apps the
  # same <app>.<default_domain> FQDN (reachable only from inside the
  # environment), so apps call each other over HTTPS. Telemetry is external because the simulator Function runs outside the
  # environment and its client only accepts an origin without path.
  internal_url = {
    for name in ["monitoring-registry", "alert-management"] :
    name => "https://${name}.${azurerm_container_app_environment.main.default_domain}"
  }

  java_apps = {
    "monitoring-registry" = {
      port     = 8080
      database = "registry"
      external = false
      # Registry has no liveness group and /actuator/health includes the DB,
      # so liveness uses TCP to avoid restarts during database maintenance.
      liveness_path = ""
      ready_path    = "/actuator/health"
      env           = {}
    }
    "telemetry-ingestion" = {
      port          = 8081
      database      = "telemetry"
      external      = true
      liveness_path = "/actuator/health/liveness"
      ready_path    = "/actuator/health/readiness"
      env = {
        REGISTRY_BASE_URL = local.internal_url["monitoring-registry"]
        ALERTS_BASE_URL   = local.internal_url["alert-management"]
        HTTP_READ_TIMEOUT = var.http_read_timeout
      }
    }
    "alert-management" = {
      port          = 8082
      database      = "alerts"
      external      = false
      liveness_path = "/actuator/health/liveness"
      ready_path    = "/actuator/health/readiness"
      env = {
        REGISTRY_BASE_URL       = local.internal_url["monitoring-registry"]
        HTTP_READ_TIMEOUT       = var.http_read_timeout
        SPRING_APPLICATION_JSON = local.thermal_rules_json
      }
    }
  }
}

resource "azurerm_container_app" "java" {
  for_each = local.java_apps

  name                         = each.key
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"
  workload_profile_name        = "Consumption"
  tags                         = local.tags

  secret {
    name  = "db-password"
    value = random_password.service_db[each.value.database].result
  }

  dynamic "secret" {
    for_each = local.private_registry ? [1] : []
    content {
      name  = "registry-password"
      value = var.registry_password
    }
  }

  dynamic "registry" {
    for_each = local.private_registry ? [1] : []
    content {
      server               = split("/", var.image_registry)[0]
      username             = var.registry_username
      password_secret_name = "registry-password"
    }
  }

  ingress {
    external_enabled           = each.value.external
    allow_insecure_connections = false
    target_port                = each.value.port
    transport                  = "http"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = var.min_replicas
    max_replicas = 1

    container {
      name   = each.key
      image  = "${var.image_registry}/${each.key}:${var.image_tag}"
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "PORT"
        value = tostring(each.value.port)
      }
      env {
        name  = "DB_URL"
        value = "${local.jdbc_base_url}/${local.databases[each.value.database].name}?sslmode=require"
      }
      env {
        name  = "DB_USER"
        value = local.databases[each.value.database].role
      }
      env {
        name        = "DB_PASSWORD"
        secret_name = "db-password"
      }
      # B1MS allows ~50 connections; 3 services x 5, doubled during a rollout,
      # stays below that.
      env {
        name  = "SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE"
        value = "5"
      }

      dynamic "env" {
        for_each = each.value.env
        content {
          name  = env.key
          value = env.value
        }
      }

      # Spring Boot needs ~30-60 s on 0.5 vCPU, plus Flyway on first start.
      startup_probe {
        transport               = each.value.liveness_path == "" ? "TCP" : "HTTP"
        port                    = each.value.port
        path                    = each.value.liveness_path == "" ? null : each.value.liveness_path
        interval_seconds        = 10
        timeout                 = 5
        failure_count_threshold = 18
      }

      liveness_probe {
        transport               = each.value.liveness_path == "" ? "TCP" : "HTTP"
        port                    = each.value.port
        path                    = each.value.liveness_path == "" ? null : each.value.liveness_path
        interval_seconds        = 30
        timeout                 = 5
        failure_count_threshold = 3
      }

      readiness_probe {
        transport               = "HTTP"
        port                    = each.value.port
        path                    = each.value.ready_path
        interval_seconds        = 10
        timeout                 = 5
        failure_count_threshold = 3
      }
    }
  }

  depends_on = [terraform_data.db_roles]

  # Express environments manage scaling and traffic themselves and rewrite
  # these fields; ignoring them keeps plans free of perpetual drift. Probe
  # transports are NOT ignored: Express omits the HTTP scheme in its response
  # and ignoring it would resend an empty transport, silently turning HTTP
  # probes into TCP. Plans therefore always show `transport = "HTTP"`.
  lifecycle {
    ignore_changes = [
      ingress[0].traffic_weight,
      template[0].cooldown_period_in_seconds,
      template[0].polling_interval_in_seconds,
      template[0].http_scale_rule,
    ]
  }
}

resource "azurerm_container_app" "frontend" {
  name                         = "frontend"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"
  workload_profile_name        = "Consumption"
  tags                         = local.tags

  dynamic "secret" {
    for_each = local.private_registry ? [1] : []
    content {
      name  = "registry-password"
      value = var.registry_password
    }
  }

  dynamic "registry" {
    for_each = local.private_registry ? [1] : []
    content {
      server               = split("/", var.image_registry)[0]
      username             = var.registry_username
      password_secret_name = "registry-password"
    }
  }

  ingress {
    external_enabled           = true
    allow_insecure_connections = false
    target_port                = 80
    transport                  = "http"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = var.min_replicas
    max_replicas = 1

    container {
      name   = "frontend"
      image  = "${var.image_registry}/frontend:${var.image_tag}"
      cpu    = 0.25
      memory = "0.5Gi"

      env {
        name  = "REGISTRY_UPSTREAM"
        value = local.internal_url["monitoring-registry"]
      }
      env {
        name  = "TELEMETRY_UPSTREAM"
        value = "https://${azurerm_container_app.java["telemetry-ingestion"].ingress[0].fqdn}"
      }
      env {
        name  = "ALERTS_UPSTREAM"
        value = local.internal_url["alert-management"]
      }

      liveness_probe {
        transport = "HTTP"
        port      = 80
        path      = "/"
      }
    }
  }

  # nginx resolves its upstreams at startup, so the APIs must exist first.
  depends_on = [azurerm_container_app.java]

  # Express environments manage scaling and traffic themselves and rewrite
  # these fields; ignoring them keeps plans free of perpetual drift. Probe
  # transports are NOT ignored: Express omits the HTTP scheme in its response
  # and ignoring it would resend an empty transport, silently turning HTTP
  # probes into TCP. Plans therefore always show `transport = "HTTP"`.
  lifecycle {
    ignore_changes = [
      ingress[0].traffic_weight,
      template[0].cooldown_period_in_seconds,
      template[0].polling_interval_in_seconds,
      template[0].http_scale_rule,
    ]
  }
}
