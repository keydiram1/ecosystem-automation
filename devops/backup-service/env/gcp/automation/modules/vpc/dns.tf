resource "google_dns_managed_zone" "dns_zone" {
  name     = "${var.prefix}-dns-zone"
  dns_name = "${var.prefix}.internal."

  visibility = "private"

  private_visibility_config {
    networks {
      network_url = google_compute_network.vpc.id
    }
  }
}