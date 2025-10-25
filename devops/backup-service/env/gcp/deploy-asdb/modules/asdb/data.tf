data "google_client_config" "current" {}

data "google_compute_zones" "available" {
  count  = var.nodes.multi_zone ? 1 : 0
  region = data.google_client_config.current.region
}

data "google_compute_network" "vpc" {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "asdb" {
  name    = "${var.prefix}-${local.module_name}-subnet"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "jenkins" {
  name    = "${var.prefix}-jenkins-subnet"
  project = data.google_client_config.current.project
}

data "google_dns_managed_zone" "dns_zone" {
  name    = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}

data "google_compute_image" "ubuntu" {
  family      = local.image_family
  project     = "ubuntu-os-cloud"
  most_recent = true
}

data "google_compute_disk" "shadow_disk_0" {
  count = (var.asdb.device_shadow && !var.nodes.multi_zone) ? var.nodes.size : 0

  name = "${terraform.workspace}-${local.module_name}-node-${count.index}-shadow-0"
  zone = data.google_client_config.current.zone
}

data "google_compute_disk" "shadow_disk_1" {
  count = (var.asdb.device_shadow && !var.nodes.multi_zone) ? var.nodes.size : 0

  name = "${terraform.workspace}-${local.module_name}-node-${count.index}-shadow-1"
  zone = data.google_client_config.current.zone
}
