resource "tls_private_key" "aerospike_private_key" {
  algorithm = "RSA"
}

resource "aws_key_pair" "aerospike_cluster_keypair" {
  key_name   = "${local.cluster_name}-keypair"
  public_key = tls_private_key.aerospike_private_key.public_key_openssh

  tags = {
    Name = "${local.cluster_name}-keypair"
  }
}

resource "aws_instance" "aerospike_cluster" {
  count                = var.nodes.size
  instance_type        = var.nodes.instance_type
  ami                  =  data.aws_ami.this.id
  subnet_id            = var.subnet_id
  security_groups      = [var.sg_id]
  key_name             = aws_key_pair.aerospike_cluster_keypair.key_name

  lifecycle {
    ignore_changes = all
  }

  tags = {
    ClusterName = local.cluster_name
    Name = "${local.cluster_name}-${count.index}"
  }
}

resource "local_sensitive_file" "aerospike_cluster_ssh_key" {
  filename        = local.ssh_private_key_filepath
  file_permission = "400"
  content         = tls_private_key.aerospike_private_key.private_key_pem
}
