terraform {
  required_version = ">= 1.5"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.100"
    }
  }

  backend "azurerm" {
    resource_group_name  = "hmcts-terraform-state"
    storage_account_name = "hmctstfstate"
    container_name       = "tfstate"
    key                  = "task-management.tfstate"
  }
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}

resource "azurerm_resource_group" "main" {
  name     = "rg-${var.project}-${var.environment}"
  location = var.location
  tags     = local.tags
}

module "aks" {
  source              = "./modules/aks"
  resource_group_name = azurerm_resource_group.main.name
  location            = var.location
  cluster_name        = "aks-${var.project}-${var.environment}"
  node_count          = var.aks_node_count
  vm_size             = var.aks_vm_size
  tags                = local.tags
}

module "postgres" {
  source              = "./modules/postgres"
  resource_group_name = azurerm_resource_group.main.name
  location            = var.location
  server_name         = "psql-${var.project}-${var.environment}"
  admin_username      = var.db_admin_username
  admin_password      = var.db_admin_password
  db_name             = "tasks"
  sku_name            = var.postgres_sku
  tags                = local.tags
}

module "redis" {
  source              = "./modules/redis"
  resource_group_name = azurerm_resource_group.main.name
  location            = var.location
  cache_name          = "redis-${var.project}-${var.environment}"
  capacity            = var.redis_capacity
  family              = var.redis_family
  sku_name            = var.redis_sku
  tags                = local.tags
}

locals {
  tags = {
    project     = var.project
    environment = var.environment
    managed_by  = "terraform"
    team        = "hmcts"
  }
}
