resource "google_compute_firewall" "allow_ssh_and_rp" {
  name    = "${var.prefix}-allow-ssh-and-rp"
  network = data.google_compute_network.vpc.self_link
  project = data.google_client_config.current.project

  allow {
    protocol = "tcp"
    ports = ["22", "8080"]
  }

  target_tags = ["allow-ssh-and-rp"]
  # source_ranges = [data.google_compute_subnetwork.jenkins.ip_cidr_range]
  source_ranges = ["0.0.0.0/0"]
}
