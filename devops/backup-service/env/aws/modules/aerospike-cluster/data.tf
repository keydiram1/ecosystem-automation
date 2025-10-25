data "aws_region" "current" {}

data "aws_ec2_instance_type" "this" {
  instance_type = var.nodes.instance_type
}

data "aws_ami" "this" {
  owners = ["amazon"]
  most_recent = true

  filter {
    name   = "name"
    values = ["al2023-ami-2*-kernel-*-${local.arch}"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
  filter {
    name   = "architecture"
    values = [local.arch]
  }
  filter {
    name   = "block-device-mapping.volume-type"
    values = ["gp3"]
  }
}

data "aws_vpc" "this" {
  id = var.vpc_id
}

data aws_route53_zone "zone" {
  name = local.dns_name
  private_zone = true
}

data "aws_instances" "cluster_instances" {
  depends_on = [
    aws_instance.aerospike_cluster
  ]

  filter {
    name   = "tag:Name"
    values = ["${local.cluster_name}-*"]
  }
  filter {
    name   = "instance-state-name"
    values = ["running"]
  }
}

data "aws_secretsmanager_secret_version" "tls" {
  secret_id = data.aws_secretsmanager_secret.tls.id
}

data "aws_secretsmanager_secret" "tls" {
  name = "testenv"
}
