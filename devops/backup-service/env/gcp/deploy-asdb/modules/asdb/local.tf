locals {
  module_name = "asdb"

  rr_zones = var.nodes.multi_zone ? sort(data.google_compute_zones.available[0].names) : [data.google_client_config.current.zone]

  image_families = {
    "ubuntu20.04-amd64" = "ubuntu-2004-lts"
    "ubuntu20.04-arm64" = "ubuntu-2004-lts-arm64"
    "ubuntu22.04-amd64" = "ubuntu-2204-lts"
    "ubuntu22.04-arm64" = "ubuntu-2204-lts-arm64"
    "ubuntu24.04-amd64" = "ubuntu-2404-lts-amd64"
    "ubuntu24.04-arm64" = "ubuntu-2404-lts-arm64"
    "el8-amd64"         = "rocky-linux-8"
    "el8-arm64"         = "rocky-linux-8-optimized-gcp-arm64"
    "el9-amd64"         = "rocky-linux-9"
    "el9-arm64"         = "rocky-linux-9-arm64"
    "debian11-amd64"    = "debian-11"
    "debian11-arm64"    = null
    "debian12-amd64"    = "debian-12"
    "debian12-arm64"    = "debian-12-arm64"
  }
  devices      = var.asdb.devices < 2 && var.nodes.machine_type == "n2-standard-16" ? 2 : var.asdb.devices
  image_family = local.image_families["${replace(var.nodes.distro, " ", "")}-${var.nodes.arch}"]
  arch = {
    "amd64" = "x86_64"
    "arm64" = "aarch64"
  }
  secret_agent_address = "gateway.${var.prefix}.internal"
  section_selection    = length(var.asdb.section_selection) == 0 ? [false, false, false] : var.asdb.section_selection
  service_port         = local.section_selection[0] == false ? "3000" : "4333"
  heartbeat_port       = local.section_selection[1] == false ? "3002" : "3012"
  fabric_port          = local.section_selection[2] == false ? "3001" : "3011"
  asdb_service_ports   = [local.service_port, local.heartbeat_port, local.fabric_port, "3003"]

  ansible_vars = jsonencode({
    project_id = data.google_client_config.current.project
    workspace  = terraform.workspace

    namespaces    = var.asdb.namespaces
    device_type   = var.asdb.device_type
    security_type = var.asdb.security_type

    section_selection  = local.section_selection
    secret_agent       = var.asdb.secret_agent
    strong_consistency = var.asdb.strong_consistency
    roster             = var.asdb.roster
    ops_agent          = var.asdb.ops_agent

    asdb_version         = var.nodes.version
    distro               = replace(var.nodes.distro, " ", "")
    arch                 = local.arch[var.nodes.arch]
    secret_agent_address = local.secret_agent_address

    encryption_at_rest   = var.asdb.encryption_at_rest
    device_shadow        = var.asdb.device_shadow
    single_query_threads = var.asdb.single_query_threads
  })

  ip_address = var.asdb.load_balancer ? google_compute_forwarding_rule.internal_nlb[0].ip_address : google_compute_instance.asdb_node[0].network_interface[0].network_ip
}
