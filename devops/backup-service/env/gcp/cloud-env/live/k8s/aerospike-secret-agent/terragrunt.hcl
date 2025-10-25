terraform {
  source = "../../../modules/k8s/aerospike-secret-agent"
}

include "root" {
  path = find_in_parent_folders("root.hcl")
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
}

inputs = {
  namespace = "${local.common_vars.k8s.aerospike-secret-agent.namespace}"
}

generate "kubernetes" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"


  contents = <<-EOF
    data "google_client_config" "provider" {}

    data "google_container_cluster" "cluster" {
      name     = "${local.common_vars.gke.cluster_name}"
      location = "${local.common_vars.gcp.region}-a"
    }

    provider "kubernetes" {
      host  = "https://$${data.google_container_cluster.cluster.endpoint}"
      token = data.google_client_config.provider.access_token
      cluster_ca_certificate = base64decode(
      data.google_container_cluster.cluster.master_auth[0].cluster_ca_certificate,
      )
    }
    EOF
}

dependency "gateway" {
  config_path  = "../gateway"
  skip_outputs = true
}
