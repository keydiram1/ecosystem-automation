data google_client_config current {}

data google_compute_network vpc {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "asdb" {
  name = "${var.prefix}-${local.module_name}-subnet"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "jenkins" {
  name = "${var.prefix}-jenkins-subnet"
  project = data.google_client_config.current.project
}

data "google_dns_managed_zone" "dns_zone" {
  name = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}

data "google_compute_image" "ubuntu" {
  family  = local.image_family
  project = "ubuntu-os-cloud"
  most_recent = true
}
