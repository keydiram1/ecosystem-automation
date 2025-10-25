resource "aws_route53_record" "cluster_record" {
  name    = var.cluster_name
  type    = "A"
  zone_id = data.aws_route53_zone.zone.zone_id
  ttl     = 60
  records = [aws_instance.aerospike_cluster.0.private_ip]
}
