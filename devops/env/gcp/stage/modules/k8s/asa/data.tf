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

data "google_secret_manager_secret_version" "ca-aerospike-com-pem" {
  secret  = "ca-aerospike-com-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "asd-aerospike-com-key" {
  secret  = "asd-aerospike-com-key"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "asd-aerospike-com-pem" {
  secret  = "asd-aerospike-com-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "encryption-key-pem" {
  secret  = "encryption-key-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "password-txt" {
  secret  = "password-txt"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "aws-access-key-id" {
  secret  = "aws-access-key-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "aws-secret-access-key" {
  secret  = "aws-secret-access-key"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure-tenant-id" {
  secret  = "azure-tenant-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure-client-id" {
  secret  = "azure-client-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure-client-secret" {
  secret  = "azure-client-secret"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "gcp-sa-key-file" {
  secret  = "gcp-sa-key-file"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "features-conf" {
  secret  = "features-conf"
  project = data.google_client_config.current.project
}
