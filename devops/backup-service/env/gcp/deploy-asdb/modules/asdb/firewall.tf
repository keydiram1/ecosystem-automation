resource "google_compute_firewall" "asdb_internal" {
  name    = "${terraform.workspace}-${local.module_name}-allow-internal"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports    = local.asdb_service_ports
  }

  source_service_accounts = [google_service_account.asdb.email]
  target_service_accounts = [google_service_account.asdb.email]
}

resource "google_compute_firewall" "asdb_jenkins" {
  name    = "${terraform.workspace}-${local.module_name}-allow-asdb"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"

  allow {
    protocol = "tcp"
    ports    = local.asdb_service_ports
  }

  target_tags   = ["allow-asdb"]
  source_ranges = [data.google_compute_subnetwork.jenkins.ip_cidr_range]
  # depends_on = [null_resource.asdb_destroy]
}


resource "google_compute_firewall" "abs_allow_lb_hc" {
  count   = var.asdb.load_balancer ? 1 : 0
  name    = "${terraform.workspace}-${local.module_name}-allow-lb-hc"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  direction = "INGRESS"
  allow {
    protocol = "tcp"
    ports    = [local.service_port]
  }

  source_ranges = [
    "35.191.0.0/16",
    "130.211.0.0/22"
  ]

  target_service_accounts = [
    google_service_account.asdb.email
  ]
}
