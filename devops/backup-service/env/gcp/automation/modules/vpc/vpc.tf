resource "google_project_service" "compute" {
  project            = data.google_client_config.current.project
  service            = "compute.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "container" {
  project            = data.google_client_config.current.project
  service            = "container.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "project_service" {
  project = data.google_client_config.current.project
  service = "iap.googleapis.com"
}

resource "google_project_service" "api_gateway" {
  project = data.google_client_config.current.project
  service = "apigateway.googleapis.com"
}

resource "google_compute_network" "vpc" {
  name                            = "${var.prefix}-vpc"
  routing_mode                    = "REGIONAL"
  mtu                             = 1460
  auto_create_subnetworks         = false
  delete_default_routes_on_create = true

  depends_on = [
    google_project_service.compute,
    google_project_service.container
  ]
}

resource "google_compute_route" "default_to_internet" {
  # TODO should be changed
  name             = "${var.prefix}-route"
  dest_range       = "0.0.0.0/0"
  network          = google_compute_network.vpc.name
  next_hop_gateway = "default-internet-gateway"
  priority         = 1000
  description      = "Default route to the Internet."
}
