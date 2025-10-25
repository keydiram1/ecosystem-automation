resource "google_compute_firewall" "asdb" {
  name    = "${local.prefix}-${local.module_name}-allow-asdb"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["4333"]
  }

  source_ranges = ["0.0.0.0/0"]

  destination_ranges = [data.google_compute_subnetwork.asdb.ip_cidr_range]

  target_tags = ["asdb"]
}

resource "google_compute_firewall" "minio" {
  name    = "${local.prefix}-${local.module_name}-allow-minio"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["9000"]
  }

  source_ranges = [
    data.google_compute_subnetwork.gke.ip_cidr_range,
    data.google_compute_subnetwork.gke.secondary_ip_range.0.ip_cidr_range,
    data.google_compute_subnetwork.gke.secondary_ip_range.1.ip_cidr_range
  ]

  destination_ranges = [data.google_compute_subnetwork.minio.ip_cidr_range]

  target_tags = ["minio"]
}
