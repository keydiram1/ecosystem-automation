resource "google_dns_record_set" "gateway" {
  name         = "${local.module_name}.${data.google_dns_managed_zone.dns_zone.dns_name}"
  type         = "A"
  ttl          = 300
  managed_zone = data.google_dns_managed_zone.dns_zone.name
  rrdatas = [data.kubernetes_service_v1.gateway.status.0.load_balancer.0.ingress.0.ip]
}
