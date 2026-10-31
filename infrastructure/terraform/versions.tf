terraform {
  required_version = ">= 1.9"

  # Settings live in backend.hcl (see backend.hcl.example and README).
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.11.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }
}

provider "azurerm" {
  features {}

  subscription_id = var.subscription_id

  # Providers are registered once with `az provider register` (see README):
  # Azure for Students rejects some of the providers the default set registers.
  resource_provider_registrations = "none"
}
