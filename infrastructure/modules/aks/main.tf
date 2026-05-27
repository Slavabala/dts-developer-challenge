resource "azurerm_kubernetes_cluster" "main" {
  name                = var.cluster_name
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = var.cluster_name

  default_node_pool {
    name       = "default"
    node_count = var.node_count
    vm_size    = var.vm_size
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "azure"
    network_policy = "calico"
  }

  tags = var.tags
}

variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "cluster_name"        { type = string }
variable "node_count"          { type = number }
variable "vm_size"             { type = string }
variable "tags"                { type = map(string) }

output "cluster_name"     { value = azurerm_kubernetes_cluster.main.name }
output "kube_config_raw"  {
  value     = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive = true
}
