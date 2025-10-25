locals {
  module_name    = "aerospike-secret-agent"
  latest_version = "latest"
  service_port   = 3005
  manage_port    = 8080
  filename       = "${local.module_name}.yaml"
  filepath       = "/etc/${local.module_name}"

}
