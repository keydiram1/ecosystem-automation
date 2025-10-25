terraform {
  source = "../../modules/minio"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

include "root" {
  path = find_in_parent_folders("root.hcl")
}

skip = "${local.common_vars.storage.provider}" != "minio" ? true : false
# exclude {
#   if                   = "${local.common_vars.storage.provider}" != "minio"
#   actions              = ["all"]
#   exclude_dependencies = true
# }

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
}

inputs = {
  prefix       = "${local.common_vars.prefix}"
  machine_type = "${local.common_vars.minio.machine_type}"
}
