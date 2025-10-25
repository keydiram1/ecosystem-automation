data google_client_config current {}

data google_compute_network vpc {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "jenkins" {
  name    = "${var.prefix}-jenkins-subnet"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "asdb" {
  name    = "${var.prefix}-asdb-subnet"
  project = data.google_client_config.current.project
}

data "google_dns_managed_zone" "dns_zone" {
  name    = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version_access" "worker_ssh_key_pair_pub" {
  secret  = "worker-ssh-key-pair-pub"
  project = data.google_client_config.current.project
  version = "latest"
}

data google_compute_image "worker_image" {
  name    = "worker-instance"
  project = data.google_client_config.current.project
}
