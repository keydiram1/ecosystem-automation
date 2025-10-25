resource "google_compute_subnetwork" "proxy" {
  provider      = google-beta
  name          = "${var.prefix}-proxy"
  region        = data.google_client_config.current.region
  purpose       = "REGIONAL_MANAGED_PROXY"
  stack_type    = "IPV4_ONLY"
  ip_cidr_range = local.primary_ip["proxy"]
  role          = "ACTIVE"
  network       = google_compute_network.vpc.id
}

resource "google_compute_subnetwork" "jenkins" {
  name                     = "${var.prefix}-jenkins-subnet"
  ip_cidr_range            = local.primary_ip["jenkins"]
  region                   = data.google_client_config.current.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
  stack_type               = "IPV4_ONLY"
}

resource "google_compute_subnetwork" "gke" {
  name                     = "${var.prefix}-gke-subnet"
  ip_cidr_range            = local.primary_ip["gke"]
  region                   = data.google_client_config.current.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
  stack_type               = "IPV4_ONLY"

  dynamic "secondary_ip_range" {
    for_each = local.subnet_secondary_ip
    content {
      range_name    = secondary_ip_range.key
      ip_cidr_range = secondary_ip_range.value
    }
  }
}

resource "google_compute_subnetwork" "asdb" {
  name                     = "${var.prefix}-asdb-subnet"
  ip_cidr_range            = local.primary_ip["asdb"]
  region                   = data.google_client_config.current.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
  stack_type               = "IPV4_ONLY"
}

resource "google_compute_subnetwork" "minio" {
  name                     = "${var.prefix}-minio-subnet"
  ip_cidr_range            = local.primary_ip["minio"]
  region                   = data.google_client_config.current.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
  stack_type               = "IPV4_ONLY"
}
