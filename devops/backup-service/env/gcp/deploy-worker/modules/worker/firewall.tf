resource "google_compute_firewall" "worker" {
  name    = "${var.prefix}-${terraform.workspace}-${local.module_name}-allow-worker"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["22"]
  }

  target_tags = ["allow-worker"]
  source_ranges = [data.google_compute_subnetwork.jenkins.ip_cidr_range]
}

resource "google_compute_firewall" "allow_xdr_traffic" {
  name    = "${var.prefix}-${terraform.workspace}-${local.module_name}-allow-xdr-traffic"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports = ["8080"]
  }

  target_tags = ["allow-xdr-traffic"]
  source_ranges = [data.google_compute_subnetwork.asdb.ip_cidr_range]
}
