data google_client_config current {}

data "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = var.namespace
  }
}

data "kubernetes_service_v1" "istio_gateway" {
  metadata {
    name      = "${terraform.workspace}-gateway"
    namespace = "istio-ingress"
  }
}

data "google_secret_manager_secret_version_access" "docker_username" {
  secret  = "docker-username"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "docker_password" {
  secret  = "docker-password"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "ca_aerospike_com_pem" {
  secret  = "ca-aerospike-com-pem"
  version = "latest"
}

data "google_dns_managed_zone" "dns_zone" {
  name    = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}

data google_compute_network vpc {
  name    = "${var.prefix}-vpc"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "asdb" {
  name    = "${var.prefix}-asdb-subnet"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "minio" {
  name    = "${var.prefix}-minio-subnet"
  project = data.google_client_config.current.project
}

data "google_compute_subnetwork" "gke" {
  name    = "${var.prefix}-gke-subnet"
  project = data.google_client_config.current.project
}

data kubernetes_service_v1 "abs_svc" {
  metadata {
    namespace = data.kubernetes_namespace_v1.namespace.metadata.0.name
    name      = "${helm_release.backup_service.name}-${helm_release.backup_service.chart}"
  }
}

data "google_secret_manager_secret_version_access" "aws_secret_access_key" {
  count   = var.storage == "s3" ? 1 : 0
  secret  = "aws-secret-access-key"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "aws_access_key_id" {
  count   = var.storage == "s3" ? 1 : 0
  secret  = "aws-access-key-id"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "azure_tenant_id" {
  count   = var.storage == "azure" ? 1 : 0
  secret  = "azure-tenant-id"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "azure_client_id" {
  count   = var.storage == "azure" ? 1 : 0
  secret  = "azure-client-id"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "azure_client_secret" {
  count   = var.storage == "azure" ? 1 : 0
  secret  = "azure-client-secret"
  version = "latest"
}

data "google_secret_manager_secret_version_access" "azure_storage_account" {
  # count   = var.storage == "azure" ? 1 : 0
  secret  = "azure-storage-account"
  version = "latest"
}

data "google_service_account" abs {
  project    = data.google_client_config.current.project
  account_id = "${local.service_name}-sa"
}
