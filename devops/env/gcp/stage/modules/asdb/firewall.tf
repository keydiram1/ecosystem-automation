resource "google_compute_firewall" "asdb_internal" {
  name    = "${var.prefix}-${local.module_name}-allow-internal-asdb"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["3011", "3012", "3003", "4333"]
  }

  source_service_accounts = [google_service_account.asdb.email]
  target_service_accounts = [google_service_account.asdb.email]
}

resource "google_compute_firewall" "asdb_jenkins" {
  name    = "${var.prefix}-${local.module_name}-allow-asdb"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["4333"]
  }

  target_tags = ["allow-asdb"]
  source_ranges = [data.google_compute_subnetwork.jenkins.ip_cidr_range]
}
