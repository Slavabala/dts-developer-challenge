resource "azurerm_redis_cache" "main" {
  name                = var.cache_name
  location            = var.location
  resource_group_name = var.resource_group_name
  capacity            = var.capacity
  family              = var.family
  sku_name            = var.sku_name
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  tags                = var.tags
}

variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "cache_name"          { type = string }
variable "capacity"            { type = number }
variable "family"              { type = string }
variable "sku_name"            { type = string }
variable "tags"                { type = map(string) }

output "hostname"      { value = azurerm_redis_cache.main.hostname; sensitive = true }
output "primary_key"   { value = azurerm_redis_cache.main.primary_access_key; sensitive = true }
