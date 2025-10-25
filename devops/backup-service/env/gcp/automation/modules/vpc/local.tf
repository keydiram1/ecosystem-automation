locals {
  primary_ip_suffix   = "10.0.0.0/16"
  secondary_ip_suffix = "172.16.0.0/16"
  primary_ip = zipmap(["jenkins", "proxy", "gke", "asdb", "minio"], cidrsubnets(local.primary_ip_suffix, 4, 4, 4, 4, 4))
  secondary_ips = zipmap(
    ["pod-ip-range", "service-ip-range", "master-ip-range"],
    cidrsubnets(local.secondary_ip_suffix, 4, 4, 12))
  subnet_secondary_ip = merge({for k, v in local.secondary_ips : k => v if k !=  "master-ip-range"})
}