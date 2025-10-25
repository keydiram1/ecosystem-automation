remote_state {
  backend = "gcs"

  generate = {
    path      = "backend.tf"
    if_exists = "overwrite"
  }

  config = {
    project              = "${local.common_vars.gcp.project_id}"
    location             = "${local.common_vars.gcp.region}"
    skip_bucket_creation = false
    bucket               = "aerospike-backup-service-terraform-state"
    prefix               = "${path_relative_to_include()}"
  }
}

locals {
  common_vars = yamldecode(file("common_vars.yaml"))
}

generate "provider" {
  path      = "provider.tf"
  if_exists = "overwrite_terragrunt"

  contents = <<-EOF
    provider "google" {
      project     = "${local.common_vars.gcp.project_id}"
      region      = "${local.common_vars.gcp.region}"
      zone = "${local.common_vars.gcp.region}-a"
    }

    EOF
}
