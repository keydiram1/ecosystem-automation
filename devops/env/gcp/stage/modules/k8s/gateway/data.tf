data google_client_config current {}

data "kubernetes_service_v1" "istio_gateway" {
  metadata {
    name      = "${terraform.workspace}-${local.module_name}"
    namespace = "istio-ingress"
  }
}

data "google_dns_managed_zone" "dns_zone" {
  name = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}
