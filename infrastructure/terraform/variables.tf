variable "subscription_id" {
  description = "Azure subscription that hosts the environment."
  type        = string
}

variable "location" {
  description = "Azure region. Azure for Students only allows brazilsouth, northcentralus, chilecentral, mexicocentral and westus."
  type        = string
  default     = "brazilsouth"
}

variable "resource_group_name" {
  description = "Resource group that holds every resource of the environment."
  type        = string
  default     = "rg-coldchain-dev"
}

variable "image_registry" {
  description = "Registry and namespace of the service images."
  type        = string
  default     = "ghcr.io/coldchainsentinel"
}

variable "image_tag" {
  description = "Image tag deployed for every service (use the Git commit SHA, never a moving tag)."
  type        = string
}

variable "registry_username" {
  description = "Username for a private registry. Leave empty when the images are public."
  type        = string
  default     = ""
}

variable "registry_password" {
  description = "Token for a private registry (read-only scope). Leave empty when the images are public."
  type        = string
  default     = ""
  sensitive   = true
}

variable "operator_ip_addresses" {
  description = "Public IPv4 addresses allowed to reach PostgreSQL directly (used to bootstrap the service roles with psql)."
  type        = list(string)

  validation {
    condition     = length(var.operator_ip_addresses) > 0
    error_message = "At least one operator IP is required to bootstrap the database roles."
  }
}

variable "alert_email" {
  description = "Email that receives budget notifications."
  type        = string
}

variable "budget_amount" {
  description = "Monthly budget in USD for the subscription."
  type        = number
  default     = 5
}

variable "budget_start_date" {
  description = "First day of the month when the budget starts (RFC 3339)."
  type        = string
  default     = "2026-10-01T00:00:00Z"
}

variable "min_replicas" {
  description = "Minimum replicas per container app. 0 stays within the Container Apps free grant; raise it to 1 only for demos."
  type        = number
  default     = 0
}

variable "http_read_timeout" {
  description = "Read timeout for service-to-service calls. Higher than the local default to absorb cold starts from zero replicas."
  type        = string
  default     = "45s"
}

variable "thermal_rules" {
  description = "Thermal rules for Alert Management keyed by monitoring point UUID, bounds inclusive in Celsius."
  type = map(object({
    threshold_min = number
    threshold_max = number
  }))
  default = {}

  validation {
    condition     = alltrue([for rule in values(var.thermal_rules) : rule.threshold_min <= rule.threshold_max])
    error_message = "Every thermal rule needs threshold_min <= threshold_max."
  }

  validation {
    condition     = alltrue([for id in keys(var.thermal_rules) : can(regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", id))])
    error_message = "Thermal rule keys must be monitoring point UUIDs."
  }
}

variable "simulator_enabled" {
  description = "Run the telemetry simulator timer. Keep it off outside demos: every run wakes the APIs and consumes the free grant."
  type        = bool
  default     = false
}

variable "simulator_point_id" {
  description = "Monitoring point UUID the simulator reports for (must exist in Registry)."
  type        = string
  default     = ""

  validation {
    condition     = var.simulator_point_id == "" || can(regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", var.simulator_point_id))
    error_message = "simulator_point_id must be empty or a UUID."
  }
}

variable "simulator_temperature" {
  description = "Temperature in Celsius sent by the simulator."
  type        = number
  default     = 5
}

variable "simulator_schedule" {
  description = "Six-field NCRONTAB schedule of the simulator timer (UTC)."
  type        = string
  default     = "0 */5 * * * *"
}
