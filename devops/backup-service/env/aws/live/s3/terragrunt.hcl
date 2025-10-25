terraform {
  source = "../../modules/s3"
  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

skip = "${local.common_vars.s3.enabled}" == false ? true : false

include "root" {
  path = find_in_parent_folders()
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
}

inputs = {
  prefix       = "${local.prefix}"
  bucket_name  = "${local.common_vars.s3.bucket_name}"
  buckets      = "${local.common_vars.s3.buckets}"
  access_point = "${local.common_vars.s3.access_point}"
}
