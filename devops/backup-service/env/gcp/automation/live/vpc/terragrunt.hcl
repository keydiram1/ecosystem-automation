terraform {
  source = "../../modules/vpc"

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
}
