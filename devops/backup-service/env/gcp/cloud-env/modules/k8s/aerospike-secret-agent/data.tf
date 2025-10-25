data google_client_config current {}

data "kubernetes_namespace_v1" "namespace" {
  metadata {
    name = var.namespace
  }
}

data "kubernetes_service_v1" "gateway" {
  metadata {
    name      = "gateway"
    namespace = "istio-ingress"
  }
}

data "google_secret_manager_secret_version" "ca_aerospike_com_pem" {
  secret  = "ca-aerospike-com-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "asd_aerospike_com_key" {
  secret  = "asd-aerospike-com-key"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "asd_aerospike_com_pem" {
  secret  = "asd-aerospike-com-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "encryption_key_pem" {
  secret  = "encryption-key-pem"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "password_txt" {
  secret  = "password-txt"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "aws_access_key_id" {
  secret  = "aws-access-key-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "aws_secret_access_key" {
  secret  = "aws-secret-access-key"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure_tenant_id" {
  secret  = "azure-tenant-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure_client_id" {
  secret  = "azure-client-id"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "azure_client_secret" {
  secret  = "azure-client-secret"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "gcp_sa_key_file" {
  secret  = "gcp-sa-key-file"
  project = data.google_client_config.current.project
}

data "google_secret_manager_secret_version" "features_conf" {
  secret  = "features-conf"
  project = data.google_client_config.current.project
}
