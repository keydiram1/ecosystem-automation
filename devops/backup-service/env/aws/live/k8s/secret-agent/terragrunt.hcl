terraform {
  source = "../../../modules/k8s/secret-agent"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
}

skip = "${local.common_vars.k8s.aerospike.secret_manager.enabled}" == false ? true : false

include "root" {
  path   = find_in_parent_folders()
  expose = true
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
  eks_name    = "${local.prefix}-eks"
}

generate "kubernetes" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"

  contents = <<-EOF
        data "aws_eks_cluster" "eks" {
            name = "${local.eks_name}"
        }

        data "aws_eks_cluster_auth" "eks" {
            name = "${local.eks_name}"
        }

        provider "kubernetes" {
            host                   = data.aws_eks_cluster.eks.endpoint
            cluster_ca_certificate = base64decode(data.aws_eks_cluster.eks.certificate_authority.0.data)
            exec {
                api_version = "client.authentication.k8s.io/v1beta1"
                args        = ["eks", "get-token", "--cluster-name", "${local.eks_name}", "--output", "json"]
                command     = "aws"
            }
            experiments {
                manifest_resource = true
            }
        }
    EOF
}

inputs = {
  prefix    = "${local.prefix}"
  namespace = "${local.common_vars.k8s.aerospike.k8s_namespace}"
  image_tag = "${local.common_vars.k8s.aerospike.secret_manager.version}"

  openid = {
    arn = dependency.eks.outputs.openid_provider_arn
    url = dependency.eks.outputs.openid_provider_url
  }
}

dependency "eks" {
  config_path  = "../../eks"
  mock_outputs = {
    eks_name            = "eks-name"
    openid_provider_arn = "123:arn"
    openid_provider_url = "url"
    ca_certificate      = "MTIz"
    endpoint            = "123"
  }
}

dependency "s3" {
  config_path  = "../../s3"
  mock_outputs = {
    bucket = "bucket-xxxx"
  }
}

dependency "istio" {
  config_path  = "../istio"
  skip_outputs = true
}

dependency "aerospike" {
  config_path  = "../aerospike"
  skip_outputs = true
}

dependency "autoscaler" {
  config_path  = "../autoscaler"
  skip_outputs = true
}

dependency "metric-server" {
  config_path  = "../metric-server"
  skip_outputs = true
}

dependency "ebs-csi-driver" {
  config_path  = "../ebs-csi-driver"
  skip_outputs = true
}
