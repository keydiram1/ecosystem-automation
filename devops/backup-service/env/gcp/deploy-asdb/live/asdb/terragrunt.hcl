terraform {
  source = "../../modules/asdb"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

include "root" {
  path = find_in_parent_folders("root.hcl")
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
}

inputs = {
  prefix = "${local.common_vars.prefix}"

  nodes = {
    machine_type = "${local.common_vars.asdb.machine_type}"
    version      = "${local.common_vars.asdb.version}"
    size         = "${local.common_vars.asdb.size}"
    distro       = "${local.common_vars.asdb.distro}"
    arch         = "${local.common_vars.asdb.arch}"
    multi_zone   = "${local.common_vars.asdb.multi_zone}"
  }

  asdb = {
    namespaces           = "${local.common_vars.asdb.namespaces}"
    device_type          = "${local.common_vars.asdb.device_type}"
    security_type        = "${local.common_vars.asdb.security_type}"
    section_selection    = "${local.common_vars.asdb.section_selection}"
    secret_agent         = "${local.common_vars.asdb.secret_agent}"
    devices              = "${local.common_vars.asdb.devices}"
    strong_consistency   = "${local.common_vars.asdb.strong_consistency}"
    roster               = "${local.common_vars.asdb.roster}"
    ops_agent            = "${local.common_vars.ops_agent}"
    device_shadow        = "${local.common_vars.asdb.device_shadow}"
    load_balancer        = "${local.common_vars.asdb.load_balancer}"
    encryption_at_rest   = "${local.common_vars.asdb.encryption_at_rest}"
    single_query_threads = "${local.common_vars.asdb.single_query_threads}"
  }
}

