terraform {
  source = "../../modules/eks"
  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }

  before_hook "kubectl_config" {
    commands = ["destroy", "plan"]
    execute  = [
      "sh", "-c",
      "aws eks --region ${local.common_vars.region} update-kubeconfig --name ${local.eks_name}"
    ]
  }

  after_hook "kubectl_config" {
    commands = ["apply"]
    execute  = [
      "sh", "-c",
      "aws eks --region ${local.common_vars.region} update-kubeconfig --name ${local.eks_name}"
    ]
  }

  extra_arguments "conditional_vars" {
    commands           = get_terraform_commands_that_need_input()
    optional_var_files = [
      "${get_parent_terragrunt_dir()}/${get_env("EKS", "eks-amd64")}.tfvars",
    ]
  }
}

skip = "${local.common_vars.eks.enabled}" == false ? true : false

include "root" {
  path = find_in_parent_folders()
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
  eks_name    = "${local.prefix}-eks"
}

inputs = {
  prefix              = "${local.eks_name}"
  eks_name            = "${local.eks_name}"
  eks_version         = "${local.common_vars.eks.version}"
  enable_irsa         = "${local.common_vars.eks.enable_irsa}"
  eks_private_subnets = dependency.vpc.outputs.private_eks_subnet_ids
  eks_public_subnets  = dependency.vpc.outputs.public_eks_subnet_ids
  node_groups         = "${local.common_vars.eks.groups}"
}

dependency "vpc" {
  config_path  = "../vpc"
  mock_outputs = {
    private_eks_subnet_ids = ["subnet-1234", "subnet-5678"]
    public_eks_subnet_ids  = ["subnet-9123", "subnet-4567"]
  }
}
