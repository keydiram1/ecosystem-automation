terraform {
  source = "../../modules/aerospike-cluster"

  before_hook "workspace" {
    commands = [get_terraform_command()]
    execute  = ["sh", "-c", "terraform workspace select -or-create ${local.common_vars.workspace}"]
  }
  before_hook "root_ca_pem" {
    commands = ["apply"]
    execute  = [
      "sh", "-c",
      "aws secretsmanager get-secret-value --secret-id testenv --query SecretString --output text | jq -r .rootCA_pem | base64 --decode > rootCA.pem && chmod 0644 rootCA.pem"
    ]
  }
}

skip = "${local.common_vars.aerospike.enabled}" == false ? true : false

include "root" {
  path = find_in_parent_folders()
}

locals {
  common_vars = yamldecode(file(find_in_parent_folders("common_vars.yaml")))
  prefix      = "abs-${local.common_vars.workspace}"
}

inputs = {
  nodes = {
    instance_type = "${local.common_vars.aerospike.instance_type}"
    size          = "${local.common_vars.aerospike.size}"
    version       = "${local.common_vars.aerospike.version}"
  }
  cluster_name = "${local.common_vars.aerospike.name}"
  subnet_id    = dependency.vpc.outputs.public_aerospike_subnet_id
  vpc_id       = dependency.vpc.outputs.vpc_id
  sg_id        = dependency.vpc.outputs.sg_id
  prefix       = "${local.prefix}"
}

dependency "vpc" {
  config_path  = "../vpc"
  mock_outputs = {
    vpc_id                     = "vpc-xxxx"
    public_aerospike_subnet_id = "subnet-xxxx"
    sg_id                      = "sg-xxxx"
  }
}

dependency "aerospike" {
  config_path  = "../k8s/aerospike"
  skip_outputs = true
}

dependency "secret-agent" {
  config_path  = "../k8s/secret-agent"
  skip_outputs = true
}
