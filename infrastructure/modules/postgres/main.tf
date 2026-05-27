resource "azurerm_postgresql_flexible_server" "main" {
  name                   = var.server_name
  resource_group_name    = var.resource_group_name
  location               = var.location
  version                = "16"
  administrator_login    = var.admin_username
  administrator_password = var.admin_password
  sku_name               = var.sku_name
  storage_mb             = 32768
  backup_retention_days  = 7
  tags                   = var.tags
}

resource "azurerm_postgresql_flexible_server_database" "main" {
  name      = var.db_name
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "server_name"         { type = string }
variable "admin_username"      { type = string; sensitive = true }
variable "admin_password"      { type = string; sensitive = true }
variable "db_name"             { type = string }
variable "sku_name"            { type = string }
variable "tags"                { type = map(string) }

output "server_fqdn" { value = azurerm_postgresql_flexible_server.main.fqdn; sensitive = true }
