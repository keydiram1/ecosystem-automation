variable "prefix" {
  type = string
}

# variable "master_ipv4_cidr_block" {
#   type = string
# }

variable "cluster_name" {
  type = string
}

variable "node_pools" {
  type = list(object({
    pool_name    = string
    role_label   = string
    machine_type = string
    autoscaling = object({
      min_size = number
      max_size = number
    })
  }))
}
