resource "null_resource" "install_aerospike" {
  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    working_dir = "${path.root}/ansible"
    command     = <<-EOT
    ansible-playbook \
    install-aerospike.yaml \
    --extra-vars "cluster_name=${local.cluster_name} aws_region=${data.aws_region.current.name} aerospike_version=${var.nodes.version} secret_agent=${local.gateway_dns_name}"
    EOT
  }

  depends_on = [
    local_file.inventory,
    local_sensitive_file.aerospike_cluster_ssh_key,
  ]
}

resource "null_resource" "create_aerospike_user" {
  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = <<-EOT
    asadm \
    -h ${aws_instance.aerospike_cluster.0.public_ip}:server.abs:4333 \
    -U admin \
    -P admin \
    --tls-enable \
    --tls-cafile=rootCA.pem \
    -e "enable; manage acl create user tester password psw roles truncate sindex-admin user-admin data-admin read-write read write read-write-udf sys-admin udf-admin"
    EOT
  }

  depends_on = [
    null_resource.install_aerospike
  ]
}

resource "local_file" "inventory" {
  content = templatefile("${path.module}/templates/hosts.tftpl", {
    nodes = data.aws_instances.cluster_instances.public_ips,
    prefix = var.prefix,
    ssh_private_key = local.ssh_private_key_filepath})
  filename = "${path.module}/ansible/inventory/hosts"

  depends_on = [data.aws_instances.cluster_instances]
}
