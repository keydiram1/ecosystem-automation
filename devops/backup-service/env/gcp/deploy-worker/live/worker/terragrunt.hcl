terraform {
  source = "../../modules/worker"

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

  worker = {
    machine_type              = "${local.common_vars.worker.machine_type}"
    devices                   = "${local.common_vars.worker.devices}"
    ops_agent                 = "${local.common_vars.worker.ops_agent}"
    clone_backup_cli_repo   = "${local.common_vars.worker.clone_backup_cli_repo}"
    clone_backup_service_repo = "${local.common_vars.worker.clone_backup_service_repo}"
    clone_backup_library_repo = "${local.common_vars.worker.clone_backup_library_repo}"
  }
}
