resource "aws_route53_record" "cluster_record" {
  name    = "gateway"
  type    = "CNAME"
  zone_id = data.aws_route53_zone.zone.zone_id
  ttl     = 60
  records = [data.kubernetes_service_v1.istio_gateway.status.0.load_balancer.0.ingress.0.hostname]
}
