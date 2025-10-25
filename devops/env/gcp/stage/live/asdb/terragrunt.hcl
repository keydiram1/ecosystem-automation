terraform {
  source = "../../modules/asdb"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
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
    distro = "${local.common_vars.asdb.distro}"
    arch = "${local.common_vars.asdb.arch}"
  }
}

dependency "gke" {
  skip_outputs = true
  config_path  = "../gke"
}

dependency "asa" {
  skip_outputs = true
  config_path  = "../k8s/asa"
}
