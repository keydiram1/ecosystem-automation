terraform {
  source = "../../../modules/k8s/ebs-csi-driver"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

skip = "${local.common_vars.k8s.csi_ebs_driver.enabled}" == false ? true : false

include "root" {
  path   = find_in_parent_folders()
  expose = true
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
}

inputs = {
  #  remote_state_config = include.remote_state.remote_state
  prefix = "${local.prefix}"

  eks = {
    name           = dependency.eks.outputs.eks_name
    version        = dependency.eks.outputs.eks_version
    endpoint       = dependency.eks.outputs.endpoint
    ca_certificate = dependency.eks.outputs.ca_certificate
    openid         = {
      arn = dependency.eks.outputs.openid_provider_arn
      url = dependency.eks.outputs.openid_provider_url
    }
  }
}

dependency "eks" {
  config_path  = "../../eks"
  mock_outputs = {
    eks_name            = "eks-name"
    eks_version         = "1.26"
    openid_provider_arn = "123:arn"
    openid_provider_url = "url"
    ca_certificate      = "MTIz"
    endpoint            = "123"
  }
  mock_outputs_merge_strategy_with_state = "shallow"
}
