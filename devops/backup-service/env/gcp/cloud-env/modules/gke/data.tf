data google_client_config current {}

data google_compute_network vpc {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "gke" {
  name    = "${var.prefix}-${local.module_name}-subnet"
  project = data.google_client_config.current.project
}
