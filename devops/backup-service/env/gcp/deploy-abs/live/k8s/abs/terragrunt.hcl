terraform {
  source = "../../../modules/k8s/abs"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }

  before_hook "clone_abs_helm_chart" {
    commands = [get_terraform_command()]
    execute = [
      "sh", "-c",
      <<-EOT
      set -e
      if [ "$HELM_CHART_SOURCE" = "repo" ]; then
        rm -rf "./$SERVICE_NAME"
        git clone "https://$GITHUB_TOKEN@github.com/aerospike/aerospike-backup-service.git"
        cd "$SERVICE_NAME"
        git sparse-checkout init --cone --sparse-index
        git checkout "$ABS_BRANCH"
        git sparse-checkout set --skip-checks "helm/aerospike-backup-service"
      fi
    EOT
    ]
  }

  before_hook "gke" {
    commands = [get_terraform_command()]
    execute = [
      "sh", "-c",
      "gcloud container clusters get-credentials ${local.common_vars.prefix}-gke --zone ${local.common_vars.gcp.region}-a --project ${local.common_vars.gcp.project_id}"
    ]
  }
}

include "root" {
  path = find_in_parent_folders("root.hcl")
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
}

inputs = {
  prefix           = "${local.common_vars.prefix}"
  namespace        = "${local.common_vars.workspace}"
  image_tag        = "${local.common_vars.k8s.abs.image-tag}"
  storage_provider = "${local.common_vars.k8s.abs.storage-provider}"
  device_type      = "${local.common_vars.k8s.abs.device-type}"
  helm_chart_source = get_env("HELM_CHART_SOURCE", "jfrog")
}

generate "kubernetes" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"


  contents = <<-EOF
    data "google_client_config" "provider" {}

    data "google_container_cluster" "cluster" {
      name     = "${local.common_vars.prefix}-gke"
      location = "${local.common_vars.gcp.region}-a"
    }

    provider "kubernetes" {
      host  = "https://$${data.google_container_cluster.cluster.endpoint}"
      token = data.google_client_config.provider.access_token
      cluster_ca_certificate = base64decode(
      data.google_container_cluster.cluster.master_auth[0].cluster_ca_certificate,
      )
    }

    provider "helm" {
      kubernetes {
        host  = "https://$${data.google_container_cluster.cluster.endpoint}"
        token = data.google_client_config.provider.access_token
        cluster_ca_certificate = base64decode(
        data.google_container_cluster.cluster.master_auth[0].cluster_ca_certificate,
        )
      }
    }
    EOF
}
