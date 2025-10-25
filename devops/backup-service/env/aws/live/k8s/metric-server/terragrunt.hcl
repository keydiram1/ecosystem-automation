terraform {
  source = "../../../modules/k8s/metric-server"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

skip = "${local.common_vars.k8s.metric_server.enabled}" == false ? true : false

include "root" {
  path = find_in_parent_folders()
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
  eks_name    = "${local.prefix}-eks"
}

generate "helm_provider" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<-EOF
    data "aws_eks_cluster" "eks" {
      name = "${local.eks_name}"
    }

    data "aws_eks_cluster_auth" "eks" {
      name = "${local.eks_name}"
    }

    provider "helm" {
        kubernetes {
            host                   = data.aws_eks_cluster.eks.endpoint
            cluster_ca_certificate = base64decode(data.aws_eks_cluster.eks.certificate_authority.0.data)
            exec {
            api_version = "client.authentication.k8s.io/v1beta1"
            args        = ["eks", "get-token", "--cluster-name", "${local.eks_name}", "--output", "json"]
            command     = "aws"
        }
      }
    }
  EOF
}

inputs = {
  prefix        = "${local.prefix}"
  metric_server = {
    chart_version = "${local.common_vars.k8s.metric_server.chart_version}"
  }
}

dependency "eks" {
  config_path  = "../../eks"
  mock_outputs = {
    eks_name       = "eks-name"
    ca_certificate = "MTIz"
    endpoint       = "123"
  }
}
