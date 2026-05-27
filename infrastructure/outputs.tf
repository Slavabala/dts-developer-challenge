output "aks_cluster_name" {
  description = "Name of the AKS cluster"
  value       = module.aks.cluster_name
}

output "postgres_fqdn" {
  description = "FQDN of the Postgres server"
  value       = module.postgres.server_fqdn
  sensitive   = true
}

output "redis_hostname" {
  description = "Hostname of the Redis cache"
  value       = module.redis.hostname
  sensitive   = true
}

output "resource_group_name" {
  description = "Name of the resource group"
  value       = azurerm_resource_group.main.name
}
