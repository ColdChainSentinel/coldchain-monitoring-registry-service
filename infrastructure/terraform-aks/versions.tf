terraform {
  required_version = ">= 1.9"

  # Mismo backend remoto que infrastructure/terraform, con otra clave:
  #   terraform init -backend-config=backend.hcl   (key = "coldchain-aks.tfstate")
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.14"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
  }
  subscription_id                 = var.subscription_id
  resource_provider_registrations = "none"
}
