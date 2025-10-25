output "public_ips" {
  value = flatten(aws_instance.aerospike_cluster.*.public_ip)
}

output "private_ips" {
  value = flatten(aws_instance.aerospike_cluster.*.private_ip)
}

output "instance_ids" {
  value = flatten(aws_instance.aerospike_cluster.*.id)
}

output "cluster_ssh_key" {
  sensitive = true
  value = local_sensitive_file.aerospike_cluster_ssh_key.content
}
