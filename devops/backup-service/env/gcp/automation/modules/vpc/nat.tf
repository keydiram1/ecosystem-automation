resource "google_compute_address" "nat" {
  name         = "${var.prefix}-ip"
  network_tier = "STANDARD"
  region       = data.google_client_config.current.region
}

resource "google_compute_router_nat" "nat" {
  name   = "${var.prefix}-nat"
  router = google_compute_router.router.name
  region = data.google_client_config.current.region

  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"
  nat_ip_allocate_option             = "AUTO_ONLY"

  subnetwork {
    name = google_compute_subnetwork.jenkins.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }

  subnetwork {
    name = google_compute_subnetwork.gke.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }

  subnetwork {
    name = google_compute_subnetwork.asdb.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }

  subnetwork {
    name = google_compute_subnetwork.minio.id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }
}
