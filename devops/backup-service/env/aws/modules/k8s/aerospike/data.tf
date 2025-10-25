data "kubernetes_service_v1" "istio_gateway" {
  metadata {
    name = "gateway"
    namespace = "istio-ingress"
  }
}

data aws_route53_zone "zone" {
  name = local.dns_name
  private_zone = true
}