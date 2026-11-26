variable "subscription_id" {
  description = "Suscripción de Azure (Azure for Students)."
  type        = string
}

variable "location" {
  description = "Región; la misma del despliegue en Container Apps."
  type        = string
  default     = "brazilsouth"
}

variable "resource_group_name" {
  type    = string
  default = "rg-coldchain-aks"
}

variable "kubernetes_version" {
  description = "Versión de Kubernetes soportada por AKS en la región."
  type        = string
  default     = "1.31"
}

variable "node_vm_size" {
  description = "B2ms (2 vCPU, 8 GiB) alcanza para prod, Argo CD y observabilidad. Detener el clúster fuera de uso: az aks stop."
  type        = string
  default     = "Standard_B2ms"
}

variable "node_count" {
  type    = number
  default = 1
}

variable "max_node_count" {
  description = "Tope del cluster autoscaler: el HPA puede pedir más pods de los que caben en un nodo."
  type        = number
  default     = 2
}

variable "admin_group_object_ids" {
  description = "Grupos de Microsoft Entra ID con rol cluster-admin (operadores de plataforma)."
  type        = list(string)
  default     = []
}

variable "operator_ip_ranges" {
  description = "Rangos CIDR autorizados para el API server. Vacío = sin restricción (no recomendado)."
  type        = list(string)
  default     = []
}

variable "budget_amount" {
  description = "Presupuesto mensual en USD para el grupo de recursos de AKS."
  type        = number
  default     = 30
}

variable "alert_email" {
  description = "Correo que recibe los avisos del presupuesto."
  type        = string
}

variable "budget_start_date" {
  description = "Primer día del mes en formato RFC 3339 (por ejemplo 2026-12-01T00:00:00Z)."
  type        = string
}
