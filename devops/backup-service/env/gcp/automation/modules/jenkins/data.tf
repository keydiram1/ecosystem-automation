data google_client_config current {}

data google_compute_address "ip" {
  name   = "${var.prefix}-ip"
  region = data.google_client_config.current.region
}

data google_compute_network "vpc" {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data google_compute_subnetwork "jenkins" {
  name    = "${var.prefix}-jenkins-subnet"
  project = data.google_client_config.current.project
  region  = data.google_client_config.current.region
}

data google_compute_image "jenkins_server_image" {
  name    = local.jenkins_server
  project = data.google_client_config.current.project
}

data google_compute_image "jenkins_gcp_env_test_provisioner_image" {
  name    = local.jenkins_gcp_env_test_provisioner
  project = data.google_client_config.current.project
}

data google_compute_image "jenkins_artifact_builder" {
  name    = local.jenkins_artifact_builder
  project = data.google_client_config.current.project
}

data google_compute_image "jenkins_gcp_test_worker_image" {
  name    = local.jenkins_gcp_test_worker
  project = data.google_client_config.current.project
}

data google_compute_image "jenkins_local_test_worker_image" {
  name    = local.jenkins_local_test_worker
  project = data.google_client_config.current.project
}

data google_compute_image "jenkins_multi_node_local_test_worker_image" {
  name    = local.jenkins_muli_node_local_test_worker
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret" "github_token" {
  secret_id = "github-token"
  project   = data.google_client_config.current.project
}

data "google_secret_manager_secret" "docker_username" {
  secret_id = "docker-username"
  project   = data.google_client_config.current.project
}

data "google_secret_manager_secret" "docker_password" {
  secret_id = "docker-password"
  project   = data.google_client_config.current.project
}

data "google_dns_managed_zone" "dns_public_zone" {
  name = "ecoeng-dev"
}

data "google_dns_managed_zone" "dns_private_zone" {
  name = "${var.prefix}-dns-zone"
}


data "google_secret_manager_secret_version_access" "fullchain_pem" {
  secret  = "fullchain-pem"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "privkey_pem" {
  secret  = "privkey-pem"
  version = "latest"
}

data "google_compute_disk" "persistent_disk" {
  name    = "ecoeng-data"
  project = data.google_client_config.current.project
}

data "google_compute_instance_group" "group" {
  name   = google_compute_instance_group_manager.vm_group_manager.name
  zone   = data.google_client_config.current.zone
  project = data.google_client_config.current.project
}

data "google_compute_instance" "jenkins_backend" {
  depends_on = [google_compute_instance_group_manager.vm_group_manager]
  name = local.instances
}

data "google_storage_bucket" "workspcace_vars_bucket" {
  name = "ecosys-workspace-vars"
  project = data.google_client_config.current.project
}

data "google_service_account" "abs" {
  account_id = "aerospike-backup-service-sa"
  project = data.google_client_config.current.project
}
