terraform {
  source = "../../modules/gke"

  after_hook "after_hook" {
    commands = ["apply"]
    execute = [
      "sh",
      "-c",
      <<-EOT
      gcloud container clusters get-credentials \
        ${local.common_vars.gke.cluster_name} \
        --zone ${local.common_vars.gcp.region}-a \
        --project ${local.common_vars.gcp.project_id}
    EOT
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
  prefix     = local.common_vars.prefix
  cluster_name = local.common_vars.gke.cluster_name
  # master_ipv4_cidr_block = dependency.vpc.outputs.master_ipv4_cidr_block
  node_pools = local.common_vars.gke.node_pools

}
