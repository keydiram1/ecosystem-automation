output "master_ipv4_cidr_block" {
  value = local.secondary_ips["master-ip-range"]
}
