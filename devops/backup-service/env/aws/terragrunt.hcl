remote_state {
  backend = "s3"
  config  = {
    bucket         = "adr-tf-remote-lock-bucket"
    key            = "${path_relative_to_include()}/terraform.tfstate"
    region         = "${local.common_vars.region}"
    encrypt        = true
    dynamodb_table = "adr-tf-remote-lock-dynamo-table"
  }
}

locals {
  common_vars = yamldecode(file("common_vars.yaml"))
}

generate "provider" {
  path      = "provider.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<-EOF
        provider "aws" {
        region = "eu-central-1"
        default_tags {
            tags = {
                Workspace = "${local.common_vars.workspace}"
                }
            }
        }
    EOF
}
