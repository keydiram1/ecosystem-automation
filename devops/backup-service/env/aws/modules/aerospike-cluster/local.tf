locals {
  ssh_private_key_filepath = "${abspath(path.module)}/${var.cluster_name}.aws.pem"
  arch = data.aws_ec2_instance_type.this.supported_architectures[0]
  cluster_name = "${var.prefix}-${var.cluster_name}"
  dns_name = "${replace(var.prefix, "-", ".")}.internal"
  gateway_dns_name = "gateway.${local.dns_name}"
}
