output "asdb_node_ips" {
  value = google_compute_instance.asdb_node.*.network_interface.0.network_ip
}
