data google_client_config current {}

data "kubernetes_service_v1" "gateway" {
  metadata {
    name      = local.module_name
    namespace = "istio-ingress"
  }
}

data "google_dns_managed_zone" "dns_zone" {
  name    = "${var.prefix}-dns-zone"
  project = data.google_client_config.current.project
}
