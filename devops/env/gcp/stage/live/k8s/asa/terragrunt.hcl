terraform {
  source = "../../../modules/k8s/asa"

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
  namespace = "${local.common_vars.k8s.abs.namespace}"
  image_tag = "${local.common_vars.k8s.asa.version}"
}

generate "kubernetes" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"


  contents = <<-EOF
    data "google_client_config" "provider" {}

    data "google_container_cluster" "cluster" {
      name     = "${local.common_vars.workspace}-gke"
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
