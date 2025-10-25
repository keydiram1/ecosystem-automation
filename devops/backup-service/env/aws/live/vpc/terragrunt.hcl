terraform {
  source = "../../modules/vpc"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

include "root" {
  path = find_in_parent_folders()
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
}

inputs = {
  prefix                           = "${local.prefix}"
  eks_public_subnets               = ["10.0.2.0/24", "10.0.3.0/24"]
  eks_private_subnets              = ["10.0.4.0/24", "10.0.5.0/24"]
  aerospike_cluster_public_subnets = ["10.0.6.0/24"]

  eks_public_subnet_tags = {
    "kubernetes.io/role/elb"        = 1
    "kubernetes.io/cluster/abs-eks" = "owned"
  }
  eks_private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
    "kubernetes.io/cluster/$abs-eks"  = "owned"
  }
}
