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

  # Express environments reject plain-HTTP ingress and publish every app as
  # <app>.<default_domain> on a shared public edge. As of 2026-11-27 that
  # edge answers 404 for internal-only apps even when the caller runs inside
  # the environment, so every API uses external ingress and apps call each
  # other over HTTPS. The frontend proxy already exposed the same paths; the
  # APIs require an Identity session on /api/**.
  app_url = {
    for name in ["monitoring-registry", "alert-management", "identity-access"] :
    name => "https://${name}.${azurerm_container_app_environment.main.default_domain}"
  }

  # Every API validates the Bearer session by calling Identity's introspection
  # endpoint. The client defaults to a 5 s read timeout, shorter than an
  # Identity cold start from zero replicas, so it reuses the service timeout.
  auth_env = {
    IDENTITY_BASE_URL          = local.app_url["identity-access"]
    COLDCHAIN_AUTH_READTIMEOUT = var.http_read_timeout
  }

  # Secret values stay out of java_apps: for_each rejects sensitive values.
  app_secret_values = {
    "bootstrap-admin-password" = random_password.bootstrap_admin.result
  }

  java_apps = {
    "identity-access" = {
      port          = 8083
      database      = "identity"
      external      = true
      liveness_path = "/actuator/health/liveness"
      ready_path    = "/actuator/health/readiness"
      env = {
        BOOTSTRAP_ADMIN_USERNAME = var.bootstrap_admin_username
      }
      # Container Apps secret name => environment variable.
      secret_env = {
        "bootstrap-admin-password" = "BOOTSTRAP_ADMIN_PASSWORD"
      }
    }
    "monitoring-registry" = {
      port     = 8080
      database = "registry"
      external = true
      # Registry has no liveness group and /actuator/health includes the DB,
      # so liveness uses TCP to avoid restarts during database maintenance.
      liveness_path = ""
      ready_path    = "/actuator/health"
      env           = local.auth_env
      secret_env    = {}
    }
    "telemetry-ingestion" = {
      port          = 8081
      database      = "telemetry"
      external      = true
      liveness_path = "/actuator/health/liveness"
      ready_path    = "/actuator/health/readiness"
      env = merge(local.auth_env, {
        REGISTRY_BASE_URL = local.app_url["monitoring-registry"]
        ALERTS_BASE_URL   = local.app_url["alert-management"]
        HTTP_READ_TIMEOUT = var.http_read_timeout
      })
      secret_env = {}
    }
    "alert-management" = {
      port          = 8082
      database      = "alerts"
      external      = true
      liveness_path = "/actuator/health/liveness"
      ready_path    = "/actuator/health/readiness"
      env = merge(local.auth_env, {
        REGISTRY_BASE_URL       = local.app_url["monitoring-registry"]
        HTTP_READ_TIMEOUT       = var.http_read_timeout
        SPRING_APPLICATION_JSON = local.thermal_rules_json
      })
      secret_env = {}
    }
  }
}

# Identity creates this ADMIN on its first start (only when the user does not
# exist yet). Read it with `terraform output -raw bootstrap_admin_password`.
resource "random_password" "bootstrap_admin" {
  length  = 24
  special = false
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
    for_each = each.value.secret_env
    content {
      name  = secret.key
      value = local.app_secret_values[secret.key]
    }
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
      # B1MS allows ~50 connections; 4 services x 4, doubled during a rollout,
      # stays below that.
      env {
        name  = "SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE"
        value = "4"
      }

      dynamic "env" {
        for_each = each.value.env
        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = each.value.secret_env
        content {
          name        = env.value
          secret_name = env.key
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
    target_port                = 8080
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
        value = local.app_url["monitoring-registry"]
      }
      env {
        name  = "TELEMETRY_UPSTREAM"
        value = "https://${azurerm_container_app.java["telemetry-ingestion"].ingress[0].fqdn}"
      }
      env {
        name  = "ALERTS_UPSTREAM"
        value = local.app_url["alert-management"]
      }
      env {
        name  = "IDENTITY_UPSTREAM"
        value = local.app_url["identity-access"]
      }

      # The unprivileged nginx image listens on 8080, like the ingress target.
      liveness_probe {
        transport = "HTTP"
        port      = 8080
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
