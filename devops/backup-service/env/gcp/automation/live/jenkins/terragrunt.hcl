terraform {
  source = "../../modules/jenkins"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

include "root" {
  path = find_in_parent_folders("root.hcl")
}

# TODO Switch to skip or refactor
exclude {
  if                   = "${local.common_vars.jenkins.enabled}" == false
  actions = ["all"]
  exclude_dependencies = false
}

# skip = "${local.common_vars.jenkins.enabled}" == true ? false : true

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  members = run_cmd(
    "bash",
    "${get_repo_root()}/devops/backup-service/env/gcp/automation/scripts/get-members.sh",
    "${local.common_vars.gcp.project_id}")
}

inputs = {
  prefix       = "${local.common_vars.prefix}"
  machine_type = "${local.common_vars.jenkins.machine_type}"
  members      = "${local.members}"
}

dependency "vpc" {
  skip_outputs = true
  config_path  = "../vpc"
}
