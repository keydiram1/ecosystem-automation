terraform {
  source = "../../modules/gke"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }

  after_hook "after_hook" {
    commands = ["apply"]
    execute = [
      "sh", "-c",
      "gcloud container clusters get-credentials ${local.common_vars.workspace}-gke --zone ${local.common_vars.gcp.region}-a --project ${local.common_vars.gcp.project_id}"
    ]
    run_on_error = false
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
  # master_ipv4_cidr_block = dependency.vpc.outputs.master_ipv4_cidr_block
  node_pools = local.common_vars.gke.node_pools
}
