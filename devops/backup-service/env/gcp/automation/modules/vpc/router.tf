resource "google_compute_router" "router" {
  name    = "${var.prefix}-router"
  region  = data.google_client_config.current.region
  network = google_compute_network.vpc.id
}
